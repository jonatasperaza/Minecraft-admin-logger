package com.adminlogger.container;

import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContainerTracker {
    private final Map<UUID, BlockPos> pendingContainerPositions = new HashMap<>();
    private final Map<UUID, ContainerSession> containerSessions = new HashMap<>();

    public void rememberCandidate(Player player, BlockPos pos) {
        pendingContainerPositions.put(player.getUUID(), pos.immutable());
    }

    public BlockPos consumePendingPosition(Player player) {
        return pendingContainerPositions.remove(player.getUUID());
    }

    public void startSession(Player player, ContainerSession session) {
        containerSessions.put(player.getUUID(), session);
    }

    public ContainerSession endSession(Player player) {
        return containerSessions.remove(player.getUUID());
    }

    public void clear(Player player) {
        pendingContainerPositions.remove(player.getUUID());
        containerSessions.remove(player.getUUID());
    }

    public boolean isOpenContainer(BlockEntity blockEntity) {
        return blockEntity instanceof Container;
    }

    public Container getContainer(LevelAccessor level, BlockPos pos) {
        if (level instanceof Level realLevel && realLevel.getBlockEntity(pos) instanceof Container container) {
            return container;
        }
        return null;
    }

    public Map<String, ItemSnapshot> snapshot(Container container) {
        Map<String, ItemSnapshot> items = new HashMap<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            String key = itemKey(stack);
            String displayName = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            items.merge(key, new ItemSnapshot(displayName, stack.getCount()),
                    (oldValue, newValue) -> new ItemSnapshot(oldValue.displayName(), oldValue.count() + newValue.count()));
        }
        return Map.copyOf(items);
    }

    public List<ContainerChange> diff(ContainerSession session, Map<String, ItemSnapshot> after) {
        List<ContainerChange> changes = new ArrayList<>();
        for (Map.Entry<String, ItemSnapshot> entry : session.items().entrySet()) {
            ItemSnapshot beforeItem = entry.getValue();
            ItemSnapshot afterItem = after.get(entry.getKey());
            int afterCount = afterItem == null ? 0 : afterItem.count();
            if (beforeItem.count() > afterCount) {
                String amount = (beforeItem.count() - afterCount) + "x " + beforeItem.displayName();
                changes.add(new ContainerChange("container.remove", amount, session.containerName(), session.location()));
            }
        }

        for (Map.Entry<String, ItemSnapshot> entry : after.entrySet()) {
            ItemSnapshot afterItem = entry.getValue();
            ItemSnapshot beforeItem = session.items().get(entry.getKey());
            int beforeCount = beforeItem == null ? 0 : beforeItem.count();
            if (afterItem.count() > beforeCount) {
                String amount = (afterItem.count() - beforeCount) + "x " + afterItem.displayName();
                changes.add(new ContainerChange("container.add", amount, session.containerName(), session.location()));
            }
        }
        return changes;
    }

    public ContainerSession createSession(Level level, BlockPos pos, Container container) {
        String containerName = MinecraftFormatters.blockName(level.getBlockState(pos));
        String location = MinecraftFormatters.blockPosition(level, pos);
        return new ContainerSession(pos, containerName, location, snapshot(container));
    }

    private String itemKey(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getTag();
    }
}
