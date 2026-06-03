package com.adminlogger.container;

import net.minecraft.core.BlockPos;

import java.util.Map;

public record ContainerSession(BlockPos pos, String containerName, String location, Map<String, ItemSnapshot> items) {
}
