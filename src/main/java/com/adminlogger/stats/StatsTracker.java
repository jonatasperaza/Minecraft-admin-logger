package com.adminlogger.stats;

import com.adminlogger.i18n.LanguageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsTracker {
    private final Map<String, Integer> eventCounters = new HashMap<>();
    private final Map<String, Integer> playerCounters = new HashMap<>();
    private int totalEvents;

    public void record(String playerName, String type) {
        totalEvents++;
        eventCounters.merge(type, 1, Integer::sum);
        playerCounters.merge(playerName, 1, Integer::sum);
    }

    public int totalEvents() {
        return totalEvents;
    }

    public String formatEventCounters(LanguageService languageService) {
        return formatCounters(eventCounters, languageService);
    }

    public String formatPlayerCounters(LanguageService languageService) {
        return formatCounters(playerCounters, languageService);
    }

    private String formatCounters(Map<String, Integer> counters, LanguageService languageService) {
        if (counters.isEmpty()) {
            return languageService.message("stats.none");
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counters.entrySet());
        entries.sort((left, right) -> {
            int countCompare = right.getValue().compareTo(left.getValue());
            return countCompare != 0 ? countCompare : left.getKey().compareTo(right.getKey());
        });

        List<String> values = new ArrayList<>();
        for (int index = 0; index < Math.min(5, entries.size()); index++) {
            Map.Entry<String, Integer> entry = entries.get(index);
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join(", ", values);
    }
}
