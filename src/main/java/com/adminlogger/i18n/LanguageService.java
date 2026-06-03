package com.adminlogger.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingFormatArgumentException;

public class LanguageService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<String, String> messages = new HashMap<>();

    public void loadLanguage(String langCode) {
        String langFile = "assets/adminlogger/lang/" + langCode + ".json";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(langFile)) {
            messages.clear();
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                jsonObject.entrySet().forEach(entry -> messages.put(entry.getKey(), entry.getValue().getAsString()));
            } else if (!"en_us".equals(langCode)) {
                LOGGER.warn("Language file {} not found! Loading English...", langFile);
                loadLanguage("en_us");
            } else {
                LOGGER.warn("Fallback language file {} not found.", langFile);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to load language file: {}", langFile, e);
        }
    }

    public String message(String key, Object... args) {
        String message = messages.getOrDefault(key, key);
        try {
            return String.format(message, args);
        } catch (MissingFormatArgumentException e) {
            LOGGER.warn("Format mismatch for key '{}': {}", key, e.getMessage());
            return message;
        }
    }
}
