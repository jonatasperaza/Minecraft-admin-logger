package com.adminlogger;

import com.adminlogger.alert.AlertService;
import com.adminlogger.command.AdminLoggerCommands;
import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.container.ContainerTracker;
import com.adminlogger.event.BlockAuditEvents;
import com.adminlogger.event.ContainerAuditEvents;
import com.adminlogger.event.ItemAuditEvents;
import com.adminlogger.event.PlayerAuditEvents;
import com.adminlogger.event.TeleportAuditEvents;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditLogService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.stats.StatsTracker;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AdminLoggerConstants.MOD_ID)
public class AdminLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LanguageService languageService = new LanguageService();
    private final StatsTracker statsTracker = new StatsTracker();
    private final AuditLogService logService = new AuditLogService(statsTracker);
    private final AlertService alertService = new AlertService(languageService, logService);
    private final AuditService auditService = new AuditService(logService, alertService);
    private final ContainerTracker containerTracker = new ContainerTracker();
    private final AdminLoggerCommands commands = new AdminLoggerCommands(languageService, logService, statsTracker);
    private final PlayerAuditEvents playerEvents = new PlayerAuditEvents(languageService, auditService, containerTracker);
    private final BlockAuditEvents blockEvents = new BlockAuditEvents(languageService, auditService);
    private final ContainerAuditEvents containerEvents = new ContainerAuditEvents(languageService, auditService, containerTracker);
    private final ItemAuditEvents itemEvents = new ItemAuditEvents(languageService, auditService);
    private final TeleportAuditEvents teleportEvents = new TeleportAuditEvents(languageService, auditService);

    public AdminLogger(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        modContainer.registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        NeoForge.EVENT_BUS.register(commands);
        NeoForge.EVENT_BUS.register(playerEvents);
        NeoForge.EVENT_BUS.register(blockEvents);
        NeoForge.EVENT_BUS.register(containerEvents);
        NeoForge.EVENT_BUS.register(itemEvents);
        NeoForge.EVENT_BUS.register(teleportEvents);

        LOGGER.info("Admin Logger v{} for Minecraft 1.21.1 NeoForge initialized!", AdminLoggerConstants.MOD_VERSION);
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        languageService.loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        logService.createLogDirectory();
        LOGGER.info("Admin Logger config loaded successfully!");
    }

    private void onConfigReload(final ModConfigEvent.Reloading event) {
        languageService.loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        logService.createLogDirectory();
        LOGGER.info("Admin Logger config reloaded successfully!");
    }
}
