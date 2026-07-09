package com.adminlogger.event;

import com.adminlogger.config.AdminLoggerConfig;
import com.adminlogger.i18n.LanguageService;
import com.adminlogger.logging.AuditService;
import com.adminlogger.util.MinecraftFormatters;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityItemPickupEvent event) {
        if (!AdminLoggerConfig.LOG_ITEMS.get() || event.isCanceled()) {
            return;
        }

        ItemStack pickedUpStack = event.getItem().getItem();
        Player player = event.getEntity();
        String playerName = MinecraftFormatters.playerName(player);
        auditService.log(
                player,
                languageService.message("item.pickup", playerName, MinecraftFormatters.itemName(pickedUpStack), MinecraftFormatters.entityPosition(event.getItem())),
                "items"
        );
    }
}
