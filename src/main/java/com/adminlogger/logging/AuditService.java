package com.adminlogger.logging;

import com.adminlogger.alert.AlertService;
import net.minecraft.world.entity.player.Player;

public class AuditService {
    private final AuditLogService logService;
    private final AlertService alertService;

    public AuditService(AuditLogService logService, AlertService alertService) {
        this.logService = logService;
        this.alertService = alertService;
    }

    public void log(Player player, String action, String type) {
        log(player, action, type, null);
    }

    public void log(Player player, String action, String type, String alertKey) {
        if (!logService.canLog(player)) {
            return;
        }

        logService.write(player, action, type);
        alertService.dispatchAlert(player, action, type, alertKey);
    }
}
