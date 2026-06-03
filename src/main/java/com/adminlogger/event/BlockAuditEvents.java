package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public class BlockAuditEvents {
    private final LanguageService languageService;
    private final AuditService auditService;

    public BlockAuditEvents(LanguageService languageService, AuditService auditService) {
        this.languageService = languageService;
        this.auditService = auditService;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!AdminLoggerConfig.LOG_BLOCKS.get() || event.isCanceled() || !MinecraftFormatters.isServerSide(event.getLevel())) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = MinecraftFormatters.playerName(player);
        String message = languageService.message(
                "block.break",
                playerName,
                MinecraftFormatters.blockName(event.getState()),
                MinecraftFormatters.blockPosition(event.getLevel(), event.getPos())
        );
        auditService.log(player, message, "blocks");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!AdminLoggerConfig.LOG_BLOCKS.get() || event.isCanceled() || !MinecraftFormatters.isServerSide(event.getLevel())) {
            return;
        }

        if (event.getEntity() instanceof Player player) {
            String playerName = MinecraftFormatters.playerName(player);
            String message = languageService.message(
                    "block.place",
                    playerName,
                    MinecraftFormatters.blockName(event.getPlacedBlock()),
                    MinecraftFormatters.blockPosition(event.getLevel(), event.getPos())
            );
            auditService.log(player, message, "blocks");
        }
    }
}
