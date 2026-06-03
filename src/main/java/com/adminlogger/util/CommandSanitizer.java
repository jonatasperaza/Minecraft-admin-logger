package com.adminlogger.util;

import com.adminlogger.config.AdminLoggerConfig;

import java.util.regex.Pattern;

public final class CommandSanitizer {
    private CommandSanitizer() {
    }

    public static boolean shouldLogCommand(String command) {
        String commandRoot = commandRoot(command);
        return !commandRoot.isEmpty() && !CollectionFilters.containsIgnoreCase(AdminLoggerConfig.IGNORED_COMMANDS.get(), commandRoot);
    }

    public static String commandRoot(String command) {
        String normalized = command.strip();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).strip();
        }
        return normalized.isEmpty() ? "" : normalized.split("\\s+", 2)[0];
    }

    public static String maskSensitiveCommand(String command) {
        if (!AdminLoggerConfig.MASK_SENSITIVE_COMMANDS.get()) {
            return command;
        }

        String masked = command;
        for (String term : AdminLoggerConfig.SENSITIVE_COMMAND_TERMS.get()) {
            String quoted = Pattern.quote(term);
            masked = masked.replaceAll("(?i)(--?" + quoted + "\\s+)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(--?" + quoted + "\\s*=\\s*)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(\\b" + quoted + "\\b\\s+)(\\S+)", "$1<redacted>");
            masked = masked.replaceAll("(?i)(\\b" + quoted + "\\b\\s*[=:]\\s*)(\\S+)", "$1<redacted>");
        }
        return masked;
    }
}
