package com.adminlogger.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AdminLoggerConfig {
    public static ForgeConfigSpec.BooleanValue LOG_CHAT;
    public static ForgeConfigSpec.BooleanValue LOG_COMMANDS;
    public static ForgeConfigSpec.BooleanValue LOG_INVENTORY;
    public static ForgeConfigSpec.EnumValue<Language> LANGUAGE;
    
    public static final ForgeConfigSpec SPEC;
    
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        init(builder);
        SPEC = builder.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
    
    public static void init(ForgeConfigSpec.Builder builder) {
        builder.comment("Configurações do Admin Logger");
        
        LANGUAGE = builder
            .comment("Idioma do mod (en_us = English, pt_br = Português Brasil)")
            .defineEnum("language", Language.pt_br);
            
        LOG_CHAT = builder
            .comment("Habilitar registro de chat")
            .define("logChat", true);
            
        LOG_COMMANDS = builder
            .comment("Habilitar registro de comandos")
            .define("logCommands", true);
            
        LOG_INVENTORY = builder
            .comment("Habilitar registro de inventário")
            .define("logInventory", false);
    }
    
    public enum Language {
        en_us,
        pt_br
    }
} 