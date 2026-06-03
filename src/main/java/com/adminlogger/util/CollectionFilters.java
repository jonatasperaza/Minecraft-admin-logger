package com.adminlogger.util;

import java.util.List;

public final class CollectionFilters {
    private CollectionFilters() {
    }

    public static boolean containsIgnoreCase(List<? extends String> values, String candidate) {
        for (String value : values) {
            if (value != null && value.trim().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }
}
