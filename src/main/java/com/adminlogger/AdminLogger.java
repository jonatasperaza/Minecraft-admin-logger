package com.adminlogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public AdminLogger() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        
        MinecraftForge.EVENT_BUS.register(this);
        
        createLogDirectory();
        
        LOGGER.info("Admin Logger v1.3 for Minecraft 1.20.1 started successfully!");
    }

    private String getLocalizedMessage(String enMessage, String ptMessage) {
        try {
            return AdminLoggerConfig.LANGUAGE.get() == AdminLoggerConfig.Language.pt_br ? ptMessage : enMessage;
        } catch (Exception e) {
            LOGGER.warn("Failed to get localized message, defaulting to English", e);
            return enMessage;
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

            Files.createDirectories(logPath.getParent());
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
            String message = getLocalizedMessage(
                "logged in to the server at ",
                "entrou no servidor em "
            );
            logEvent(playerName, message + coords, "actions");
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName().getString();
            String message = getLocalizedMessage(
                "logged out of the server",
                "saiu do servidor"
            );
            logEvent(playerName, message, "actions");
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = event.getPlayer().getName().getString();
            String message = getLocalizedMessage("said: ", "disse: ");
            logEvent(playerName, message + event.getMessage(), "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (AdminLoggerConfig.LOG_COMMANDS.get() && 
            event.getParseResults().getContext().getSource().getEntity() instanceof Player) {
            String playerName = event.getParseResults().getContext().getSource()
                .getEntity().getName().getString();
            String command = event.getParseResults().getReader().getString();
            logEvent(playerName, "executed: " + command, "commands");
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
                cause = getLocalizedMessage(
                    "was killed by " + killer.getName().getString(),
                    "foi morto por " + killer.getName().getString()
                );
            } else {
                cause = getLocalizedMessage(
                    "died by " + event.getSource().getMsgId(),
                    "morreu por " + event.getSource().getMsgId()
                );
            }
            
            logEvent(playerName, cause, "actions");
        }
    }
}