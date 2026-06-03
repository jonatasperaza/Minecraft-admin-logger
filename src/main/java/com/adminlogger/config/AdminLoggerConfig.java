package com.adminlogger.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AdminLoggerConfig {
    public static final ModConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ModConfigSpec.ConfigValue<String> LOG_DIRECTORY;
    public static final ModConfigSpec.IntValue MAX_LOG_SIZE_MB;
    public static final ModConfigSpec.ConfigValue<String> LOG_FORMAT;
    public static final ModConfigSpec.BooleanValue LOG_GLOBAL_INDEX;
    public static final ModConfigSpec.BooleanValue USE_UUID_FOLDERS;
    public static final ModConfigSpec.BooleanValue INCLUDE_PLAYER_UUID;
    public static final ModConfigSpec.BooleanValue LOG_CHAT;
    public static final ModConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ModConfigSpec.BooleanValue LOG_INVENTORY;
    public static final ModConfigSpec.BooleanValue LOG_BLOCKS;
    public static final ModConfigSpec.BooleanValue LOG_CONTAINERS;
    public static final ModConfigSpec.BooleanValue LOG_ITEMS;
    public static final ModConfigSpec.BooleanValue LOG_GAME_MODE;
    public static final ModConfigSpec.BooleanValue LOG_TELEPORTS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> IGNORED_PLAYERS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> IGNORED_WORLDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> IGNORED_COMMANDS;
    public static final ModConfigSpec.BooleanValue MASK_SENSITIVE_COMMANDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SENSITIVE_COMMAND_TERMS;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");

        LANGUAGE = BUILDER
                .comment("Mod language (en_us or pt_br)")
                .define("language", "pt_br", AdminLoggerConfig::isSupportedLanguage);

        LOG_DIRECTORY = BUILDER
                .comment("Directory where Admin Logger stores logs. Relative paths use the game/server directory.")
                .define("logDirectory", "logs/adminlogger", AdminLoggerConfig::isNonBlankString);

        MAX_LOG_SIZE_MB = BUILDER
                .comment("Maximum size in MB for each log file before archiving")
                .defineInRange("maxLogSizeMb", 5, 1, 1024);

        LOG_FORMAT = BUILDER
                .comment("Log format: text or jsonl")
                .define("logFormat", "text", AdminLoggerConfig::isSupportedLogFormat);

        LOG_GLOBAL_INDEX = BUILDER
                .comment("Also write every event to logs/adminlogger/_global for quick server-wide review")
                .define("logGlobalIndex", true);

        USE_UUID_FOLDERS = BUILDER
                .comment("Use player UUIDs instead of player names for per-player folders")
                .define("useUuidFolders", false);

        INCLUDE_PLAYER_UUID = BUILDER
                .comment("Include the player UUID in text log lines")
                .define("includePlayerUuid", false);

        LOG_CHAT = BUILDER
                .comment("Enable chat logging")
                .define("logChat", true);

        LOG_COMMANDS = BUILDER
                .comment("Enable command logging")
                .define("logCommands", true);

        LOG_INVENTORY = BUILDER
                .comment("Enable inventory logging")
                .define("logInventory", false);

        LOG_BLOCKS = BUILDER
                .comment("Enable block break/place logging")
                .define("logBlocks", true);

        LOG_CONTAINERS = BUILDER
                .comment("Enable container open/change logging")
                .define("logContainers", true);

        LOG_ITEMS = BUILDER
                .comment("Enable item pickup/drop logging")
                .define("logItems", true);

        LOG_GAME_MODE = BUILDER
                .comment("Enable game mode change logging")
                .define("logGameMode", true);

        LOG_TELEPORTS = BUILDER
                .comment("Enable teleport and dimension change logging")
                .define("logTeleports", true);

        IGNORED_PLAYERS = BUILDER
                .comment("Player names or UUIDs that should not be logged")
                .defineListAllowEmpty(
                        "ignoredPlayers",
                        java.util.List::of,
                        value -> value instanceof String
                );

        IGNORED_WORLDS = BUILDER
                .comment("Dimension ids that should not be logged, for example minecraft:the_end")
                .defineListAllowEmpty(
                        "ignoredWorlds",
                        java.util.List::of,
                        value -> value instanceof String
                );

        IGNORED_COMMANDS = BUILDER
                .comment("Command roots that should not be logged, useful for auth commands like /login")
                .defineListAllowEmpty(
                        "ignoredCommands",
                        () -> java.util.List.of("login", "register", "l", "reg"),
                        value -> value instanceof String
                );

        MASK_SENSITIVE_COMMANDS = BUILDER
                .comment("Mask sensitive command arguments before logging")
                .define("maskSensitiveCommands", true);

        SENSITIVE_COMMAND_TERMS = BUILDER
                .comment("Command argument names that should have their following value redacted")
                .defineListAllowEmpty(
                        "sensitiveCommandTerms",
                        () -> java.util.List.of("password", "passwd", "senha", "token", "secret", "key", "apikey", "api_key", "webhook"),
                        value -> value instanceof String
                );

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static ModConfigSpec getSpec() {
        return SPEC;
    }

    private static boolean isSupportedLanguage(Object value) {
        return value instanceof String language && ("en_us".equals(language) || "pt_br".equals(language));
    }

    private static boolean isSupportedLogFormat(Object value) {
        return value instanceof String format && ("text".equalsIgnoreCase(format) || "jsonl".equalsIgnoreCase(format));
    }

    private static boolean isNonBlankString(Object value) {
        return value instanceof String string && !string.isBlank();
    }
}
