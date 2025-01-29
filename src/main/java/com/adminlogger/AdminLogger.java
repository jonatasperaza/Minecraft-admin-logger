package com.adminlogger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.nio.charset.StandardCharsets;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import com.adminlogger.config.AdminLoggerConfig;

@Mod(AdminLogger.MOD_ID)
public class AdminLogger {
    public static final String MOD_ID = "adminlogger";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String LOG_DIRECTORY = "logs/adminlogger/";
    private static final int MAX_LOG_SIZE_MB = 5;
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static Map<String, String> languageMap = new HashMap<>();

    public AdminLogger() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onConfigLoad);
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        MinecraftForge.EVENT_BUS.register(this);
        createLogDirectory();
        
        LOGGER.info("Admin Logger v1.4 for Minecraft 1.20.1 initialized!");
    }

    private void loadLanguage(String langCode) {
        String langFile = "assets/adminlogger/lang/" + langCode + ".json";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(langFile)) {
            languageMap.clear();
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                jsonObject.entrySet().forEach(entry -> 
                    languageMap.put(entry.getKey(), entry.getValue().getAsString())
                );
            } else {
                LOGGER.warn("Language file {} not found! Loading English...", langFile);
                loadLanguage("en_us");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load language file: {}", langFile, e);
        }
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        LOGGER.info("Admin Logger config loaded successfully!");
    }

    private String getLocalizedMessage(String key, Object... args) {
        String message = languageMap.getOrDefault(key, key);
        try {
            return String.format(message, args);
        } catch (MissingFormatArgumentException e) {
            LOGGER.warn("Format mismatch for key '{}': {}", key, e.getMessage());
            return message;
        }
    }

    private void createLogDirectory() {
        try {
            Files.createDirectories(Paths.get(LOG_DIRECTORY));
        } catch (IOException e) {
            LOGGER.error("Failed to create log directory", e);
        }
    }

    private void createPlayerDirectory(String playerName) {
        try {
            Files.createDirectories(Paths.get(LOG_DIRECTORY, playerName));
        } catch (IOException e) {
            LOGGER.error("Failed to create player directory: " + playerName, e);
        }
    }

    private void logEvent(String playerName, String action, String type) {
        try {
            createPlayerDirectory(playerName);
            String date = DATE_FORMAT.format(new Date());
            String time = TIME_FORMAT.format(new Date());
            Path logPath = Paths.get(LOG_DIRECTORY, playerName, date + "-" + type + ".log");
            manageLogSize(logPath);

            try (BufferedWriter writer = Files.newBufferedWriter(logPath, 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(String.format("[%s] %s%n", time, action));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to log event: ", e);
        }
    }

    private void manageLogSize(Path logPath) throws IOException {
        if (Files.exists(logPath) && Files.size(logPath) > MAX_LOG_SIZE_MB * 1024 * 1024) {
            Path archivePath = Paths.get(logPath.toString().replace(".log", 
                    "-archived-" + System.currentTimeMillis() + ".log"));
            Files.move(logPath, archivePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName().getString();
            String coords = String.format("(x:%.2f, y:%.2f, z:%.2f)", 
                player.getX(), player.getY(), player.getZ());
            String message = getLocalizedMessage("login", playerName, coords);
            logEvent(playerName, message, "actions");
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName().getString();
            String message = getLocalizedMessage("logout", playerName);
            logEvent(playerName, message, "actions");
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = event.getPlayer().getName().getString();
            String content = event.getMessage().getString();
            content = content.replaceAll("literal\\{|\\}", "");
            String message = getLocalizedMessage("chat", playerName, content);
            logEvent(playerName, message, "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (AdminLoggerConfig.LOG_COMMANDS.get() && 
            event.getParseResults().getContext().getSource().getEntity() instanceof Player) {
            String playerName = event.getParseResults().getContext().getSource()
                .getEntity().getName().getString();
            String command = event.getParseResults().getReader().getString();
            String message = getLocalizedMessage("command", playerName, command);
            logEvent(playerName, message, "commands");
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName().getString();
            String cause;
            
            if (event.getSource().getEntity() instanceof Player) {
                Player killer = (Player) event.getSource().getEntity();
                cause = getLocalizedMessage("death.by.player", playerName, killer.getName().getString());
            } else {
                cause = getLocalizedMessage("death.generic", playerName, event.getSource().getMsgId());
            }
            
            logEvent(playerName, cause, "actions");
        }
    }
}