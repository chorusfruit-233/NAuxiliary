package com.fruit.xposed.nauxv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class StringTranslations {
    private static final Locale FORMAT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    private final Properties properties;

    private StringTranslations(Properties properties) {
        this.properties = properties;
    }

    public static StringTranslations empty() {
        return new StringTranslations(new Properties());
    }

    public static StringTranslations load(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return new StringTranslations(properties);
    }

    static StringTranslations loadForTest(String content) throws IOException {
        return load(new StringReader(content));
    }

    public int size() {
        return properties.size();
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String replacePhrases(String source) {
        String result = source;
        List<String> keys = new ArrayList<>(properties.stringPropertyNames());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        for (String key : keys) {
            String value = properties.getProperty(key);
            if (value != null && !key.isEmpty()) {
                result = result.replace(key, value);
            }
        }
        return result.equals(source) ? null : result;
    }

    public String format(String template, Object[] args) {
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(FORMAT_LOCALE, template, args);
        } catch (IllegalArgumentException ignored) {
            return template;
        }
    }
}
