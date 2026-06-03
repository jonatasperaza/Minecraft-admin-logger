package com.adminlogger.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

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
                .defineInList("language", "pt_br", List.of("en_us", "pt_br"));

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
}
