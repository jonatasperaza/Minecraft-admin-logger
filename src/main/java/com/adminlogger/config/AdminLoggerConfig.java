package com.adminlogger.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AdminLoggerConfig {
    public static final ModConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ModConfigSpec.BooleanValue LOG_CHAT;
    public static final ModConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ModConfigSpec.BooleanValue LOG_INVENTORY;

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
