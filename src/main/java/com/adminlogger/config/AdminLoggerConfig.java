package com.adminlogger.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AdminLoggerConfig {
    public static ForgeConfigSpec.BooleanValue LOG_CHAT;
    public static ForgeConfigSpec.BooleanValue LOG_COMMANDS;
    public static ForgeConfigSpec.BooleanValue LOG_INVENTORY;
    public static ForgeConfigSpec.EnumValue<Language> LANGUAGE;
    
    private static final ForgeConfigSpec SPEC;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        setupConfig(builder);
        SPEC = builder.build();
    }
    
    public static ForgeConfigSpec getSpec() {
        return SPEC;
    }
    
    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("Admin Logger Configuration");
        
        LANGUAGE = builder
            .comment("Mod language (en_us = English, pt_br = Português Brasil)")
            .defineEnum("language", Language.pt_br);
            
        LOG_CHAT = builder
            .comment("Enable chat logging")
            .define("logChat", true);
            
        LOG_COMMANDS = builder
            .comment("Enable command logging")
            .define("logCommands", true);
            
        LOG_INVENTORY = builder
            .comment("Enable inventory logging")
            .define("logInventory", false);
    }
    
    public enum Language {
        en_us,
        pt_br
    }
}