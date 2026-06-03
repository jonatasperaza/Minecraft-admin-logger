package com.adminlogger.command;

import com.adminlogger.AdminLoggerConstants;
import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditLogService;
import com.adminlogger.stats.StatsTracker;
import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminLoggerCommands {
    private final LanguageService languageService;
    private final AuditLogService logService;
    private final StatsTracker statsTracker;

    public AdminLoggerCommands(LanguageService languageService, AuditLogService logService, StatsTracker statsTracker) {
        this.languageService = languageService;
        this.logService = logService;
        this.statsTracker = statsTracker;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("adminlogger")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload").executes(context -> reloadCommand(context.getSource())))
                        .then(Commands.literal("status").executes(context -> statusCommand(context.getSource())))
                        .then(Commands.literal("stats").executes(context -> statsCommand(context.getSource())))
        );
    }

    private int reloadCommand(CommandSourceStack source) {
        languageService.loadLanguage(AdminLoggerConfig.LANGUAGE.get());
        logService.createLogDirectory();
        source.sendSuccess(() -> Component.literal(languageService.message("reload.success")), true);
        return Command.SINGLE_SUCCESS;
    }

    private int statusCommand(CommandSourceStack source) {
        sendStatusLine(source, "status.header");
        sendStatusLine(source, "status.version", AdminLoggerConstants.MOD_VERSION);
        sendStatusLine(source, "status.log_directory", logService.logDirectory().toAbsolutePath().normalize());
        sendStatusLine(source, "status.log_format", AdminLoggerConfig.LOG_FORMAT.get().toLowerCase(Locale.ROOT));
        sendStatusLine(source, "status.enabled_categories", enabledCategories());
        sendStatusLine(source, "status.global_index", AdminLoggerConfig.LOG_GLOBAL_INDEX.get());
        sendStatusLine(source, "status.alerts", AdminLoggerConfig.ENABLE_ALERTS.get(), AdminLoggerConfig.DISCORD_WEBHOOK_ENABLED.get());
        sendStatusLine(
                source,
                "status.filters",
                AdminLoggerConfig.IGNORED_PLAYERS.get().size(),
                AdminLoggerConfig.IGNORED_WORLDS.get().size(),
                AdminLoggerConfig.IGNORED_COMMANDS.get().size()
        );
        return Command.SINGLE_SUCCESS;
    }

    private int statsCommand(CommandSourceStack source) {
        sendStatusLine(source, "stats.header");
        sendStatusLine(source, "stats.total", statsTracker.totalEvents());
        sendStatusLine(source, "stats.categories", statsTracker.formatEventCounters(languageService));
        sendStatusLine(source, "stats.players", statsTracker.formatPlayerCounters(languageService));
        return Command.SINGLE_SUCCESS;
    }

    private void sendStatusLine(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.literal(languageService.message(key, args)), false);
    }

    private String enabledCategories() {
        List<String> categories = new ArrayList<>();
        if (AdminLoggerConfig.LOG_CHAT.get()) {
            categories.add("chat");
        }
        if (AdminLoggerConfig.LOG_COMMANDS.get()) {
            categories.add("commands");
        }
        if (AdminLoggerConfig.LOG_INVENTORY.get()) {
            categories.add("inventory");
        }
        if (AdminLoggerConfig.LOG_BLOCKS.get()) {
            categories.add("blocks");
        }
        if (AdminLoggerConfig.LOG_CONTAINERS.get()) {
            categories.add("containers");
        }
        if (AdminLoggerConfig.LOG_ITEMS.get()) {
            categories.add("items");
        }
        if (AdminLoggerConfig.LOG_GAME_MODE.get()) {
            categories.add("gamemode");
        }
        if (AdminLoggerConfig.LOG_TELEPORTS.get()) {
            categories.add("teleports");
        }
        return categories.isEmpty() ? "none" : String.join(", ", categories);
    }
}
