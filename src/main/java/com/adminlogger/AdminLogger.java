package com.adminlogger;

import com.adminlogger.config.AdminLoggerConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.UUID;
import java.util.regex.Pattern;

@Mod(AdminLogger.MOD_ID)
public class AdminLogger {
    public static final String MOD_ID = "adminlogger";
    private static final String MOD_VERSION = "2.3.0";

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();

    private final Map<UUID, BlockPos> pendingContainerPositions = new HashMap<>();
    private final Map<UUID, ContainerSession> containerSessions = new HashMap<>();
    private final Map<String, Integer> eventCounters = new HashMap<>();
    private final Map<String, Integer> playerCounters = new HashMap<>();
    private int totalEvents;

    public AdminLogger(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("Admin Logger v{} for Minecraft 1.21.1 NeoForge initialized!", MOD_VERSION);
    }

    private void loadLanguage(String langCode) {
        String langFile = "assets/adminlogger/lang/" + langCode + ".json";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(langFile)) {
            LANGUAGE_MAP.clear();
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                jsonObject.entrySet().forEach(entry ->
                        LANGUAGE_MAP.put(entry.getKey(), entry.getValue().getAsString())
                );
            } else {
                LOGGER.warn("Language file {} not found! Loading English...", langFile);
                loadLanguage("en_us");
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load language file: {}", langFile, e);
        }
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        createLogDirectory();
        LOGGER.info("Admin Logger config loaded successfully!");
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        createLogDirectory();
        LOGGER.info("Admin Logger config reloaded successfully!");
    }

    private String getLocalizedMessage(String key, Object... args) {
        String message = LANGUAGE_MAP.getOrDefault(key, key);
        try {
            return String.format(message, args);
        } catch (MissingFormatArgumentException e) {
            LOGGER.warn("Format mismatch for key '{}': {}", key, e.getMessage());
            return message;
        }
    }

    private void createLogDirectory() {
        try {
            Files.createDirectories(logDirectory());
            if (AdminLoggerConfig.LOG_GLOBAL_INDEX.get()) {
                Files.createDirectories(logDirectory().resolve("_global"));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create log directory", e);
        }
    }

    private void createPlayerDirectory(String playerName, UUID playerUuid) {
        try {
            Files.createDirectories(playerDirectory(playerName, playerUuid));
        } catch (IOException e) {
            LOGGER.error("Failed to create player directory: {}", playerName, e);
        }
    }

    private void logEvent(Player player, String action, String type) {
        logEvent(player, action, type, null);
    }

    private void logEvent(Player player, String action, String type, String alertKey) {
        if (!shouldLogPlayer(player) || !shouldLogWorld(player.level())) {
            return;
        }

        String playerName = playerName(player);
        writeEvent(playerName, player.getUUID(), action, type);
        dispatchAlert(player, action, type, alertKey);
    }

    private void writeEvent(String playerName, UUID playerUuid, String action, String type) {
        try {
            createPlayerDirectory(playerName, playerUuid);
            String date = DATE_FORMAT.format(new Date());
            String time = TIME_FORMAT.format(new Date());
            String fileName = date + "-" + type + logFileExtension();
            String logLine = formatLogLine(date, time, playerName, playerUuid, action, type);

            writeLogLine(playerDirectory(playerName, playerUuid).resolve(fileName), logLine);

            if (AdminLoggerConfig.LOG_GLOBAL_INDEX.get()) {
                writeLogLine(logDirectory().resolve("_global").resolve(fileName), logLine);
            }

            recordStats(playerName, type);
        } catch (IOException e) {
            LOGGER.error("Failed to log event", e);
        }
    }

    private void writeLogLine(Path logPath, String logLine) throws IOException {
        Files.createDirectories(logPath.getParent());
        manageLogSize(logPath);

        try (BufferedWriter writer = Files.newBufferedWriter(
                logPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(logLine);
        }
    }

    private String formatLogLine(String date, String time, String playerName, UUID playerUuid, String action, String type) {
        if (isJsonLogFormat()) {
            JsonObject event = new JsonObject();
            event.addProperty("date", date);
            event.addProperty("time", time);
            event.addProperty("type", type);
            event.addProperty("player", playerName);
            event.addProperty("uuid", playerUuid.toString());
            event.addProperty("message", action);
            return GSON.toJson(event) + System.lineSeparator();
        }

        String uuidPart = AdminLoggerConfig.INCLUDE_PLAYER_UUID.get() ? " [uuid:" + playerUuid + "]" : "";
        return String.format("[%s] [%s]%s %s%n", time, type, uuidPart, action);
    }

    private void manageLogSize(Path logPath) throws IOException {
        if (Files.exists(logPath) && Files.size(logPath) > AdminLoggerConfig.MAX_LOG_SIZE_MB.get() * 1024L * 1024L) {
            String archiveName = logPath.getFileName().toString().replace(
                    logFileExtension(),
                    "-archived-" + System.currentTimeMillis() + logFileExtension()
            );
            Files.move(logPath, logPath.resolveSibling(archiveName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path logDirectory() {
        return Path.of(AdminLoggerConfig.LOG_DIRECTORY.get().trim());
    }

    private Path playerDirectory(String playerName, UUID playerUuid) {
        String folderName = AdminLoggerConfig.USE_UUID_FOLDERS.get() ? playerUuid.toString() : safePathSegment(playerName);
        return logDirectory().resolve(folderName);
    }

    private String safePathSegment(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private boolean isJsonLogFormat() {
        return "jsonl".equalsIgnoreCase(AdminLoggerConfig.LOG_FORMAT.get());
    }

    private String logFileExtension() {
        return isJsonLogFormat() ? ".jsonl" : ".log";
    }

    private void recordStats(String playerName, String type) {
        totalEvents++;
        eventCounters.merge(type, 1, Integer::sum);
        playerCounters.merge(playerName, 1, Integer::sum);
    }

    private void dispatchAlert(Player player, String action, String type, String alertKey) {
        if (!AdminLoggerConfig.ENABLE_ALERTS.get() || !shouldAlert(player, type, alertKey)) {
            return;
        }

        String playerName = playerName(player);
        String alertMessage = getLocalizedMessage("alert.message", playerName, type, action);

        if (AdminLoggerConfig.WRITE_ALERT_LOG.get()) {
            writeEvent(playerName, player.getUUID(), alertMessage, "alerts");
        }

        if (AdminLoggerConfig.BROADCAST_ALERTS_TO_OPS.get()) {
            broadcastAlertToOps(player, alertMessage);
        }

        if (AdminLoggerConfig.DISCORD_WEBHOOK_ENABLED.get()) {
            sendDiscordAlert(alertMessage);
        }
    }

    private boolean shouldAlert(Player player, String type, String alertKey) {
        return containsIgnoreCase(AdminLoggerConfig.ALERT_EVENT_TYPES.get(), type)
                || containsIgnoreCase(AdminLoggerConfig.WATCHED_PLAYERS.get(), playerName(player))
                || containsIgnoreCase(AdminLoggerConfig.WATCHED_PLAYERS.get(), player.getUUID().toString())
                || (alertKey != null && containsIgnoreCase(AdminLoggerConfig.WATCHED_COMMANDS.get(), alertKey));
    }

    private void broadcastAlertToOps(Player player, String alertMessage) {
        var server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(onlinePlayer.getGameProfile())) {
                onlinePlayer.sendSystemMessage(Component.literal(alertMessage));
            }
        }
    }

    private void sendDiscordAlert(String alertMessage) {
        String webhookUrl = AdminLoggerConfig.DISCORD_WEBHOOK_URL.get().trim();
        if (webhookUrl.isEmpty()) {
            return;
        }

        URI webhookUri;
        try {
            webhookUri = URI.create(webhookUrl);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid Discord webhook URL configured for Admin Logger.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", AdminLoggerConfig.DISCORD_WEBHOOK_USERNAME.get());
        payload.addProperty("content", alertMessage);

        HttpRequest request = HttpRequest.newBuilder(webhookUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        LOGGER.warn("Discord webhook alert failed with HTTP status {}", response.statusCode());
                    }
                })
                .exceptionally(error -> {
                    LOGGER.warn("Failed to send Discord webhook alert.", error);
                    return null;
                });
    }

    private String playerName(Player player) {
        return player.getName().getString();
    }

    private String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private String itemName(ItemStack stack) {
        return itemName(stack, stack.getCount());
    }

    private String itemName(ItemStack stack, int count) {
        return count + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    private String worldName(LevelAccessor level) {
        if (level instanceof Level realLevel) {
            return realLevel.dimension().location().toString();
        }
        return "unknown";
    }

    private String dimensionName(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    private String blockPosition(LevelAccessor level, BlockPos pos) {
        return String.format(Locale.ROOT, "world:%s x:%d y:%d z:%d", worldName(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private String entityPosition(Entity entity) {
        return position(entity.level(), entity.getX(), entity.getY(), entity.getZ());
    }

    private String position(LevelAccessor level, double x, double y, double z) {
        return String.format(Locale.ROOT, "world:%s x:%.2f y:%.2f z:%.2f", worldName(level), x, y, z);
    }

    private boolean isServerSide(LevelAccessor level) {
        return !(level instanceof Level realLevel) || !realLevel.isClientSide;
    }

    private boolean shouldLogPlayer(Player player) {
        return !containsIgnoreCase(AdminLoggerConfig.IGNORED_PLAYERS.get(), playerName(player))
                && !containsIgnoreCase(AdminLoggerConfig.IGNORED_PLAYERS.get(), player.getUUID().toString());
    }

    private boolean shouldLogWorld(LevelAccessor level) {
        return !containsIgnoreCase(AdminLoggerConfig.IGNORED_WORLDS.get(), worldName(level));
    }

    private boolean shouldLogCommand(String command) {
        String commandRoot = commandRoot(command);
        return !commandRoot.isEmpty() && !containsIgnoreCase(AdminLoggerConfig.IGNORED_COMMANDS.get(), commandRoot);
    }

    private String commandRoot(String command) {
        String normalized = command.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).strip();
        }
        return normalized.isEmpty() ? "" : normalized.split("\\s+", 2)[0];
    }

    private boolean containsIgnoreCase(List<? extends String> values, String candidate) {
        for (String value : values) {
            if (value != null && value.trim().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOpenContainer(BlockEntity blockEntity) {
        return blockEntity instanceof Container;
    }

    private Container getContainer(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level realLevel && realLevel.getBlockEntity(pos) instanceof Container container) {
            return container;
        }
        return null;
    }

    private Map<String, ItemSnapshot> snapshot(Container container) {
        Map<String, ItemSnapshot> items = new HashMap<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            String key = itemKey(stack);
            String displayName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            items.merge(key, new ItemSnapshot(displayName, stack.getCount()),
                    (oldValue, newValue) -> new ItemSnapshot(oldValue.displayName(), oldValue.count() + newValue.count()));
        }
        return Map.copyOf(items);
    }

    private String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getComponentsPatch();
    }

    private void logContainerDiff(Player player, ContainerSession session, Map<String, ItemSnapshot> after) {
        String playerName = playerName(player);
        for (Map.Entry<String, ItemSnapshot> entry : session.items().entrySet()) {
            ItemSnapshot beforeItem = entry.getValue();
            ItemSnapshot afterItem = after.get(entry.getKey());
            int afterCount = afterItem == null ? 0 : afterItem.count();
            if (beforeItem.count() > afterCount) {
                String amount = (beforeItem.count() - afterCount) + "x " + beforeItem.displayName();
                logEvent(player, getLocalizedMessage("container.remove", playerName, amount, session.containerName(), session.location()), "containers");
            }
        }

        for (Map.Entry<String, ItemSnapshot> entry : after.entrySet()) {
            ItemSnapshot afterItem = entry.getValue();
            ItemSnapshot beforeItem = session.items().get(entry.getKey());
            int beforeCount = beforeItem == null ? 0 : beforeItem.count();
            if (afterItem.count() > beforeCount) {
                String amount = (afterItem.count() - beforeCount) + "x " + afterItem.displayName();
                logEvent(player, getLocalizedMessage("container.add", playerName, amount, session.containerName(), session.location()), "containers");
            }
        }
    }

    private String maskSensitiveCommand(String command) {
        if (!AdminLoggerConfig.MASK_SENSITIVE_COMMANDS.get()) {
            return command;
        }

        String masked = command;
        for (String term : AdminLoggerConfig.SENSITIVE_COMMAND_TERMS.get()) {
            String quoted = Pattern.quote(term);
            masked = masked.replaceAll("(?i)(--?" + quoted + "\\s+)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(--?" + quoted + "\\s*=\\s*)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(\\b" + quoted + "\\b\\s+)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(\\b" + quoted + "\\b\\s*[=:]\\s*)(\\S+)", "$1<redacted>");
        }
        return masked;
    }

    private int reloadCommand(CommandSourceStack source) {
        loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        createLogDirectory();
        source.sendSuccess(() -> Component.literal(getLocalizedMessage("reload.success")), true);
        LOGGER.info("Admin Logger reloaded by command.");
        return Command.SINGLE_SUCCESS;
    }

    private int statusCommand(CommandSourceStack source) {
        sendStatusLine(source, "status.header");
        sendStatusLine(source, "status.version", MOD_VERSION);
        sendStatusLine(source, "status.log_directory", logDirectory().toAbsolutePath().normalize());
        sendStatusLine(source, "status.log_format", AdminLoggerConfig.LOG_FORMAT.get().toLowerCase(Locale.ROOT));
        sendStatusLine(source, "status.enabled_categories", enabledCategories());
        sendStatusLine(source, "status.global_index", AdminLoggerConfig.LOG_GLOBAL_INDEX.get());
        sendStatusLine(source, "status.alerts", AdminLoggerConfig.ENABLE_ALERTS.get(), AdminLoggerConfig.DISCORD_WEBHOOK_ENABLED.get());
        sendStatusLine(
                source,
                "status.filters",
                AdminLoggerConfig.IGNORED_PLAYERS.get().size(),
                AdminLoggerConfig.IGNORED_WORLDS.get().size(),
                AdminLoggerConfig.IGNORED_COMMANDS.get().size()
        );
        return Command.SINGLE_SUCCESS;
    }

    private int statsCommand(CommandSourceStack source) {
        sendStatusLine(source, "stats.header");
        sendStatusLine(source, "stats.total", totalEvents);
        sendStatusLine(source, "stats.categories", formatCounters(eventCounters));
        sendStatusLine(source, "stats.players", formatCounters(playerCounters));
        return Command.SINGLE_SUCCESS;
    }

    private void sendStatusLine(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.literal(getLocalizedMessage(key, args)), false);
    }

    private String enabledCategories() {
        List<String> categories = new ArrayList<>();
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            categories.add("chat");
        }
        if (AdminLoggerConfig.LOG_COMMANDS.get()) {
            categories.add("commands");
        }
        if (AdminLoggerConfig.LOG_INVENTORY.get()) {
            categories.add("inventory");
        }
        if (AdminLoggerConfig.LOG_BLOCKS.get()) {
            categories.add("blocks");
        }
        if (AdminLoggerConfig.LOG_CONTAINERS.get()) {
            categories.add("containers");
        }
        if (AdminLoggerConfig.LOG_ITEMS.get()) {
            categories.add("items");
        }
        if (AdminLoggerConfig.LOG_GAME_MODE.get()) {
            categories.add("gamemode");
        }
        if (AdminLoggerConfig.LOG_TELEPORTS.get()) {
            categories.add("teleports");
        }
        return categories.isEmpty() ? "none" : String.join(", ", categories);
    }

    private String formatCounters(Map<String, Integer> counters) {
        if (counters.isEmpty()) {
            return getLocalizedMessage("stats.none");
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counters.entrySet());
        entries.sort((left, right) -> {
            int countCompare = right.getValue().compareTo(left.getValue());
            return countCompare != 0 ? countCompare : left.getKey().compareTo(right.getKey());
        });

        List<String> values = new ArrayList<>();
        for (int index = 0; index < Math.min(5, entries.size()); index++) {
            Map.Entry<String, Integer> entry = entries.get(index);
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(", ", values);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("adminlogger")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload").executes(context -> reloadCommand(context.getSource())))
                        .then(Commands.literal("status").executes(context -> statusCommand(context.getSource())))
                        .then(Commands.literal("stats").executes(context -> statsCommand(context.getSource())))
        );
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String playerName = playerName(player);
        String coords = position(player.level(), player.getX(), player.getY(), player.getZ());
        String message = getLocalizedMessage("login", playerName, coords);
        logEvent(player, message, "actions");
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        String playerName = playerName(player);
        pendingContainerPositions.remove(player.getUUID());
        containerSessions.remove(player.getUUID());
        String message = getLocalizedMessage("logout", playerName);
        logEvent(player, message, "actions");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = playerName(event.getPlayer());
            String message = getLocalizedMessage("chat", playerName, event.getRawText());
            logEvent(event.getPlayer(), message, "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        var sourceEntity = event.getParseResults().getContext().getSource().getEntity();
        if (AdminLoggerConfig.LOG_COMMANDS.get() && sourceEntity instanceof Player player) {
            String playerName = playerName(player);
            String rawCommand = event.getParseResults().getReader().getString();
            String alertKey = commandRoot(rawCommand);
            if (!shouldLogCommand(rawCommand)) {
                return;
            }

            String command = maskSensitiveCommand(rawCommand);
            String message = getLocalizedMessage("command", playerName, command);
            logEvent(player, message, "commands", alertKey);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            String playerName = playerName(player);
            String cause;

            if (event.getSource().getEntity() instanceof Player killer) {
                cause = getLocalizedMessage("death.by.player", playerName, killer.getName().getString());
            } else {
                cause = getLocalizedMessage("death.generic", playerName, event.getSource().getMsgId());
            }

            logEvent(player, cause, "actions");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!AdminLoggerConfig.LOG_BLOCKS.get() || event.isCanceled() || !isServerSide(event.getLevel())) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = playerName(player);
        String message = getLocalizedMessage("block.break", playerName, blockName(event.getState()), blockPosition(event.getLevel(), event.getPos()));
        logEvent(player, message, "blocks");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AdminLoggerConfig.LOG_BLOCKS.get() || event.isCanceled() || !isServerSide(event.getLevel())) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            String playerName = playerName(player);
            String message = getLocalizedMessage("block.place", playerName, blockName(event.getPlacedBlock()), blockPosition(event.getLevel(), event.getPos()));
            logEvent(player, message, "blocks");
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get() || event.getHand() != InteractionHand.MAIN_HAND || !isServerSide(event.getLevel())) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity != null && isOpenContainer(blockEntity)) {
            pendingContainerPositions.put(event.getEntity().getUUID(), event.getPos().immutable());
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get()) {
            return;
        }

        Player player = event.getEntity();
        BlockPos pos = pendingContainerPositions.remove(player.getUUID());
        if (pos == null || !isServerSide(player.level())) {
            return;
        }

        Container container = getContainer(player.level(), pos);
        if (container == null) {
            return;
        }

        BlockState state = player.level().getBlockState(pos);
        String playerName = playerName(player);
        String containerName = blockName(state);
        String location = blockPosition(player.level(), pos);
        containerSessions.put(player.getUUID(), new ContainerSession(pos, containerName, location, snapshot(container)));
        logEvent(player, getLocalizedMessage("container.open", playerName, containerName, location), "containers");
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get()) {
            return;
        }

        Player player = event.getEntity();
        ContainerSession session = containerSessions.remove(player.getUUID());
        if (session == null || !isServerSide(player.level())) {
            return;
        }

        Container container = getContainer(player.level(), session.pos());
        if (container != null) {
            logContainerDiff(player, session, snapshot(container));
        }
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!AdminLoggerConfig.LOG_ITEMS.get()) {
            return;
        }

        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getEntity();
        String playerName = playerName(player);
        String message = getLocalizedMessage("item.drop", playerName, itemName(itemEntity.getItem()), entityPosition(itemEntity));
        logEvent(player, message, "items");
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!AdminLoggerConfig.LOG_ITEMS.get()) {
            return;
        }

        ItemStack original = event.getOriginalStack();
        ItemStack current = event.getCurrentStack();
        int pickedUp = ItemStack.isSameItemSameComponents(original, current) ? original.getCount() - current.getCount() : original.getCount();
        if (pickedUp <= 0) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = playerName(player);
        String message = getLocalizedMessage("item.pickup", playerName, itemName(original, pickedUp), entityPosition(event.getItemEntity()));
        logEvent(player, message, "items");
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!AdminLoggerConfig.LOG_GAME_MODE.get()) {
            return;
        }

        Player player = event.getEntity();
        String playerName = playerName(player);
        String message = getLocalizedMessage("gamemode.change", playerName, event.getCurrentGameMode().getName(), event.getNewGameMode().getName());
        logEvent(player, message, "actions");
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get()) {
            return;
        }

        Player player = event.getEntity();
        String playerName = playerName(player);
        String message = getLocalizedMessage("dimension.change", playerName, dimensionName(event.getFrom()), dimensionName(event.getTo()), entityPosition(player));
        logEvent(player, message, "actions");
    }

    @SubscribeEvent
    public void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        String playerName = playerName(player);
        String from = entityPosition(player);
        String to = position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        logEvent(player, getLocalizedMessage("teleport.command", playerName, from, to), "actions");
    }

    @SubscribeEvent
    public void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get()) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = playerName(player);
        String from = entityPosition(player);
        String to = position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        logEvent(player, getLocalizedMessage("teleport.ender_pearl", playerName, from, to), "actions");
    }

    @SubscribeEvent
    public void onChorusFruitTeleport(EntityTeleportEvent.ChorusFruit event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        String playerName = playerName(player);
        String from = entityPosition(player);
        String to = position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        logEvent(player, getLocalizedMessage("teleport.chorus", playerName, from, to), "actions");
    }

    private record ContainerSession(BlockPos pos, String containerName, String location, Map<String, ItemSnapshot> items) {
    }

    private record ItemSnapshot(String displayName, int count) {
    }
}
