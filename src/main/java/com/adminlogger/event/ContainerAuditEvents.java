package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.container.ContainerChange;
import com.adminlogger.container.ContainerSession;
import com.adminlogger.container.ContainerTracker;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ContainerAuditEvents {
    private final LanguageService languageService;
    private final AuditService auditService;
    private final ContainerTracker containerTracker;

    public ContainerAuditEvents(LanguageService languageService, AuditService auditService, ContainerTracker containerTracker) {
        this.languageService = languageService;
        this.auditService = auditService;
        this.containerTracker = containerTracker;
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get() || event.getHand() != InteractionHand.MAIN_HAND || !MinecraftFormatters.isServerSide(event.getLevel())) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity != null && containerTracker.isOpenContainer(blockEntity)) {
            containerTracker.rememberCandidate(event.getEntity(), event.getPos());
        }
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get()) {
            return;
        }

        Player player = event.getEntity();
        BlockPos pos = containerTracker.consumePendingPosition(player);
        if (pos == null || !MinecraftFormatters.isServerSide(player.level())) {
            return;
        }

        Container container = containerTracker.getContainer(player.level(), pos);
        if (container == null) {
            return;
        }

        ContainerSession session = containerTracker.createSession(player.level(), pos, container);
        containerTracker.startSession(player, session);

        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(player, languageService.message("container.open", playerName, session.containerName(), session.location()), "containers");
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (!AdminLoggerConfig.LOG_CONTAINERS.get()) {
            return;
        }

        Player player = event.getEntity();
        ContainerSession session = containerTracker.endSession(player);
        if (session == null || !MinecraftFormatters.isServerSide(player.level())) {
            return;
        }

        Container container = containerTracker.getContainer(player.level(), session.pos());
        if (container == null) {
            return;
        }

        String playerName = MinecraftFormatters.playerName(player);
        for (ContainerChange change : containerTracker.diff(session, containerTracker.snapshot(container))) {
            auditService.log(
                    player,
                    languageService.message(change.languageKey(), playerName, change.amount(), change.containerName(), change.location()),
                    "containers"
            );
        }
    }
}
