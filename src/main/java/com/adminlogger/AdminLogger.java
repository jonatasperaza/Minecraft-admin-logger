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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public AdminLogger() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AdminLoggerConfig.getSpec());
        MinecraftForge.EVENT_BUS.register(commands);
        MinecraftForge.EVENT_BUS.register(playerEvents);
        MinecraftForge.EVENT_BUS.register(blockEvents);
        MinecraftForge.EVENT_BUS.register(containerEvents);
        MinecraftForge.EVENT_BUS.register(itemEvents);
        MinecraftForge.EVENT_BUS.register(teleportEvents);

        LOGGER.info("Admin Logger v{} for Minecraft 1.20.1 Forge initialized!", AdminLoggerConstants.MOD_VERSION);
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
