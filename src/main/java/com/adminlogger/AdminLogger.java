package com.adminlogger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import com.adminlogger.config.AdminLoggerConfig;


@Mod("adminlogger")
public class AdminLogger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final String LOG_DIRECTORY = "logs/adminlogger/";
    private static final Logger LOGGER = LogManager.getLogger();

    public AdminLogger() {
        AdminLoggerConfig.register();
        
        LOGGER.info("Admin Logger v1.0 para Minecraft 1.20.1 iniciado com sucesso!");
        LOGGER.info("Desenvolvido por jonatasperaza");
        
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
        double x = event.getEntity().getX();
        double y = event.getEntity().getY();
        double z = event.getEntity().getZ();
        String coords = String.format("(x:%.2f, y:%.2f, z:%.2f)", x, y, z);
        logEvent(playerName, "entrou no servidor em " + coords, "action");
        
        // Adicionar log do inventário no login
        if (event.getEntity() instanceof Player) {
            logInventory((Player) event.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String playerName = event.getEntity().getName().getString();
        logEvent(playerName, "saiu do servidor", "action");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = event.getPlayer().getName().getString();
            String message = AdminLoggerConfig.LANGUAGE.get() == AdminLoggerConfig.Language.pt_br ? 
                "disse: " : "said: ";
            logEvent(playerName, message + event.getMessage(), "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() != null) {
            String playerName = event.getParseResults().getContext().getSource().getEntity().getName().getString();
            String command = event.getParseResults().getReader().getString();
            
            // Verificar atividades suspeitas
            checkForSuspiciousActivity(playerName, command);
            
            // Ignorar comandos específicos para não sobrecarregar o log
            if (!command.startsWith("/tell") && !command.startsWith("/msg")) {
                logEvent(playerName, "executou comando: " + command, "action");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            String playerName = player.getName().getString();
            
            if (event.getSource().getEntity() instanceof Player) {
                Player killer = (Player) event.getSource().getEntity();
                logEvent(playerName, "foi morto por " + killer.getName().getString(), "action");
            } else {
                logEvent(playerName, "morreu por " + event.getSource().getMsgId(), "action");
            }
        }
    }

    private void logInventory(Player player) {
        StringBuilder items = new StringBuilder();
        player.getInventory().items.forEach(itemStack -> {
            if (!itemStack.isEmpty()) {
                items.append(itemStack.getCount())
                     .append("x ")
                     .append(itemStack.getDisplayName().getString())
                     .append(", ");
            }
        });
        logEvent(player.getName().getString(), "inventário: " + items.toString(), "inventory");
    }

    private void checkForSuspiciousActivity(String playerName, String action) {
        if (action.contains("gamemode creative") || action.contains("op")) {
            // Notificar admins online
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            server.getPlayerList().getPlayers().forEach(player -> {
                if (server.getPlayerList().isOp(player.getGameProfile())) {
                    player.sendSystemMessage(Component.literal("§c[AdminLogger] Ação suspeita: " + 
                        playerName + " " + action));
                }
            });
        }
    }
} 