package com.fruit.xposed.nauxv;

import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;

public class TranslationAssetsTest {
    private static final List<String> TRANSLATION_ASSETS = Arrays.asList(
            "strings.properties",
            "exact.properties",
            "phrases.properties"
    );

    @Test
    public void translationAssets_loadWithJavaProperties() throws Exception {
        for (String asset : TRANSLATION_ASSETS) {
            Properties properties = loadProperties(asset);

            assertFalse(asset + " should not be empty", properties.isEmpty());
        }
    }

    @Test
    public void translationAssets_valuesDoNotContainJapaneseKana() throws Exception {
        for (String asset : TRANSLATION_ASSETS) {
            Properties properties = loadProperties(asset);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);

                assertFalse(asset + " value contains Japanese kana for key: " + key,
                        containsJapaneseKana(value));
            }
        }
    }

    private static Properties loadProperties(String asset) throws Exception {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(assetPath(asset)),
                StandardCharsets.UTF_8
        )) {
            properties.load(reader);
        }
        return properties;
    }

    private static Path assetPath(String asset) {
        Path moduleRelative = Paths.get("src/main/assets/translations/zh-CN", asset);
        if (Files.exists(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("app/src/main/assets/translations/zh-CN", asset);
    }

    private static boolean containsJapaneseKana(String value) {
        for (int i = 0; i < value.length(); ) {
            int codePoint = value.codePointAt(i);
            if ((codePoint >= 0x3040 && codePoint <= 0x309f)
                    || (codePoint >= 0x30a0 && codePoint <= 0x30ff && codePoint != 0x30fb && codePoint != 0x30fc)
                    || (codePoint >= 0x31f0 && codePoint <= 0x31ff)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }
}
