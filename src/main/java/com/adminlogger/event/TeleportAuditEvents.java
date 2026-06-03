package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class TeleportAuditEvents {
    private final LanguageService languageService;
    private final AuditService auditService;

    public TeleportAuditEvents(LanguageService languageService, AuditService auditService) {
        this.languageService = languageService;
        this.auditService = auditService;
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get()) {
            return;
        }

        Player player = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(
                player,
                languageService.message("dimension.change", playerName, MinecraftFormatters.dimensionName(event.getFrom()), MinecraftFormatters.dimensionName(event.getTo()), MinecraftFormatters.entityPosition(player)),
                "actions"
        );
    }

    @SubscribeEvent
    public void onTeleportCommand(EntityTeleportEvent.TeleportCommand event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        String playerName = MinecraftFormatters.playerName(player);
        String from = MinecraftFormatters.entityPosition(player);
        String to = MinecraftFormatters.position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        auditService.log(player, languageService.message("teleport.command", playerName, from, to), "actions");
    }

    @SubscribeEvent
    public void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get()) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = MinecraftFormatters.playerName(player);
        String from = MinecraftFormatters.entityPosition(player);
        String to = MinecraftFormatters.position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        auditService.log(player, languageService.message("teleport.ender_pearl", playerName, from, to), "actions");
    }

    @SubscribeEvent
    public void onChorusFruitTeleport(EntityTeleportEvent.ChorusFruit event) {
        if (!AdminLoggerConfig.LOG_TELEPORTS.get() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        String playerName = MinecraftFormatters.playerName(player);
        String from = MinecraftFormatters.entityPosition(player);
        String to = MinecraftFormatters.position(player.level(), event.getTargetX(), event.getTargetY(), event.getTargetZ());
        auditService.log(player, languageService.message("teleport.chorus", playerName, from, to), "actions");
    }
}
