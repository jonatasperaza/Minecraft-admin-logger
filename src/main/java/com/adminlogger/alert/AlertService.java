package com.adminlogger.alert;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditLogService;
import com.adminlogger.util.CollectionFilters;
import com.adminlogger.util.MinecraftFormatters;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class AlertService {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LanguageService languageService;
    private final AuditLogService logService;

    public AlertService(LanguageService languageService, AuditLogService logService) {
        this.languageService = languageService;
        this.logService = logService;
    }

    public void dispatchAlert(Player player, String action, String type, String alertKey) {
        if (!AdminLoggerConfig.ENABLE_ALERTS.get() || !shouldAlert(player, type, alertKey)) {
            return;
        }

        String playerName = MinecraftFormatters.playerName(player);
        String alertMessage = languageService.message("alert.message", playerName, type, action);

        if (AdminLoggerConfig.WRITE_ALERT_LOG.get()) {
            logService.writeEvent(playerName, player.getUUID(), alertMessage, "alerts");
        }

        if (AdminLoggerConfig.BROADCAST_ALERTS_TO_OPS.get()) {
            broadcastAlertToOps(player, alertMessage);
        }

        if (AdminLoggerConfig.DISCORD_WEBHOOK_ENABLED.get()) {
            sendDiscordAlert(alertMessage);
        }
    }

    private boolean shouldAlert(Player player, String type, String alertKey) {
        return CollectionFilters.containsIgnoreCase(AdminLoggerConfig.ALERT_EVENT_TYPES.get(), type)
                || CollectionFilters.containsIgnoreCase(AdminLoggerConfig.WATCHED_PLAYERS.get(), MinecraftFormatters.playerName(player))
                || CollectionFilters.containsIgnoreCase(AdminLoggerConfig.WATCHED_PLAYERS.get(), player.getUUID().toString())
                || (alertKey != null && CollectionFilters.containsIgnoreCase(AdminLoggerConfig.WATCHED_COMMANDS.get(), alertKey));
    }

    private void broadcastAlertToOps(Player player, String alertMessage) {
        var server = player.getServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer onlinePlayer : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(onlinePlayer.getGameProfile())) {
                onlinePlayer.sendSystemMessage(Component.literal(alertMessage));
            }
        }
    }

    private void sendDiscordAlert(String alertMessage) {
        String webhookUrl = AdminLoggerConfig.DISCORD_WEBHOOK_URL.get().trim();
        if (webhookUrl.isEmpty()) {
            return;
        }

        URI webhookUri;
        try {
            webhookUri = URI.create(webhookUrl);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid Discord webhook URL configured for Admin Logger.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("username", AdminLoggerConfig.DISCORD_WEBHOOK_USERNAME.get());
        payload.addProperty("content", alertMessage);

        HttpRequest request = HttpRequest.newBuilder(webhookUri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        LOGGER.warn("Discord webhook alert failed with HTTP status {}", response.statusCode());
                    }
                })
                .exceptionally(error -> {
                    LOGGER.warn("Failed to send Discord webhook alert.", error);
                    return null;
                });
    }
}
