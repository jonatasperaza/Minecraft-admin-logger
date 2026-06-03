package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.container.ContainerTracker;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.CommandSanitizer;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerAuditEvents {
    private final LanguageService languageService;
    private final AuditService auditService;
    private final ContainerTracker containerTracker;

    public PlayerAuditEvents(LanguageService languageService, AuditService auditService, ContainerTracker containerTracker) {
        this.languageService = languageService;
        this.auditService = auditService;
        this.containerTracker = containerTracker;
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        String coords = MinecraftFormatters.position(player.level(), player.getX(), player.getY(), player.getZ());
        auditService.log(player, languageService.message("login", playerName, coords), "actions");
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        containerTracker.clear(player);
        auditService.log(player, languageService.message("logout", playerName), "actions");
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            String playerName = MinecraftFormatters.playerName(event.getPlayer());
            auditService.log(event.getPlayer(), languageService.message("chat", playerName, event.getRawText()), "chat");
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        var sourceEntity = event.getParseResults().getContext().getSource().getEntity();
        if (AdminLoggerConfig.LOG_COMMANDS.get() && sourceEntity instanceof Player player) {
            String rawCommand = event.getParseResults().getReader().getString();
            String alertKey = CommandSanitizer.commandRoot(rawCommand);
            if (!CommandSanitizer.shouldLogCommand(rawCommand)) {
                return;
            }

            String playerName = MinecraftFormatters.playerName(player);
            String command = CommandSanitizer.maskSensitiveCommand(rawCommand);
            auditService.log(player, languageService.message("command", playerName, command), "commands", alertKey);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            String playerName = MinecraftFormatters.playerName(player);
            String cause;

            if (event.getSource().getEntity() instanceof Player killer) {
                cause = languageService.message("death.by.player", playerName, killer.getName().getString());
            } else {
                cause = languageService.message("death.generic", playerName, event.getSource().getMsgId());
            }

            auditService.log(player, cause, "actions");
        }
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (!AdminLoggerConfig.LOG_GAME_MODE.get()) {
            return;
        }

        Player player = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(
                player,
                languageService.message("gamemode.change", playerName, event.getCurrentGameMode().getName(), event.getNewGameMode().getName()),
                "actions"
        );
    }
}
