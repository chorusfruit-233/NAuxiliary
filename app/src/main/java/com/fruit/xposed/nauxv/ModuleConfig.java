package com.fruit.xposed.nauxv;

import android.content.Context;
import android.content.SharedPreferences;

final class ModuleConfig {
    static final String PREFS_NAME = "nauxiliary_config";
    private static final String KEY_TRANSLATION_ENABLED = "translation_enabled";
    private static final String KEY_RUNTIME_TEXT_TRANSLATION_ENABLED = "runtime_text_translation_enabled";
    private static final String KEY_WEBVIEW_TRANSLATION_ENABLED = "webview_translation_enabled";
    private static final String KEY_AD_REMOVAL_ENABLED = "ad_removal_enabled";
    private static final String KEY_DEBUG_LOG_ENABLED = "debug_log_enabled";

    private volatile boolean loaded;
    private volatile boolean translationEnabled = true;
    private volatile boolean runtimeTextTranslationEnabled = true;
    private volatile boolean webViewTranslationEnabled = true;
    private volatile boolean adRemovalEnabled = true;
    private volatile boolean debugLogEnabled;

    void refresh(Context context) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        translationEnabled = preferences.getBoolean(KEY_TRANSLATION_ENABLED, true);
        runtimeTextTranslationEnabled = preferences.getBoolean(KEY_RUNTIME_TEXT_TRANSLATION_ENABLED, true);
        webViewTranslationEnabled = preferences.getBoolean(KEY_WEBVIEW_TRANSLATION_ENABLED, true);
        adRemovalEnabled = preferences.getBoolean(KEY_AD_REMOVAL_ENABLED, true);
        debugLogEnabled = preferences.getBoolean(KEY_DEBUG_LOG_ENABLED, false);
        loaded = true;
    }

    void save(
            Context context,
            boolean translationEnabled,
            boolean runtimeTextTranslationEnabled,
            boolean webViewTranslationEnabled,
            boolean adRemovalEnabled,
            boolean debugLogEnabled
    ) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences != null) {
            preferences.edit()
                    .putBoolean(KEY_TRANSLATION_ENABLED, translationEnabled)
                    .putBoolean(KEY_RUNTIME_TEXT_TRANSLATION_ENABLED, runtimeTextTranslationEnabled)
                    .putBoolean(KEY_WEBVIEW_TRANSLATION_ENABLED, webViewTranslationEnabled)
                    .putBoolean(KEY_AD_REMOVAL_ENABLED, adRemovalEnabled)
                    .putBoolean(KEY_DEBUG_LOG_ENABLED, debugLogEnabled)
                    .apply();
        }
        this.translationEnabled = translationEnabled;
        this.runtimeTextTranslationEnabled = runtimeTextTranslationEnabled;
        this.webViewTranslationEnabled = webViewTranslationEnabled;
        this.adRemovalEnabled = adRemovalEnabled;
        this.debugLogEnabled = debugLogEnabled;
        loaded = true;
    }

    boolean isTranslationEnabled() {
        return translationEnabled;
    }

    boolean isRuntimeTextTranslationEnabled() {
        return translationEnabled && runtimeTextTranslationEnabled;
    }

    boolean isRuntimeTextTranslationSwitchEnabled() {
        return runtimeTextTranslationEnabled;
    }

    boolean isWebViewTranslationEnabled() {
        return translationEnabled && webViewTranslationEnabled;
    }

    boolean isWebViewTranslationSwitchEnabled() {
        return webViewTranslationEnabled;
    }

    boolean isAdRemovalEnabled() {
        return adRemovalEnabled;
    }

    boolean isDebugLogEnabled() {
        return debugLogEnabled;
    }

    boolean isLoaded() {
        return loaded;
    }

    private SharedPreferences getPreferences(Context context) {
        if (context == null) {
            return null;
        }
        Context appContext = context.getApplicationContext();
        Context owner = appContext != null ? appContext : context;
        return owner.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
