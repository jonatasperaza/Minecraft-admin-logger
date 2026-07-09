package com.adminlogger.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

public final class MinecraftFormatters {
    private MinecraftFormatters() {
    }

    public static String playerName(Player player) {
        return player.getName().getString();
    }

    public static String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    public static String itemName(ItemStack stack) {
        return itemName(stack, stack.getCount());
    }

    public static String itemName(ItemStack stack, int count) {
        return count + "x " + BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    public static String worldName(LevelAccessor level) {
        if (level instanceof Level realLevel) {
            return realLevel.dimension().location().toString();
        }
        return "unknown";
    }

    public static String dimensionName(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    public static String blockPosition(LevelAccessor level, BlockPos pos) {
        return String.format(Locale.ROOT, "world:%s x:%d y:%d z:%d", worldName(level), pos.getX(), pos.getY(), pos.getZ());
    }

    public static String entityPosition(Entity entity) {
        return position(entity.level(), entity.getX(), entity.getY(), entity.getZ());
    }

    public static String position(LevelAccessor level, double x, double y, double z) {
        return String.format(Locale.ROOT, "world:%s x:%.2f y:%.2f z:%.2f", worldName(level), x, y, z);
    }

    public static boolean isServerSide(LevelAccessor level) {
        return !(level instanceof Level realLevel) || !realLevel.isClientSide;
    }
}
