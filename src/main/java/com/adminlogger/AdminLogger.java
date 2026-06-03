package com.adminlogger;

import com.adminlogger.config.AdminLoggerConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

@Mod(AdminLogger.MOD_ID)
public class AdminLogger {
    public static final String MOD_ID = "adminlogger";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final Path LOG_DIRECTORY = Path.of("logs", "adminlogger");
    private static final int MAX_LOG_SIZE_MB = 5;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, String> LANGUAGE_MAP = new HashMap<>();

    public AdminLogger(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        NeoForge.EVENT_BUS.register(this);
        createLogDirectory();

        LOGGER.info("Admin Logger v2.0.0 for Minecraft 1.21.1 NeoForge initialized!");
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
        LOGGER.info("Admin Logger config loaded successfully!");
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        loadLanguage(AdminLoggerConfig.LANGUAGE.get());
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
            Files.createDirectories(LOG_DIRECTORY);
        } catch (IOException e) {
            LOGGER.error("Failed to create log directory", e);
        }
    }

    private void createPlayerDirectory(String playerName) {
        try {
            Files.createDirectories(LOG_DIRECTORY.resolve(playerName));
        } catch (IOException e) {
            LOGGER.error("Failed to create player directory: {}", playerName, e);
        }
    }

    private void logEvent(String playerName, String action, String type) {
        try {
            createPlayerDirectory(playerName);
            String date = DATE_FORMAT.format(new Date());
            String time = TIME_FORMAT.format(new Date());
            Path logPath = LOG_DIRECTORY.resolve(playerName).resolve(date + "-" + type + ".log");
            manageLogSize(logPath);

            try (BufferedWriter writer = Files.newBufferedWriter(
                    logPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(String.format("[%s] %s%n", time, action));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to log event", e);
        }
    }

    private void manageLogSize(Path logPath) throws IOException {
        if (Files.exists(logPath) && Files.size(logPath) > MAX_LOG_SIZE_MB * 1024L * 1024L) {
            String archiveName = logPath.getFileName().toString().replace(
                    ".log",
                    "-archived-" + System.currentTimeMillis() + ".log"
            );
            Files.move(logPath, logPath.resolveSibling(archiveName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName().getString();
        String coords = String.format("(x:%.2f, y:%.2f, z:%.2f)", player.getX(), player.getY(), player.getZ());
        String message = getLocalizedMessage("login", playerName, coords);
        logEvent(playerName, message, "actions");
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName().getString();
        String message = getLocalizedMessage("logout", playerName);
        logEvent(playerName, message, "actions");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = event.getPlayer().getName().getString();
            String message = getLocalizedMessage("chat", playerName, event.getRawText());
            logEvent(playerName, message, "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        var sourceEntity = event.getParseResults().getContext().getSource().getEntity();
        if (AdminLoggerConfig.LOG_COMMANDS.get() && sourceEntity instanceof Player player) {
            String playerName = player.getName().getString();
            String command = event.getParseResults().getReader().getString();
            String message = getLocalizedMessage("command", playerName, command);
            logEvent(playerName, message, "commands");
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            String playerName = player.getName().getString();
            String cause;

            if (event.getSource().getEntity() instanceof Player killer) {
                cause = getLocalizedMessage("death.by.player", playerName, killer.getName().getString());
            } else {
                cause = getLocalizedMessage("death.generic", playerName, event.getSource().getMsgId());
            }

            logEvent(playerName, cause, "actions");
        }
    }
}
