package com.adminlogger.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AdminLoggerConfig {
    public static ForgeConfigSpec.BooleanValue LOG_CHAT;
    public static ForgeConfigSpec.BooleanValue LOG_COMMANDS;
    public static ForgeConfigSpec.BooleanValue LOG_INVENTORY;
    public static ForgeConfigSpec.ConfigValue<String> LANGUAGE;
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;
    
    static {
        setupConfig(BUILDER);
        SPEC = BUILDER.build();
    }
    
    public static ForgeConfigSpec getSpec() {
        return SPEC;
    }
    
    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general");
        
        LANGUAGE = builder
            .comment("Mod language (e.g., en_us, pt_br)")
            .define("language", "pt_br");
            
        LOG_CHAT = builder
            .comment("Enable chat logging")
            .define("logChat", true);
            
        LOG_COMMANDS = builder
            .comment("Enable command logging")
            .define("logCommands", true);
            
        LOG_INVENTORY = builder
            .comment("Enable inventory logging")
            .define("logInventory", false);
            
        builder.pop();
    }
}