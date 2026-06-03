package com.adminlogger.logging;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.stats.StatsTracker;
import com.adminlogger.util.CollectionFilters;
import com.adminlogger.util.MinecraftFormatters;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class AuditLogService {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    private final StatsTracker statsTracker;

    public AuditLogService(StatsTracker statsTracker) {
        this.statsTracker = statsTracker;
    }

    public void createLogDirectory() {
        try {
            Files.createDirectories(logDirectory());
            if (AdminLoggerConfig.LOG_GLOBAL_INDEX.get()) {
                Files.createDirectories(logDirectory().resolve("_global"));
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create log directory", e);
        }
    }

    public boolean canLog(Player player) {
        return !CollectionFilters.containsIgnoreCase(AdminLoggerConfig.IGNORED_PLAYERS.get(), MinecraftFormatters.playerName(player))
                && !CollectionFilters.containsIgnoreCase(AdminLoggerConfig.IGNORED_PLAYERS.get(), player.getUUID().toString())
                && !CollectionFilters.containsIgnoreCase(AdminLoggerConfig.IGNORED_WORLDS.get(), MinecraftFormatters.worldName(player.level()));
    }

    public void write(Player player, String action, String type) {
        writeEvent(MinecraftFormatters.playerName(player), player.getUUID(), action, type);
    }

    public void writeEvent(String playerName, UUID playerUuid, String action, String type) {
        try {
            createPlayerDirectory(playerName, playerUuid);
            String date = DATE_FORMAT.format(new Date());
            String time = TIME_FORMAT.format(new Date());
            String fileName = date + "-" + type + logFileExtension();
            String logLine = formatLogLine(date, time, playerName, playerUuid, action, type);

            writeLogLine(playerDirectory(playerName, playerUuid).resolve(fileName), logLine);

            if (AdminLoggerConfig.LOG_GLOBAL_INDEX.get()) {
                writeLogLine(logDirectory().resolve("_global").resolve(fileName), logLine);
            }

            statsTracker.record(playerName, type);
        } catch (IOException e) {
            LOGGER.error("Failed to log event", e);
        }
    }

    public Path logDirectory() {
        return Path.of(AdminLoggerConfig.LOG_DIRECTORY.get().trim());
    }

    private void createPlayerDirectory(String playerName, UUID playerUuid) throws IOException {
        Files.createDirectories(playerDirectory(playerName, playerUuid));
    }

    private void writeLogLine(Path logPath, String logLine) throws IOException {
        Files.createDirectories(logPath.getParent());
        manageLogSize(logPath);

        try (BufferedWriter writer = Files.newBufferedWriter(
                logPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(logLine);
        }
    }

    private String formatLogLine(String date, String time, String playerName, UUID playerUuid, String action, String type) {
        if (isJsonLogFormat()) {
            JsonObject event = new JsonObject();
            event.addProperty("date", date);
            event.addProperty("time", time);
            event.addProperty("type", type);
            event.addProperty("player", playerName);
            event.addProperty("uuid", playerUuid.toString());
            event.addProperty("message", action);
            return GSON.toJson(event) + System.lineSeparator();
        }

        String uuidPart = AdminLoggerConfig.INCLUDE_PLAYER_UUID.get() ? " [uuid:" + playerUuid + "]" : "";
        return String.format("[%s] [%s]%s %s%n", time, type, uuidPart, action);
    }

    private void manageLogSize(Path logPath) throws IOException {
        if (Files.exists(logPath) && Files.size(logPath) > AdminLoggerConfig.MAX_LOG_SIZE_MB.get() * 1024L * 1024L) {
            String archiveName = logPath.getFileName().toString().replace(
                    logFileExtension(),
                    "-archived-" + System.currentTimeMillis() + logFileExtension()
            );
            Files.move(logPath, logPath.resolveSibling(archiveName), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path playerDirectory(String playerName, UUID playerUuid) {
        String folderName = AdminLoggerConfig.USE_UUID_FOLDERS.get() ? playerUuid.toString() : safePathSegment(playerName);
        return logDirectory().resolve(folderName);
    }

    private String safePathSegment(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private boolean isJsonLogFormat() {
        return "jsonl".equalsIgnoreCase(AdminLoggerConfig.LOG_FORMAT.get());
    }

    private String logFileExtension() {
        return isJsonLogFormat() ? ".jsonl" : ".log";
    }
}
