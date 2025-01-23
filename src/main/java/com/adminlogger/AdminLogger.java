package com.adminlogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Mod("adminlogger")
public class AdminLogger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String LOG_DIRECTORY = "logs/adminlogger/";

    public AdminLogger() {
        MinecraftForge.EVENT_BUS.register(this);
        createLogDirectory();
    }

    private void createLogDirectory() {
        File directory = new File(LOG_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void createPlayerDirectory(String playerName) {
        File playerDir = new File(LOG_DIRECTORY + playerName);
        if (!playerDir.exists()) {
            playerDir.mkdirs();
        }
    }

    private void logEvent(String playerName, String action, String type) {
        try {
            createPlayerDirectory(playerName);
            String date = DATE_FORMAT.format(new Date());
            String time = TIME_FORMAT.format(new Date());
            String fileName = type.equals("chat") ? "chat.log" : "actions.log";
            String logFile = LOG_DIRECTORY + playerName + "/" + date + "-" + fileName;
            
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(String.format("[%s] %s%n", time, action));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String playerName = event.getEntity().getName().getString();
        logEvent(playerName, "entrou no servidor", "action");
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.getEntity().getName().getString();
        logEvent(playerName, "saiu do servidor", "action");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String playerName = event.getPlayer().getName().getString();
        logEvent(playerName, "disse: " + event.getMessage(), "chat");
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() != null) {
            String playerName = event.getParseResults().getContext().getSource().getEntity().getName().getString();
            String command = event.getParseResults().getReader().getString();
            logEvent(playerName, "executou comando: " + command, "action");
        }
    }
} 