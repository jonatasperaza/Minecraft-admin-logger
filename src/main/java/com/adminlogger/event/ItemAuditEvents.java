package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

public class ItemAuditEvents {
    private final LanguageService languageService;
    private final AuditService auditService;

    public ItemAuditEvents(LanguageService languageService, AuditService auditService) {
        this.languageService = languageService;
        this.auditService = auditService;
    }

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!AdminLoggerConfig.LOG_ITEMS.get()) {
            return;
        }

        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(
                player,
                languageService.message("item.drop", playerName, MinecraftFormatters.itemName(itemEntity.getItem()), MinecraftFormatters.entityPosition(itemEntity)),
                "items"
        );
    }

    @SubscribeEvent
    public void onItemPickup(ItemEntityPickupEvent.Post event) {
        if (!AdminLoggerConfig.LOG_ITEMS.get()) {
            return;
        }

        ItemStack original = event.getOriginalStack();
        ItemStack current = event.getCurrentStack();
        int pickedUp = ItemStack.isSameItemSameComponents(original, current) ? original.getCount() - current.getCount() : original.getCount();
        if (pickedUp <= 0) {
            return;
        }

        Player player = event.getPlayer();
        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(
                player,
                languageService.message("item.pickup", playerName, MinecraftFormatters.itemName(original, pickedUp), MinecraftFormatters.entityPosition(event.getItemEntity())),
                "items"
        );
    }
}
