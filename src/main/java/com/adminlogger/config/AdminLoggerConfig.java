package com.adminlogger.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AdminLoggerConfig {
    public static final ModConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ModConfigSpec.BooleanValue LOG_CHAT;
    public static final ModConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ModConfigSpec.BooleanValue LOG_INVENTORY;
    public static final ModConfigSpec.BooleanValue LOG_BLOCKS;
    public static final ModConfigSpec.BooleanValue LOG_CONTAINERS;
    public static final ModConfigSpec.BooleanValue LOG_ITEMS;
    public static final ModConfigSpec.BooleanValue LOG_GAME_MODE;
    public static final ModConfigSpec.BooleanValue LOG_TELEPORTS;
    public static final ModConfigSpec.BooleanValue MASK_SENSITIVE_COMMANDS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SENSITIVE_COMMAND_TERMS;

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec SPEC;

    static {
        BUILDER.push("general");

        LANGUAGE = BUILDER
                .comment("Mod language (en_us or pt_br)")
                .define("language", "pt_br", AdminLoggerConfig::isSupportedLanguage);

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
}
