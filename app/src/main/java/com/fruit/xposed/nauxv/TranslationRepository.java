package com.fruit.xposed.nauxv;

import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class TranslationRepository {
    private static final String TARGET_PACKAGE = "jp.nicovideo.android";
    private static final String STRING_TYPE = "string";
    private static final String PLURALS_TYPE = "plurals";
    private static final String ARRAY_TYPE = "array";
    private static final String STRING_ARRAY_TYPE = "string-array";
    private static final String ASSET_PATH = "assets/translations/zh-CN/strings.properties";
    private static final String EXACT_TEXT_ASSET_PATH = "assets/translations/zh-CN/exact.properties";
    private static final String PHRASE_ASSET_PATH = "assets/translations/zh-CN/phrases.properties";

    private final StringTranslations translations;
    private final StringTranslations exactTranslations;
    private final StringTranslations phraseTranslations;
    private final ModuleLogger logger;
    private final Map<Integer, Optional<String>> stringCache = new ConcurrentHashMap<>();
    private final Map<Integer, Optional<String>> pluralCache = new ConcurrentHashMap<>();

    private TranslationRepository(
            StringTranslations translations,
            StringTranslations exactTranslations,
            StringTranslations phraseTranslations,
            ModuleLogger logger
    ) {
        this.translations = translations;
        this.exactTranslations = exactTranslations;
        this.phraseTranslations = phraseTranslations;
        this.logger = logger;
    }

    public static TranslationRepository fromModuleApk(String moduleApkPath, ModuleLogger logger) {
        try (ZipFile zipFile = new ZipFile(moduleApkPath)) {
            StringTranslations translations = loadAsset(zipFile, ASSET_PATH, logger);
            StringTranslations exactTranslations = loadAsset(zipFile, EXACT_TEXT_ASSET_PATH, logger);
            StringTranslations phraseTranslations = loadAsset(zipFile, PHRASE_ASSET_PATH, logger);
            logger.info("Loaded zh-CN translations: " + translations.size()
                    + ", exact texts: " + exactTranslations.size()
                    + ", phrases: " + phraseTranslations.size());
            return new TranslationRepository(translations, exactTranslations, phraseTranslations, logger);
        } catch (IOException e) {
            logger.error("Failed to load translations", e);
            return new TranslationRepository(
                    StringTranslations.empty(),
                    StringTranslations.empty(),
                    StringTranslations.empty(),
                    logger
            );
        }
    }

    public String findString(Resources resources, int id) {
        return stringCache.computeIfAbsent(id, ignored -> Optional.ofNullable(resolve(resources, id, STRING_TYPE, "")))
                .orElse(null);
    }

    public String findQuantityString(Resources resources, int id) {
        return pluralCache.computeIfAbsent(id, ignored -> Optional.ofNullable(resolve(resources, id, PLURALS_TYPE, "plurals.")))
                .orElse(null);
    }

    public String findArrayItem(Resources resources, int id, int index) {
        ResourceName name = resolveName(resources, id);
        if (name == null || (!ARRAY_TYPE.equals(name.type) && !STRING_ARRAY_TYPE.equals(name.type))) {
            return null;
        }
        return translations.get("array." + name.entry + "." + index);
    }

    public String findExactText(CharSequence source) {
        return translateText(source);
    }

    public String translateText(CharSequence source) {
        if (source == null) {
            return null;
        }
        String text = source.toString();
        String exact = exactTranslations.get(text);
        if (exact != null) {
            return exact;
        }
        if (!containsJapaneseWriting(text)) {
            return null;
        }
        return phraseTranslations.replacePhrases(text);
    }

    public String format(String template, Object[] args) {
        return translations.format(template, args);
    }

    private static StringTranslations loadAsset(ZipFile zipFile, String path, ModuleLogger logger) throws IOException {
        ZipEntry entry = zipFile.getEntry(path);
        if (entry == null) {
            logger.warn("Translation asset missing: " + path);
            return StringTranslations.empty();
        }
        try (InputStream input = zipFile.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return StringTranslations.load(reader);
        }
    }

    private String resolve(Resources resources, int id, String expectedType, String keyPrefix) {
        ResourceName name = resolveName(resources, id);
        if (name == null || !expectedType.equals(name.type)) {
            return null;
        }
        return translations.get(keyPrefix + name.entry);
    }

    private ResourceName resolveName(Resources resources, int id) {
        try {
            String packageName = resources.getResourcePackageName(id);
            if (!TARGET_PACKAGE.equals(packageName)) {
                return null;
            }
            return new ResourceName(resources.getResourceTypeName(id), resources.getResourceEntryName(id));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean containsJapaneseWriting(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (isJapaneseWritingCodePoint(codePoint)) {
                return true;
            }
            i += Character.charCount(codePoint);
        }
        return false;
    }

    private static boolean isJapaneseWritingCodePoint(int codePoint) {
        return (codePoint >= 0x3040 && codePoint <= 0x30ff)
                || (codePoint >= 0x31f0 && codePoint <= 0x31ff)
                || (codePoint >= 0x3400 && codePoint <= 0x4dbf)
                || (codePoint >= 0x4e00 && codePoint <= 0x9fff)
                || (codePoint >= 0xf900 && codePoint <= 0xfaff)
                || (codePoint >= 0x20000 && codePoint <= 0x2ebef);
    }

    public interface ModuleLogger {
        void info(String message);

        void warn(String message);

        void error(String message, Throwable throwable);
    }

    private static final class ResourceName {
        private final String type;
        private final String entry;

        private ResourceName(String type, String entry) {
            this.type = type;
            this.entry = entry;
        }
    }
}
