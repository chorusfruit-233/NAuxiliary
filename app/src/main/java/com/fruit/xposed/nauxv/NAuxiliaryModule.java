package com.fruit.xposed.nauxv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.ref.WeakReference;

import io.github.libxposed.api.XposedModule;

public final class NAuxiliaryModule extends XposedModule {
    private static final String TAG = "NAuxiliary";
    private static final String TARGET_PACKAGE = "jp.nicovideo.android";
    private static final String COPYRIGHT_ASSET_URL = "file:///android_asset/copyright/copyright.html";
    private static final String COPYRIGHT_BASE_URL = "file:///android_asset/copyright/";
    private static final String SUPPORTER_RENDERER_ASSET_URL = "file:///android_asset/supporter_renderer/index.html";
    private static final String SUPPORTER_RENDERER_BASE_URL = "file:///android_asset/supporter_renderer/";
    private static final String SUPPORTER_RENDERER_SCRIPT_ASSET = "supporter_renderer/index.js";
    private static final String SETTINGS_FRAGMENT_CLASS = "jp.nicovideo.android.ui.setting.SettingFragment";
    private static final String SETTINGS_BUTTON_TAG = "nauxiliary_settings_button";
    private static final String SETTINGS_BUTTON_SPACER_TAG = "nauxiliary_settings_button_spacer";
    private static final String SETTINGS_FALLBACK_ROW_TAG = "nauxiliary_settings_fallback_row";
    private static final String SETTINGS_ENTRY_TITLE = "NAuxiliary";
    private static final String SETTINGS_ENTRY_SUMMARY = "\u7ffb\u8bd1\u4e0e\u589e\u5f3a\u8bbe\u7f6e";
    private static final String ABOUT_APP_JA = "\u3053\u306e\u30a2\u30d7\u30ea\u306b\u3064\u3044\u3066";
    private static final String ABOUT_APP_ZH = "\u5173\u4e8e\u672c\u5e94\u7528";
    private static final String CONFIG_DIALOG_TITLE = "NAuxiliary";
    private static final String CONFIG_TRANSLATION_ENABLED = "\u542f\u7528\u7ffb\u8bd1\u4e0e\u589e\u5f3a";
    private static final String CONFIG_RUNTIME_TRANSLATION_ENABLED = "\u7ffb\u8bd1\u5e94\u7528\u6587\u672c";
    private static final String CONFIG_WEBVIEW_TRANSLATION_ENABLED = "\u7ffb\u8bd1 WebView \u5185\u5bb9";
    private static final String CONFIG_AD_REMOVAL_ENABLED = "\u53bb\u9664\u5e7f\u544a";
    private static final String CONFIG_DEBUG_LOG_ENABLED = "\u8c03\u8bd5\u65e5\u5fd7";
    private static final String CONFIG_PREMIUM_UNLOCK = "\u89e3\u9501\u4f1a\u5458\u7279\u6743";
    private static final String CONFIG_SAVE = "\u4fdd\u5b58";
    private static final String CONFIG_CANCEL = "\u53d6\u6d88";
    private static final String CONFIG_SAVED = "\u5df2\u4fdd\u5b58\uff0c\u90e8\u5206\u9875\u9762\u9700\u91cd\u65b0\u8fdb\u5165\u6216\u91cd\u542f niconico";
    private static final String TRANSLATED_COPYRIGHT_HTML = "<!DOCTYPE html>"
            + "<html><head>"
            + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />"
            + "<meta http-equiv=\"Content-Script-Type\" content=\"text/javascript\" />"
            + "<title>niconico | &#29256;&#26435;&#20449;&#24687;</title>"
            + "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />"
            + "<meta name=\"viewport\" content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0\" />"
            + "</head><body><div id=\"content\">"
            + "<span style=\"color:#0099ff; font-size:18px\"><strong>&#29256;&#26435;&#20449;&#24687;</strong></span>"
            + "<div class=\"contentBlock\"><p>niconico (niconico &#21160;&#30011; / niconico &#30452;&#25773;)</p>"
            + "<p>(C) DWANGO Co., Ltd.</p></div>"
            + "</div></body></html>";

    private TranslationRepository repository;
    private final ModuleConfig config = new ModuleConfig();
    private WeakReference<Activity> currentActivity = new WeakReference<>(null);
    private boolean installed;
    private boolean appHooksInstalled;

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            detach();
            return;
        }
        installResourceHooks();
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) {
            return;
        }
        installResourceHooks();
        installAppHooks(param.getClassLoader());
    }

    @Override
    public boolean onHotReloading(HotReloadingParam param) {
        return true;
    }

    private void installResourceHooks() {
        if (installed) {
            return;
        }
        installed = true;
        repository = TranslationRepository.fromModuleApk(getModuleApplicationInfo().sourceDir, new XposedLogger());

        try {
            hookStringMethods();
            hookTextMethods();
            hookQuantityMethods();
            hookArrayMethods();
            hookTypedArrayMethods();
            hookTextViewMethods();
            hookViewMethods();
            hookActivityMethods();
            hookToastMethods();
            hookWebViewMethods();
            log(Log.INFO, TAG, "Resource translation hooks installed");
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to install resource translation hooks", throwable);
        }
    }

    private void installAppHooks(ClassLoader classLoader) {
        if (appHooksInstalled) {
            return;
        }
        appHooksInstalled = true;
        try {
            hookNicoSettingsEntry(classLoader);
            hookAboutAppComposeEntry(classLoader);
            hookAdRemoval(classLoader);
            hookComposeTextMethods(classLoader);
            hookPreferenceMethods(classLoader);
            hookPremiumUnlock(classLoader);
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to install app translation hooks", throwable);
        }
    }

    private void hookPremiumUnlock(ClassLoader classLoader) {
        int count = 0;
        count += hookNicoSessionGetter(classLoader);
        count += hookNicoSessionReturn(classLoader);
        count += hookSettingUiStatePremium(classLoader);
        count += hookDataModelPremiumWithDexKit(classLoader);
        if (count > 0) {
            log(Log.INFO, TAG, "Premium unlock hooks installed: " + count);
        } else {
            log(Log.WARN, TAG, "No premium unlock hooks were installed");
        }
    }

    private int hookNicoSessionGetter(ClassLoader classLoader) {
        try {
            Class<?> sessionClass = Class.forName(
                    "jp.co.dwango.niconico.domain.user.NicoSession", false, classLoader);
            Method isPremium = sessionClass.getDeclaredMethod("isPremium");
            isPremium.setAccessible(true);
            hook(isPremium)
                    .setId("nauxv.premium.nicoSession.isPremium")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!shouldUnlockPremium()) {
                            return chain.proceed();
                        }
                        return true;
                    });
            return 1;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook NicoSession.isPremium", throwable);
            return 0;
        }
    }

    private int hookNicoSessionReturn(ClassLoader classLoader) {
        try {
            Class<?> clientContextClass = Class.forName("aj.b", false, classLoader);
            Method jMethod = clientContextClass.getDeclaredMethod("j");
            jMethod.setAccessible(true);
            hook(jMethod)
                    .setId("nauxv.premium.clientContext.j")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if (!shouldUnlockPremium() || result == null) {
                            return result;
                        }
                        forcePremiumField(result);
                        return result;
                    });
            return 1;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook aj.b.j", throwable);
            return 0;
        }
    }

    private void forcePremiumField(Object nicoSession) {
        try {
            Field field = nicoSession.getClass().getDeclaredField("isPremium");
            field.setAccessible(true);
            makeFieldModifiable(field);
            field.setBoolean(nicoSession, true);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to force isPremium field on NicoSession", throwable);
        }
    }

    private void makeFieldModifiable(Field field) {
        try {
            Field accessFlagsField = Field.class.getDeclaredField("accessFlags");
            accessFlagsField.setAccessible(true);
            accessFlagsField.setInt(field, field.getModifiers() & ~0x10);
        } catch (NoSuchFieldException e) {
            try {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~0x10);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private int hookSettingUiStatePremium(ClassLoader classLoader) {
        int count = 0;
        try {
            Class<?> uiStateClass = Class.forName("gp.y1", false, classLoader);
            for (Method method : uiStateClass.getDeclaredMethods()) {
                if (method.getReturnType() != Boolean.TYPE || method.getParameterTypes().length != 0) {
                    continue;
                }
                String name = method.getName();
                if ("e".equals(name)) {
                    method.setAccessible(true);
                    hook(method)
                            .setId("nauxv.premium.settingUiState.e")
                            .setExceptionMode(ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                if (!shouldUnlockPremium()) {
                                    return chain.proceed();
                                }
                                return true;
                            });
                    count++;
                    break;
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook gp.y1.e", throwable);
        }
        return count;
    }

    private int hookDataModelPremiumWithDexKit(ClassLoader classLoader) {
        int count = 0;
        try (DexKitLocator locator = DexKitLocator.fromClassLoader(classLoader)) {
            if (!locator.isValid()) {
                return 0;
            }
            for (Class<?> clazz : locator.findClassesUsingStrings("isPremium")) {
                count += hookBooleanGettersOnClass(clazz);
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "DexKit failed to find premium data models", throwable);
        }
        return count;
    }

    private int hookBooleanGettersOnClass(Class<?> clazz) {
        int count = 0;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getReturnType() != Boolean.TYPE || method.getParameterTypes().length != 0) {
                continue;
            }
            String name = method.getName();
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                continue;
            }
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            method.setAccessible(true);
            try {
                hook(method)
                        .setId("nauxv.premium.dataModel." + clazz.getName() + "." + name)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            if (!shouldUnlockPremium()) {
                                return chain.proceed();
                            }
                            return true;
                        });
                count++;
            } catch (Throwable throwable) {
                log(Log.WARN, TAG, "Failed to hook boolean getter: "
                        + clazz.getName() + "." + name, throwable);
            }
        }
        return count;
    }

    private boolean shouldUnlockPremium() {
        ensureConfigLoaded();
        return config.isPremiumUnlockEnabled();
    }

    private void hookNicoSettingsEntry(ClassLoader classLoader) {
        Class<?> fragmentClass = findSettingFragmentClass(classLoader);
        if (fragmentClass == null) {
            log(Log.WARN, TAG, "SettingFragment was not found; NAuxiliary settings entry skipped");
            return;
        }

        Method onCreateView = findDeclaredOnCreateView(fragmentClass);
        if (onCreateView == null) {
            log(Log.WARN, TAG, "SettingFragment.onCreateView was not found: " + fragmentClass.getName());
            return;
        }
        onCreateView.setAccessible(true);
        hook(onCreateView)
                .setId("nauxv.nicoSettingsEntry")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof View)) {
                        return result;
                    }
                    View root = (View) result;
                    Context context = root.getContext();
                    config.refresh(context);
                    return root;
                });
        log(Log.INFO, TAG, "NAuxiliary settings entry hook installed: " + fragmentClass.getName());
    }

    private Class<?> findSettingFragmentClass(ClassLoader classLoader) {
        try {
            return Class.forName(SETTINGS_FRAGMENT_CLASS, false, classLoader);
        } catch (ClassNotFoundException ignored) {
            // Fall back to DexKit string fingerprints below.
        }

        try (DexKitLocator locator = DexKitLocator.fromClassLoader(classLoader)) {
            if (!locator.isValid()) {
                return null;
            }
            String[][] fingerprints = {
                    {"\u8a2d\u5b9a"},
                    {"\u30a2\u30ab\u30a6\u30f3\u30c8"},
                    {"\u30d8\u30eb\u30d7"},
                    {"setting"}
            };
            for (String[] fingerprint : fingerprints) {
                Class<?> candidate = chooseSettingFragmentCandidate(locator.findClassesUsingStrings(fingerprint));
                if (candidate != null) {
                    return candidate;
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "DexKit failed to locate SettingFragment", throwable);
        }
        return null;
    }

    private Class<?> chooseSettingFragmentCandidate(List<Class<?>> candidates) {
        Class<?> fallback = null;
        for (Class<?> candidate : candidates) {
            if (!isFragmentSubclass(candidate) || findDeclaredOnCreateView(candidate) == null) {
                continue;
            }
            String name = candidate.getName().toLowerCase(Locale.ROOT);
            if (name.contains("setting")) {
                return candidate;
            }
            if (fallback == null) {
                fallback = candidate;
            }
        }
        return fallback;
    }

    private boolean isFragmentSubclass(Class<?> candidate) {
        Class<?> current = candidate;
        while (current != null) {
            String name = current.getName();
            if ("androidx.fragment.app.Fragment".equals(name) || "android.app.Fragment".equals(name)) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private Method findDeclaredOnCreateView(Class<?> fragmentClass) {
        try {
            return fragmentClass.getDeclaredMethod("onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private void hookAboutAppComposeEntry(ClassLoader classLoader) {
        try {
            Class<?> settingComponents = Class.forName("hp.e0", false, classLoader);
            Class<?> function0 = Class.forName("qr.a", false, classLoader);
            Class<?> composer = Class.forName("androidx.compose.runtime.Composer", false, classLoader);
            Method settingTextItemByRes = settingComponents.getDeclaredMethod(
                    "k",
                    int.class,
                    function0,
                    composer,
                    int.class,
                    int.class
            );
            Method settingTextItemByText = settingComponents.getDeclaredMethod(
                    "l",
                    String.class,
                    function0,
                    composer,
                    int.class,
                    int.class
            );
            settingTextItemByRes.setAccessible(true);
            settingTextItemByText.setAccessible(true);
            int aboutAppTitleRes = Class.forName("mf.l0", false, classLoader)
                    .getField("config_application_info")
                    .getInt(null);
            Object nauxiliaryClick = createNauxiliaryClickCallback(function0, classLoader);
            hook(settingTextItemByRes)
                    .setId("nauxv.setting.aboutAppComposeEntry")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        if ((Integer) chain.getArg(0) == aboutAppTitleRes) {
                            try {
                                settingTextItemByText.invoke(
                                        null,
                                        SETTINGS_ENTRY_TITLE,
                                        nauxiliaryClick,
                                        chain.getArg(2),
                                        0,
                                        0
                                );
                            } catch (Throwable throwable) {
                                log(Log.WARN, TAG, "Failed to append NAuxiliary Compose setting row", throwable);
                            }
                        }
                        return result;
                    });
            log(Log.INFO, TAG, "NAuxiliary Compose row append installed on hp.e0.k");
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook about-app Compose setting entry", throwable);
        }
    }

    private Object createNauxiliaryClickCallback(Class<?> function0, ClassLoader classLoader) {
        Object unit = findKotlinUnit(classLoader);
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("invoke".equals(name) && method.getParameterTypes().length == 0) {
                Activity activity = currentActivity.get();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> showConfigDialog(activity));
                }
                return unit;
            }
            if ("toString".equals(name)) {
                return "NAuxiliarySettingsClick";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            return unit;
        };
        return Proxy.newProxyInstance(classLoader, new Class<?>[]{function0}, handler);
    }

    private Object findKotlinUnit(ClassLoader classLoader) {
        String[] candidates = {"cr.j0", "kotlin.Unit"};
        String[] fields = {"f61931a", "INSTANCE"};
        for (String className : candidates) {
            try {
                Class<?> unitClass = Class.forName(className, false, classLoader);
                for (String fieldName : fields) {
                    try {
                        Field field = unitClass.getField(fieldName);
                        return field.get(null);
                    } catch (NoSuchFieldException ignored) {
                        // Try the next known singleton field.
                    }
                }
            } catch (Throwable ignored) {
                // Try the next known Unit class.
            }
        }
        return null;
    }

    private void attachSettingsButtonToTitleBar(View root, int attempt) {
        Context context = root.getContext();
        if (context == null) {
            return;
        }

        Activity activity = findActivity(context);
        View decor = activity != null ? activity.getWindow().getDecorView() : root;
        ViewGroup titleBar = findSettingsTitleContainer(decor);
        if (titleBar == null) {
            titleBar = findSettingsTitleContainer(root);
        }
        if (titleBar == null) {
            titleBar = findTitleBar(root);
        }
        if (titleBar == null) {
            titleBar = findTitleBar(decor);
        }
        if (titleBar != null) {
            if (findTaggedView(decor, SETTINGS_BUTTON_TAG) != null) {
                return;
            }

            View button = createSettingsEntryButton(context);
            button.setTag(SETTINGS_BUTTON_TAG);
            addButtonToTitleBar(titleBar, button);
            return;
        }

        if (attempt < 8) {
            root.postDelayed(() -> attachSettingsButtonToTitleBar(root, attempt + 1), 150);
            return;
        }

        if (!attachAboutAppFallback(root)) {
            log(Log.WARN, TAG, "Settings title bar and about-app row were not found; NAuxiliary entry skipped");
        }
    }

    private View createSettingsEntryButton(Context context) {
        TextView button = new TextView(context);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 48));
        button.setMinWidth(dp(context, 88));
        button.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        button.setSingleLine(true);
        button.setText(SETTINGS_ENTRY_TITLE);
        button.setTextSize(14);
        button.setTextColor(resolveColor(context, android.R.attr.colorAccent, Color.rgb(0, 153, 255)));
        button.setClickable(true);
        button.setFocusable(true);
        Drawable background = resolveDrawable(context, android.R.attr.selectableItemBackgroundBorderless);
        if (background == null) {
            background = resolveDrawable(context, android.R.attr.selectableItemBackground);
        }
        if (background != null) {
            button.setBackground(background);
        }
        button.setOnClickListener(view -> showConfigDialog(view.getContext()));
        return button;
    }

    private void addButtonToTitleBar(ViewGroup titleBar, View button) {
        if (titleBar instanceof LinearLayout
                && ((LinearLayout) titleBar).getOrientation() == LinearLayout.HORIZONTAL) {
            addButtonToHorizontalTitleBar((LinearLayout) titleBar, button);
            return;
        }

        ViewGroup.LayoutParams params = createTitleBarButtonParams(titleBar, button.getContext());
        try {
            titleBar.addView(button, params);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to add NAuxiliary button to title bar", throwable);
        }
    }

    private void addButtonToHorizontalTitleBar(LinearLayout titleBar, View button) {
        Context context = button.getContext();
        if (findTaggedView(titleBar, SETTINGS_BUTTON_SPACER_TAG) == null) {
            View spacer = new View(context);
            spacer.setTag(SETTINGS_BUTTON_SPACER_TAG);
            LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                    0,
                    1,
                    1f
            );
            titleBar.addView(spacer, spacerParams);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Math.max(dp(context, 40), Math.min(dp(context, 56), titleBar.getHeight()))
        );
        params.gravity = Gravity.CENTER_VERTICAL;
        params.setMargins(dp(context, 8), 0, dp(context, 8), 0);
        try {
            titleBar.addView(button, params);
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to add NAuxiliary button to horizontal title bar", throwable);
        }
    }

    private ViewGroup.LayoutParams createTitleBarButtonParams(ViewGroup titleBar, Context context) {
        int height = Math.max(dp(context, 40), Math.min(dp(context, 56), titleBar.getHeight()));
        String className = titleBar.getClass().getName();
        ViewGroup.LayoutParams toolbarParams = createToolbarLayoutParams(titleBar, height);
        if (toolbarParams != null) {
            return toolbarParams;
        }
        if (titleBar instanceof FrameLayout) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    height,
                    Gravity.END | Gravity.CENTER_VERTICAL
            );
            params.setMargins(0, 0, dp(context, 8), 0);
            return params;
        }
        if (titleBar instanceof RelativeLayout) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    height
            );
            params.addRule(RelativeLayout.ALIGN_PARENT_END);
            params.addRule(RelativeLayout.CENTER_VERTICAL);
            params.setMargins(0, 0, dp(context, 8), 0);
            return params;
        }
        ViewGroup.LayoutParams constraintParams = createConstraintLayoutParams(titleBar, context, height);
        if (constraintParams != null) {
            return constraintParams;
        }
        if (titleBar instanceof LinearLayout) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    height
            );
            params.gravity = Gravity.CENTER_VERTICAL;
            params.setMargins(dp(context, 8), 0, dp(context, 8), 0);
            return params;
        }
        ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                height
        );
        params.setMargins(dp(context, 8), 0, dp(context, 8), 0);
        return params;
    }

    private ViewGroup.LayoutParams createToolbarLayoutParams(ViewGroup titleBar, int height) {
        String name = titleBar.getClass().getName().toLowerCase(Locale.ROOT);
        if (!name.contains("toolbar")) {
            return null;
        }
        String[] layoutParamClasses = {
                "androidx.appcompat.widget.Toolbar$LayoutParams",
                "android.widget.Toolbar$LayoutParams"
        };
        for (String className : layoutParamClasses) {
            try {
                Class<?> paramsClass = Class.forName(className, false, titleBar.getClass().getClassLoader());
                Object params = paramsClass
                        .getConstructor(int.class, int.class, int.class)
                        .newInstance(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                height,
                                Gravity.END | Gravity.CENTER_VERTICAL
                        );
                if (params instanceof ViewGroup.LayoutParams) {
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, dp(titleBar.getContext(), 8), 0);
                    }
                    return (ViewGroup.LayoutParams) params;
                }
            } catch (Throwable ignored) {
                // Try the next Toolbar implementation.
            }
        }
        return null;
    }

    private ViewGroup.LayoutParams createConstraintLayoutParams(ViewGroup titleBar, Context context, int height) {
        if (!titleBar.getClass().getName().toLowerCase(Locale.ROOT).contains("constraintlayout")) {
            return null;
        }
        try {
            Class<?> paramsClass = Class.forName(
                    "androidx.constraintlayout.widget.ConstraintLayout$LayoutParams",
                    false,
                    titleBar.getClass().getClassLoader()
            );
            Object params = paramsClass
                    .getConstructor(int.class, int.class)
                    .newInstance(ViewGroup.LayoutParams.WRAP_CONTENT, height);
            paramsClass.getField("endToEnd").setInt(params, 0);
            paramsClass.getField("topToTop").setInt(params, 0);
            paramsClass.getField("bottomToBottom").setInt(params, 0);
            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, dp(context, 8), 0);
            }
            return (ViewGroup.LayoutParams) params;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ViewGroup findTitleBar(View view) {
        if (!(view instanceof ViewGroup) || view.getVisibility() != View.VISIBLE) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        if (isTitleBarCandidate(group)) {
            return group;
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup found = findTitleBar(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private ViewGroup findSettingsTitleContainer(View root) {
        View titleView = findSettingsTitleView(root);
        if (titleView == null) {
            return null;
        }

        ViewParent parent = titleView.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            if (isInsertableTitleContainer(group)) {
                return group;
            }
            parent = group.getParent();
        }
        return null;
    }

    private boolean isInsertableTitleContainer(ViewGroup group) {
        if (group.getVisibility() != View.VISIBLE) {
            return false;
        }
        String className = group.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("toolbar") || className.contains("appbar") || className.contains("actionbar")) {
            return true;
        }

        int height = group.getHeight();
        if (height < dp(group.getContext(), 40) || height > dp(group.getContext(), 112)) {
            return false;
        }
        if (group instanceof LinearLayout) {
            return ((LinearLayout) group).getOrientation() == LinearLayout.HORIZONTAL;
        }
        return group instanceof FrameLayout
                || group instanceof RelativeLayout
                || className.contains("constraintlayout");
    }

    private boolean isTitleBarCandidate(ViewGroup group) {
        String className = group.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("toolbar") || className.contains("appbar") || className.contains("actionbar")) {
            return true;
        }

        int height = group.getHeight();
        if (height < dp(group.getContext(), 40) || height > dp(group.getContext(), 96)) {
            return false;
        }
        return containsSettingsTitle(group);
    }

    private boolean containsSettingsTitle(View view) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (isSettingsTitle(text)) {
                return true;
            }
        }
        CharSequence description = view.getContentDescription();
        if (isSettingsTitle(description)) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (containsSettingsTitle(group.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    private View findSettingsTitleView(View view) {
        if (view instanceof TextView && isSettingsTitle(((TextView) view).getText())) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findSettingsTitleView(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean isSettingsTitle(CharSequence text) {
        if (text == null) {
            return false;
        }
        String value = text.toString();
        return value.contains("\u8a2d\u5b9a")
                || value.contains("\u8bbe\u7f6e")
                || value.toLowerCase(Locale.ROOT).contains("settings");
    }

    private boolean attachAboutAppFallback(View root) {
        if (!(root instanceof ViewGroup)) {
            return false;
        }
        if (findTaggedView(root, SETTINGS_BUTTON_TAG) != null
                || findTaggedView(root, SETTINGS_FALLBACK_ROW_TAG) != null) {
            return true;
        }

        TextView aboutTitle = findAboutAppTextView(root);
        if (aboutTitle == null) {
            Activity activity = findActivity(root.getContext());
            if (activity != null) {
                aboutTitle = findAboutAppTextView(activity.getWindow().getDecorView());
            }
        }
        if (aboutTitle == null) {
            return false;
        }

        View row = findSettingsRow(aboutTitle);
        if (row == null) {
            row = aboutTitle;
        }
        if (insertFallbackRowAfter(row)) {
            return true;
        }
        reuseAboutAppRow(row, aboutTitle);
        return true;
    }

    private TextView findAboutAppTextView(View view) {
        if (view instanceof TextView && isAboutAppText(((TextView) view).getText())) {
            return (TextView) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            TextView found = findAboutAppTextView(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private boolean isAboutAppText(CharSequence text) {
        if (text == null) {
            return false;
        }
        String value = text.toString();
        return ABOUT_APP_JA.equals(value) || ABOUT_APP_ZH.equals(value);
    }

    private View findSettingsRow(View titleView) {
        View current = titleView;
        ViewParent parent = titleView.getParent();
        while (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            int height = group.getHeight();
            if ((group.isClickable() || group.isFocusable() || height >= dp(group.getContext(), 48))
                    && height <= dp(group.getContext(), 112)
                    && group.getParent() instanceof ViewGroup) {
                return group;
            }
            current = group;
            parent = current.getParent();
        }
        return current;
    }

    private boolean insertFallbackRowAfter(View templateRow) {
        ViewParent parent = templateRow.getParent();
        if (!(parent instanceof ViewGroup)) {
            return false;
        }
        ViewGroup parentGroup = (ViewGroup) parent;
        if (findTaggedView(parentGroup, SETTINGS_FALLBACK_ROW_TAG) != null) {
            return true;
        }
        int index = parentGroup.indexOfChild(templateRow);
        if (index < 0) {
            return false;
        }

        View row = createFallbackSettingsRow(templateRow.getContext(), templateRow);
        row.setTag(SETTINGS_FALLBACK_ROW_TAG);
        try {
            parentGroup.addView(row, index + 1, createFallbackRowLayoutParams(templateRow));
            return true;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to insert NAuxiliary fallback row after about-app row", throwable);
            return false;
        }
    }

    private View createFallbackSettingsRow(Context context, View templateRow) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Math.max(dp(context, 64), templateRow.getHeight()));
        row.setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 8));
        row.setClickable(true);
        row.setFocusable(true);
        Drawable background = resolveDrawable(context, android.R.attr.selectableItemBackground);
        if (background != null) {
            row.setBackground(background);
        }

        TextView title = new TextView(context);
        title.setText(SETTINGS_ENTRY_TITLE);
        title.setTextSize(16);
        title.setTextColor(resolveColor(context, android.R.attr.textColorPrimary, Color.WHITE));
        title.setSingleLine(true);

        TextView summary = new TextView(context);
        summary.setText(SETTINGS_ENTRY_SUMMARY);
        summary.setTextSize(12);
        summary.setTextColor(resolveColor(context, android.R.attr.textColorSecondary, Color.LTGRAY));
        summary.setSingleLine(true);

        row.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.addView(summary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setOnClickListener(view -> showConfigDialog(view.getContext()));
        return row;
    }

    private ViewGroup.LayoutParams createFallbackRowLayoutParams(View templateRow) {
        ViewGroup.LayoutParams original = templateRow.getLayoutParams();
        if (original instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) original);
            params.height = original.height;
            return params;
        }
        if (original instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams((ViewGroup.MarginLayoutParams) original);
            params.height = original.height;
            return params;
        }
        int height = original != null ? original.height : ViewGroup.LayoutParams.WRAP_CONTENT;
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
    }

    private void reuseAboutAppRow(View row, TextView aboutTitle) {
        aboutTitle.setText(SETTINGS_ENTRY_TITLE);
        row.setTag(SETTINGS_FALLBACK_ROW_TAG);
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(view -> showConfigDialog(view.getContext()));
    }

    private View findTaggedView(View view, Object tag) {
        if (tag.equals(view.getTag())) {
            return view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            View found = findTaggedView(group.getChildAt(i), tag);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private void showConfigDialog(Context context) {
        config.refresh(context);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 20), dp(context, 8), dp(context, 20), 0);

        Switch translationSwitch = createConfigSwitch(context, CONFIG_TRANSLATION_ENABLED, config.isTranslationEnabled());
        Switch runtimeSwitch = createConfigSwitch(context, CONFIG_RUNTIME_TRANSLATION_ENABLED, config.isRuntimeTextTranslationSwitchEnabled());
        Switch webViewSwitch = createConfigSwitch(context, CONFIG_WEBVIEW_TRANSLATION_ENABLED, config.isWebViewTranslationSwitchEnabled());
        Switch adRemovalSwitch = createConfigSwitch(context, CONFIG_AD_REMOVAL_ENABLED, config.isAdRemovalEnabled());
        Switch debugSwitch = createConfigSwitch(context, CONFIG_DEBUG_LOG_ENABLED, config.isDebugLogEnabled());
        Switch premiumSwitch = createConfigSwitch(context, CONFIG_PREMIUM_UNLOCK, config.isPremiumUnlockEnabled());

        content.addView(translationSwitch);
        content.addView(runtimeSwitch);
        content.addView(webViewSwitch);
        content.addView(adRemovalSwitch);
        content.addView(debugSwitch);
        content.addView(premiumSwitch);

        new AlertDialog.Builder(context)
                .setTitle(CONFIG_DIALOG_TITLE)
                .setView(content)
                .setNegativeButton(CONFIG_CANCEL, null)
                .setPositiveButton(CONFIG_SAVE, (dialog, which) -> {
                    config.save(
                            context,
                            translationSwitch.isChecked(),
                            runtimeSwitch.isChecked(),
                            webViewSwitch.isChecked(),
                            adRemovalSwitch.isChecked(),
                            debugSwitch.isChecked(),
                            premiumSwitch.isChecked()
                    );
                    Toast.makeText(context, CONFIG_SAVED, Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private Switch createConfigSwitch(Context context, String text, boolean checked) {
        Switch configSwitch = new Switch(context);
        configSwitch.setText(text);
        configSwitch.setTextSize(15);
        configSwitch.setChecked(checked);
        configSwitch.setPadding(0, dp(context, 8), 0, dp(context, 8));
        return configSwitch;
    }

    private void hookAdRemoval(ClassLoader classLoader) {
        int count = 0;
        count += hookInAppAdFactory(classLoader);
        count += hookInAppAdController(classLoader);
        count += hookInAppAdViewClass(classLoader, "jp.nicovideo.android.ui.inappad.InAppAdMobView");
        count += hookInAppAdViewClass(classLoader, "jp.nicovideo.android.ui.inappad.InAppAdGenerationView");
        count += hookComposeAdBanner(classLoader);
        count += hookPlayerVideoAdView(classLoader);
        if (count > 0) {
            log(Log.INFO, TAG, "Ad removal hooks installed: " + count);
        } else {
            log(Log.WARN, TAG, "No ad removal hooks were installed");
        }
    }

    private int hookInAppAdFactory(ClassLoader classLoader) {
        int count = 0;
        try {
            Class<?> factoryClass = Class.forName("ul.i", false, classLoader);
            count += hookInAppAdFactoryMethods(factoryClass, "known");
        } catch (ClassNotFoundException ignored) {
            // Fall back to DexKit below.
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook known in-app ad factory", throwable);
        }
        if (count == 0) {
            count += hookInAppAdFactoryWithDexKit(classLoader);
        }
        return count;
    }

    private int hookInAppAdFactoryWithDexKit(ClassLoader classLoader) {
        int count = 0;
        List<Method> candidates = new ArrayList<>();
        try (DexKitLocator locator = DexKitLocator.fromClassLoader(classLoader)) {
            if (!locator.isValid()) {
                return 0;
            }
            addMethods(candidates, locator.findMethodsUsingStrings("adUnitId", "baseFrameSizeDp"));
            addMethods(candidates, locator.findMethodsUsingStrings("oxInAppAd"));
            for (Method method : candidates) {
                if (isInAppAdFactoryMethod(method)) {
                    count += hookInAppAdFactoryMethod(method, "dexkit");
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "DexKit failed to locate in-app ad factory", throwable);
        }
        return count;
    }

    private int hookInAppAdFactoryMethods(Class<?> factoryClass, String source) {
        int count = 0;
        for (Method method : factoryClass.getDeclaredMethods()) {
            if (isInAppAdFactoryMethod(method)) {
                count += hookInAppAdFactoryMethod(method, source);
            }
        }
        return count;
    }

    private boolean isInAppAdFactoryMethod(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length > 0
                && Context.class.isAssignableFrom(parameterTypes[0])
                && isAdFacadeClass(method.getReturnType());
    }

    private boolean isAdFacadeClass(Class<?> candidate) {
        if (!candidate.isInterface()) {
            return false;
        }
        try {
            return View.class.isAssignableFrom(candidate.getMethod("getAdView").getReturnType());
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }

    private int hookInAppAdFactoryMethod(Method method, String source) {
        method.setAccessible(true);
        try {
            hook(method)
                    .setId("nauxv.ads.inAppFactory." + source + "." + method.getDeclaringClass().getName() + "." + method.getName())
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Context context = (Context) chain.getArg(0);
                        if (!shouldRemoveAds(context)) {
                            return chain.proceed();
                        }
                        return createNoOpAdFacade(method.getReturnType(), context);
                    });
            return 1;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook in-app ad factory method: " + method, throwable);
            return 0;
        }
    }

    private int hookInAppAdController(ClassLoader classLoader) {
        try {
            Class<?> controllerClass = Class.forName("uf.g", false, classLoader);
            int count = 0;
            count += hookAdControllerMethod(controllerClass, "h");
            count += hookAdControllerMethod(controllerClass, "j");
            count += hookAdControllerMethod(controllerClass, "k");
            count += hookAdControllerMethod(controllerClass, "m");
            return count;
        } catch (ClassNotFoundException ignored) {
            return 0;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook in-app ad controller", throwable);
            return 0;
        }
    }

    private int hookAdControllerMethod(Class<?> controllerClass, String methodName) {
        int count = 0;
        for (Method method : controllerClass.getDeclaredMethods()) {
            if (!methodName.equals(method.getName()) || method.getReturnType() != Void.TYPE) {
                continue;
            }
            method.setAccessible(true);
            try {
                hook(method)
                        .setId("nauxv.ads.controller." + methodName + "." + count)
                        .setExceptionMode(ExceptionMode.PROTECTIVE)
                        .intercept(chain -> {
                            if (!shouldRemoveAds(chain.getThisObject())) {
                                return chain.proceed();
                            }
                            hideControllerContainer(chain.getThisObject());
                            return null;
                        });
                count++;
            } catch (Throwable throwable) {
                log(Log.WARN, TAG, "Failed to hook ad controller method: " + method, throwable);
            }
        }
        return count;
    }

    private int hookInAppAdViewClass(ClassLoader classLoader, String className) {
        try {
            Class<?> adViewClass = Class.forName(className, false, classLoader);
            int count = 0;
            count += hookAdViewNoArgMethod(adViewClass, "start");
            count += hookAdViewNoArgMethod(adViewClass, "a");
            return count;
        } catch (ClassNotFoundException ignored) {
            return 0;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook ad view class: " + className, throwable);
            return 0;
        }
    }

    private int hookAdViewNoArgMethod(Class<?> adViewClass, String methodName) {
        try {
            Method method = adViewClass.getMethod(methodName);
            if (method.getReturnType() != Void.TYPE) {
                return 0;
            }
            hook(method)
                    .setId("nauxv.ads.view." + adViewClass.getName() + "." + methodName)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!shouldRemoveAds(chain.getThisObject())) {
                            return chain.proceed();
                        }
                        hideAdView(chain.getThisObject());
                        return null;
                    });
            return 1;
        } catch (NoSuchMethodException ignored) {
            return 0;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook ad view method: " + adViewClass.getName() + "." + methodName, throwable);
            return 0;
        }
    }

    private int hookComposeAdBanner(ClassLoader classLoader) {
        int count = hookKnownComposeAdBanner(classLoader);
        if (count == 0) {
            count += hookComposeAdBannerWithDexKit(classLoader);
        }
        return count;
    }

    private int hookKnownComposeAdBanner(ClassLoader classLoader) {
        try {
            Class<?> containerClass = Class.forName("jk.c", false, classLoader);
            int count = 0;
            for (Method method : containerClass.getDeclaredMethods()) {
                if (isComposeAdBannerMethod(method)) {
                    count += hookComposeAdBannerMethod(method, "known");
                }
            }
            return count;
        } catch (ClassNotFoundException ignored) {
            return 0;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook known Compose ad banner", throwable);
            return 0;
        }
    }

    private int hookComposeAdBannerWithDexKit(ClassLoader classLoader) {
        int count = 0;
        try (DexKitLocator locator = DexKitLocator.fromClassLoader(classLoader)) {
            if (!locator.isValid()) {
                return 0;
            }
            for (Method method : locator.findMethodsUsingStrings("jp.nicovideo.android.ui.base.compose.container.AdBannerContainer (AdBannerContainer.kt:33)")) {
                if (isComposeAdBannerMethod(method)) {
                    count += hookComposeAdBannerMethod(method, "dexkit");
                }
            }
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "DexKit failed to locate Compose ad banner", throwable);
        }
        return count;
    }

    private boolean isComposeAdBannerMethod(Method method) {
        if (method.getReturnType() != Void.TYPE) {
            return false;
        }
        for (Class<?> parameterType : method.getParameterTypes()) {
            if ("androidx.compose.runtime.Composer".equals(parameterType.getName())) {
                return true;
            }
        }
        return false;
    }

    private int hookComposeAdBannerMethod(Method method, String source) {
        method.setAccessible(true);
        try {
            hook(method)
                    .setId("nauxv.ads.composeBanner." + source + "." + method.getDeclaringClass().getName() + "." + method.getName())
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object adEntry = chain.getArgs().isEmpty() ? null : chain.getArg(0);
                        View adView = getAdEntryView(adEntry);
                        if (!shouldRemoveAds(adView != null ? adView : adEntry)) {
                            return chain.proceed();
                        }
                        hideAdView(adView);
                        stopAdEntry(adEntry);
                        return null;
                    });
            return 1;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook Compose ad banner method: " + method, throwable);
            return 0;
        }
    }

    private int hookPlayerVideoAdView(ClassLoader classLoader) {
        try {
            Class<?> playerAdClass = Class.forName("jp.nicovideo.android.ui.player.panel.PlayerVideoAdvertisementView", false, classLoader);
            int count = 0;
            for (Method method : playerAdClass.getDeclaredMethods()) {
                if (("l".equals(method.getName()) && method.getParameterTypes().length == 0)
                        || ("n".equals(method.getName()) && method.getParameterTypes().length == 1)) {
                    count += hookPlayerVideoAdMethod(method);
                }
            }
            return count;
        } catch (ClassNotFoundException ignored) {
            return 0;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook player video ad view", throwable);
            return 0;
        }
    }

    private int hookPlayerVideoAdMethod(Method method) {
        method.setAccessible(true);
        try {
            hook(method)
                    .setId("nauxv.ads.playerVideo." + method.getName())
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (!shouldRemoveAds(chain.getThisObject())) {
                            return chain.proceed();
                        }
                        hideAdView(chain.getThisObject());
                        if ("n".equals(method.getName())) {
                            invokePlayerVideoAdSkip(chain.getThisObject());
                        }
                        return null;
                    });
            return 1;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook player video ad method: " + method, throwable);
            return 0;
        }
    }

    private Object createNoOpAdFacade(Class<?> facadeClass, Context context) {
        View adView = createEmptyAdView(context);
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("getAdView".equals(name) && method.getParameterTypes().length == 0) {
                return adView;
            }
            if ("toString".equals(name)) {
                return "NAuxiliaryNoOpAdFacade";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            return defaultValue(method.getReturnType());
        };
        return Proxy.newProxyInstance(facadeClass.getClassLoader(), new Class<?>[]{facadeClass}, handler);
    }

    private View createEmptyAdView(Context context) {
        FrameLayout view = new FrameLayout(context);
        view.setVisibility(View.GONE);
        view.setMinimumWidth(0);
        view.setMinimumHeight(0);
        view.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
        return view;
    }

    private Object defaultValue(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Character.TYPE) {
            return (char) 0;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Float.TYPE) {
            return 0f;
        }
        if (type == Double.TYPE) {
            return 0d;
        }
        return null;
    }

    private boolean shouldRemoveAds(Object source) {
        Context context = extractContext(source);
        if (context == null) {
            context = currentActivity.get();
        }
        if (context != null) {
            config.refresh(context);
        }
        return config.isAdRemovalEnabled();
    }

    private Context extractContext(Object source) {
        if (source instanceof Context) {
            return (Context) source;
        }
        if (source instanceof View) {
            return ((View) source).getContext();
        }
        return null;
    }

    private void hideControllerContainer(Object controller) {
        try {
            Method method = controller.getClass().getMethod("f");
            Object container = method.invoke(controller);
            hideAdView(container);
        } catch (Throwable ignored) {
            // Some app versions may not expose the controller container getter.
        }
    }

    private View getAdEntryView(Object adEntry) {
        if (adEntry == null) {
            return null;
        }
        try {
            Method method = adEntry.getClass().getMethod("b");
            Object view = method.invoke(adEntry);
            return view instanceof View ? (View) view : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void stopAdEntry(Object adEntry) {
        if (adEntry == null) {
            return;
        }
        try {
            Method method = adEntry.getClass().getMethod("h");
            method.invoke(adEntry);
        } catch (Throwable ignored) {
            // Best effort cleanup for the decompiled rh.b holder.
        }
    }

    private void hideAdView(Object object) {
        if (!(object instanceof View)) {
            return;
        }
        View view = (View) object;
        view.setVisibility(View.GONE);
        view.setMinimumWidth(0);
        view.setMinimumHeight(0);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.width = 0;
            params.height = 0;
            view.setLayoutParams(params);
        }
        if (view instanceof ViewGroup) {
            ((ViewGroup) view).removeAllViews();
        }
    }

    private void invokePlayerVideoAdSkip(Object playerAdView) {
        Class<?> current = playerAdView.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!fieldType.getName().startsWith("jp.nicovideo.android.ui.player.panel.PlayerVideoAdvertisementView$")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object listener = field.get(playerAdView);
                    if (listener == null) {
                        continue;
                    }
                    Method skip = fieldType.getMethod("a");
                    skip.invoke(listener);
                    return;
                } catch (Throwable ignored) {
                    // Try the next candidate listener field.
                }
            }
            current = current.getSuperclass();
        }
    }

    private void addMethods(List<Method> target, List<Method> source) {
        for (Method method : source) {
            if (!target.contains(method)) {
                target.add(method);
            }
        }
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private int getStatusBarHeight(Context context) {
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId == 0) {
            return dp(context, 24);
        }
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    private int resolveColor(Context context, int attr, int fallback) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true)) {
            return fallback;
        }
        if (value.resourceId != 0) {
            try {
                return context.getColor(value.resourceId);
            } catch (Throwable ignored) {
                return fallback;
            }
        }
        return value.data;
    }

    private Drawable resolveDrawable(Context context, int attr) {
        TypedValue value = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, value, true) || value.resourceId == 0) {
            return null;
        }
        try {
            return context.getDrawable(value.resourceId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void hookStringMethods() throws NoSuchMethodException {
        Method getString = Resources.class.getMethod("getString", int.class);
        hook(getString)
                .setId("nauxv.resources.getString")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getStringFormat = Resources.class.getMethod("getString", int.class, Object[].class);
        hook(getStringFormat)
                .setId("nauxv.resources.getString.format")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findString(resources, id);
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return repository.format(translated, (Object[]) chain.getArg(1));
                });
    }

    private void hookTextMethods() throws NoSuchMethodException {
        Method getText = Resources.class.getMethod("getText", int.class);
        hook(getText)
                .setId("nauxv.resources.getText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getTextDefault = Resources.class.getMethod("getText", int.class, CharSequence.class);
        hook(getTextDefault)
                .setId("nauxv.resources.getText.default")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });
    }

    private void hookQuantityMethods() throws NoSuchMethodException {
        Method getQuantityText = Resources.class.getMethod("getQuantityText", int.class, int.class);
        hook(getQuantityText)
                .setId("nauxv.resources.getQuantityText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findQuantityString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getQuantityString = Resources.class.getMethod("getQuantityString", int.class, int.class);
        hook(getQuantityString)
                .setId("nauxv.resources.getQuantityString")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findQuantityString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getQuantityStringFormat = Resources.class.getMethod("getQuantityString", int.class, int.class, Object[].class);
        hook(getQuantityStringFormat)
                .setId("nauxv.resources.getQuantityString.format")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = findQuantityString(resources, id);
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return repository.format(translated, (Object[]) chain.getArg(2));
                });
    }

    private void hookArrayMethods() throws NoSuchMethodException {
        Method getStringArray = Resources.class.getMethod("getStringArray", int.class);
        hook(getStringArray)
                .setId("nauxv.resources.getStringArray")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof String[])) {
                        return translateResult(result);
                    }
                    return translateArray((Resources) chain.getThisObject(), (Integer) chain.getArg(0), (String[]) result);
                });

        Method getTextArray = Resources.class.getMethod("getTextArray", int.class);
        hook(getTextArray)
                .setId("nauxv.resources.getTextArray")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof CharSequence[])) {
                        return translateResult(result);
                    }
                    return translateArray((Resources) chain.getThisObject(), (Integer) chain.getArg(0), (CharSequence[]) result);
                });
    }

    private void hookTypedArrayMethods() throws NoSuchMethodException {
        Method getText = TypedArray.class.getMethod("getText", int.class);
        hook(getText)
                .setId("nauxv.typedArray.getText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    TypedArray typedArray = (TypedArray) chain.getThisObject();
                    String translated = findTypedArrayString(typedArray, (Integer) chain.getArg(0));
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getString = TypedArray.class.getMethod("getString", int.class);
        hook(getString)
                .setId("nauxv.typedArray.getString")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    TypedArray typedArray = (TypedArray) chain.getThisObject();
                    String translated = findTypedArrayString(typedArray, (Integer) chain.getArg(0));
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getTextArray = TypedArray.class.getMethod("getTextArray", int.class);
        hook(getTextArray)
                .setId("nauxv.typedArray.getTextArray")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    if (!(result instanceof CharSequence[])) {
                        return translateResult(result);
                    }
                    TypedArray typedArray = (TypedArray) chain.getThisObject();
                    int resourceId = typedArray.getResourceId((Integer) chain.getArg(0), 0);
                    if (resourceId == 0) {
                        return result;
                    }
                    return translateArray(typedArray.getResources(), resourceId, (CharSequence[]) result);
                });
    }

    private String findTypedArrayString(TypedArray typedArray, int index) {
        int resourceId = typedArray.getResourceId(index, 0);
        if (resourceId == 0) {
            return null;
        }
        return findString(typedArray.getResources(), resourceId);
    }

    private void hookTextViewMethods() throws NoSuchMethodException {
        Method setText = TextView.class.getMethod("setText", CharSequence.class);
        hook(setText)
                .setId("nauxv.textView.setText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{translated});
                });

        Method setTextWithBuffer = TextView.class.getMethod("setText", CharSequence.class, TextView.BufferType.class);
        hook(setTextWithBuffer)
                .setId("nauxv.textView.setText.buffer")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{translated, chain.getArg(1)});
                });

        Method setTextResource = TextView.class.getMethod("setText", int.class);
        hook(setTextResource)
                .setId("nauxv.textView.setText.resource")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    TextView textView = (TextView) chain.getThisObject();
                    String translated = findString(textView.getResources(), (Integer) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    textView.setText(translated);
                    return null;
                });

        Method setTextResourceWithBuffer = TextView.class.getMethod("setText", int.class, TextView.BufferType.class);
        hook(setTextResourceWithBuffer)
                .setId("nauxv.textView.setText.resource.buffer")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    TextView textView = (TextView) chain.getThisObject();
                    String translated = findString(textView.getResources(), (Integer) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    textView.setText(translated, (TextView.BufferType) chain.getArg(1));
                    return null;
                });

        Method setHint = TextView.class.getMethod("setHint", CharSequence.class);
        hook(setHint)
                .setId("nauxv.textView.setHint")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{translated});
                });

        Method setHintResource = TextView.class.getMethod("setHint", int.class);
        hook(setHintResource)
                .setId("nauxv.textView.setHint.resource")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    TextView textView = (TextView) chain.getThisObject();
                    String translated = findString(textView.getResources(), (Integer) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    textView.setHint(translated);
                    return null;
                });
    }

    private void hookViewMethods() throws NoSuchMethodException {
        Method setContentDescription = View.class.getMethod("setContentDescription", CharSequence.class);
        hook(setContentDescription)
                .setId("nauxv.view.setContentDescription")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{translated});
                });

        Method onAttachedToWindow = View.class.getDeclaredMethod("onAttachedToWindow");
        onAttachedToWindow.setAccessible(true);
        hook(onAttachedToWindow)
                .setId("nauxv.view.onAttachedToWindow")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    translateViewTree((View) chain.getThisObject());
                    return result;
                });
    }

    private void hookActivityMethods() throws NoSuchMethodException {
        Method onResume = Activity.class.getDeclaredMethod("onResume");
        onResume.setAccessible(true);
        hook(onResume)
                .setId("nauxv.activity.onResume")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Activity activity = (Activity) chain.getThisObject();
                    currentActivity = new WeakReference<>(activity);
                    config.refresh(activity);
                    translateViewTree(activity.getWindow().getDecorView());
                    return result;
                });
    }

    private void hookToastMethods() throws NoSuchMethodException {
        Method makeText = Toast.class.getMethod("makeText", Context.class, CharSequence.class, int.class);
        hook(makeText)
                .setId("nauxv.toast.makeText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(1));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{chain.getArg(0), translated, chain.getArg(2)});
                });

        Method makeTextResource = Toast.class.getMethod("makeText", Context.class, int.class, int.class);
        hook(makeTextResource)
                .setId("nauxv.toast.makeText.resource")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Context context = (Context) chain.getArg(0);
                    String translated = findString(context.getResources(), (Integer) chain.getArg(1));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return Toast.makeText(context, translated, (Integer) chain.getArg(2));
                });
    }

    private void hookWebViewMethods() throws NoSuchMethodException {
        Method loadUrl = WebView.class.getMethod("loadUrl", String.class);
        hook(loadUrl)
                .setId("nauxv.webView.loadUrl")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    WebView webView = (WebView) chain.getThisObject();
                    if (loadTranslatedLocalAsset(webView, (String) chain.getArg(0))) {
                        return null;
                    }
                    return chain.proceed();
                });

        Method loadUrlWithHeaders = WebView.class.getMethod("loadUrl", String.class, Map.class);
        hook(loadUrlWithHeaders)
                .setId("nauxv.webView.loadUrl.headers")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    WebView webView = (WebView) chain.getThisObject();
                    if (loadTranslatedLocalAsset(webView, (String) chain.getArg(0))) {
                        return null;
                    }
                    return chain.proceed();
                });

        Method loadData = WebView.class.getMethod("loadData", String.class, String.class, String.class);
        hook(loadData)
                .setId("nauxv.webView.loadData")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = translateWebContent((String) chain.getArg(0));
                    if (translated == null) {
                        return chain.proceed();
                    }
                    return chain.proceed(new Object[]{translated, chain.getArg(1), chain.getArg(2)});
                });

        Method loadDataWithBaseUrl = WebView.class.getMethod(
                "loadDataWithBaseURL",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );
        hook(loadDataWithBaseUrl)
                .setId("nauxv.webView.loadDataWithBaseURL")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = translateWebContent((String) chain.getArg(1));
                    if (translated == null) {
                        return chain.proceed();
                    }
                    return chain.proceed(new Object[]{
                            chain.getArg(0),
                            translated,
                            chain.getArg(2),
                            chain.getArg(3),
                            chain.getArg(4)
                    });
                });
    }

    private boolean loadTranslatedLocalAsset(WebView webView, String url) {
        config.refresh(webView.getContext());
        if (!shouldTranslateWebContent()) {
            return false;
        }
        if (COPYRIGHT_ASSET_URL.equals(url)) {
            webView.loadDataWithBaseURL(COPYRIGHT_BASE_URL, TRANSLATED_COPYRIGHT_HTML, "text/html", "UTF-8", null);
            return true;
        }
        if (SUPPORTER_RENDERER_ASSET_URL.equals(url)) {
            return loadTranslatedSupporterRenderer(webView);
        }
        return false;
    }

    private boolean loadTranslatedSupporterRenderer(WebView webView) {
        try {
            String script = translateSupporterRendererScript(readTargetAsset(webView, SUPPORTER_RENDERER_SCRIPT_ASSET));
            String html = "<!DOCTYPE html>"
                    + "<html><head>"
                    + "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">"
                    + "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>"
                    + "<link href=\"https://fonts.googleapis.com/css2?family=M+PLUS+Rounded+1c&display=swap\" rel=\"stylesheet\">"
                    + "<link rel=\"stylesheet\" href=\"./index.css\">"
                    + "</head><body><div id=\"app\" width=\"1280\" height=\"720\"></div>"
                    + "<script>" + script.replace("</script", "<\\/script") + "</script>"
                    + "</body></html>";
            webView.loadDataWithBaseURL(SUPPORTER_RENDERER_BASE_URL, html, "text/html", "UTF-8", null);
            return true;
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to load translated supporter renderer", throwable);
            return false;
        }
    }

    private String readTargetAsset(WebView webView, String path) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                webView.getContext().getAssets().open(path),
                StandardCharsets.UTF_8
        ))) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
    }

    private String translateSupporterRendererScript(String script) {
        return script
                .replace("\"ja-jp\":\"\u63d0\u3000\u4f9b\"", "\"ja-jp\":\"\u8d5e\u3000\u52a9\"")
                .replace("\"zh-tw\":\"\u8d0a\u3000\u52a9\"", "\"zh-tw\":\"\u8d5e\u3000\u52a9\"")
                .replace("\"\u5de6\u5074\u30b5\u30a4\u30c9\u30d0\u30ca\u30fc\"", "\"\u5de6\u4fa7\u8fb9\u680f\u6a2a\u5e45\"")
                .replace("\"\u53f3\u5074\u30b5\u30a4\u30c9\u30d0\u30ca\u30fc\"", "\"\u53f3\u4fa7\u8fb9\u680f\u6a2a\u5e45\"")
                .replace("\"ready()\u304c\u5b8c\u4e86\u3057\u3066\u3044\u307e\u305b\u3093\"", "\"ready() \u5c1a\u672a\u5b8c\u6210\"");
    }

    private String translateWebContent(String data) {
        if (data == null) {
            return null;
        }
        if (!shouldTranslateWebContent()) {
            return null;
        }
        return repository.translateText(data);
    }

    private void hookComposeTextMethods(ClassLoader classLoader) {
        hookComposeTextClass(classLoader, "androidx.compose.material3.TextKt");
        hookComposeTextClass(classLoader, "androidx.compose.material.TextKt");
        hookComposeTextClass(classLoader, "androidx.compose.foundation.text.BasicTextKt");
    }

    private void hookPreferenceMethods(ClassLoader classLoader) {
        try {
            Class<?> preferenceClass = Class.forName("androidx.preference.Preference", false, classLoader);
            Method getContext = preferenceClass.getMethod("getContext");
            hookPreferenceTextSetter(preferenceClass, "setTitle");
            hookPreferenceTextSetter(preferenceClass, "setSummary");
            hookPreferenceTextGetter(preferenceClass, "getTitle");
            hookPreferenceTextGetter(preferenceClass, "getSummary");
            hookPreferenceResourceSetter(preferenceClass, getContext, "setTitle");
            hookPreferenceResourceSetter(preferenceClass, getContext, "setSummary");
            hookPreferenceBindViewHolder(classLoader, preferenceClass);
            hookDialogPreferenceMethods(classLoader);
            hookListPreferenceMethods(classLoader);
            log(Log.INFO, TAG, "Preference translation hooks installed");
        } catch (ClassNotFoundException ignored) {
            // The app version may not include AndroidX Preference.
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook Preference text methods", throwable);
        }
    }

    private void hookPreferenceTextSetter(Class<?> preferenceClass, String methodName) throws NoSuchMethodException {
        Method method = preferenceClass.getMethod(methodName, CharSequence.class);
        hook(method)
                .setId("nauxv.preference." + methodName)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = findExactText((CharSequence) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    return chain.proceed(new Object[]{translated});
                });
    }

    private void hookPreferenceTextGetter(Class<?> preferenceClass, String methodName) throws NoSuchMethodException {
        Method method = preferenceClass.getMethod(methodName);
        hook(method)
                .setId("nauxv.preference." + methodName)
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> translateResult(chain.proceed()));
    }

    private void hookPreferenceBindViewHolder(ClassLoader classLoader, Class<?> preferenceClass) {
        try {
            Class<?> holderClass = Class.forName("androidx.preference.PreferenceViewHolder", false, classLoader);
            Field itemView = Class.forName("androidx.recyclerview.widget.RecyclerView$ViewHolder", false, classLoader)
                    .getField("itemView");
            Method method = preferenceClass.getMethod("onBindViewHolder", holderClass);
            hook(method)
                    .setId("nauxv.preference.onBindViewHolder")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        translateViewTree((View) itemView.get(chain.getArg(0)));
                        return result;
                    });
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook Preference.onBindViewHolder", throwable);
        }
    }

    private void hookPreferenceResourceSetter(Class<?> preferenceClass, Method getContext, String methodName) throws NoSuchMethodException {
        Method resourceMethod = preferenceClass.getMethod(methodName, int.class);
        Method textMethod = preferenceClass.getMethod(methodName, CharSequence.class);
        hook(resourceMethod)
                .setId("nauxv.preference." + methodName + ".resource")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Context context = (Context) getContext.invoke(chain.getThisObject());
                    String translated = findString(context.getResources(), (Integer) chain.getArg(0));
                    if (translated == null) {
                        return translateResult(chain.proceed());
                    }
                    textMethod.invoke(chain.getThisObject(), translated);
                    return null;
                });
    }

    private void hookDialogPreferenceMethods(ClassLoader classLoader) {
        hookPreferenceSubclassTextSetter(classLoader, "androidx.preference.DialogPreference", "setDialogTitle");
        hookPreferenceSubclassTextSetter(classLoader, "androidx.preference.DialogPreference", "setDialogMessage");
        hookPreferenceSubclassTextSetter(classLoader, "androidx.preference.DialogPreference", "setPositiveButtonText");
        hookPreferenceSubclassTextSetter(classLoader, "androidx.preference.DialogPreference", "setNegativeButtonText");
        hookPreferenceSubclassResourceSetter(classLoader, "androidx.preference.DialogPreference", "setDialogTitle");
        hookPreferenceSubclassResourceSetter(classLoader, "androidx.preference.DialogPreference", "setDialogMessage");
        hookPreferenceSubclassResourceSetter(classLoader, "androidx.preference.DialogPreference", "setPositiveButtonText");
        hookPreferenceSubclassResourceSetter(classLoader, "androidx.preference.DialogPreference", "setNegativeButtonText");
    }

    private void hookListPreferenceMethods(ClassLoader classLoader) {
        hookPreferenceEntriesSetter(classLoader, "androidx.preference.ListPreference");
        hookPreferenceEntriesSetter(classLoader, "androidx.preference.MultiSelectListPreference");
        hookPreferenceSubclassTextGetter(classLoader, "androidx.preference.ListPreference", "getEntry");
    }

    private void hookPreferenceSubclassTextGetter(ClassLoader classLoader, String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className, false, classLoader);
            Method method = targetClass.getMethod(methodName);
            hook(method)
                    .setId("nauxv.preference." + className + "." + methodName)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> translateResult(chain.proceed()));
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook " + className + "." + methodName, throwable);
        }
    }

    private void hookPreferenceSubclassTextSetter(ClassLoader classLoader, String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className, false, classLoader);
            Method method = targetClass.getMethod(methodName, CharSequence.class);
            hook(method)
                    .setId("nauxv.preference." + className + "." + methodName)
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        String translated = findExactText((CharSequence) chain.getArg(0));
                        if (translated == null) {
                            return translateResult(chain.proceed());
                        }
                        return chain.proceed(new Object[]{translated});
                    });
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook " + className + "." + methodName, throwable);
        }
    }

    private void hookPreferenceSubclassResourceSetter(ClassLoader classLoader, String className, String methodName) {
        try {
            Class<?> targetClass = Class.forName(className, false, classLoader);
            Method getContext = targetClass.getMethod("getContext");
            Method resourceMethod = targetClass.getMethod(methodName, int.class);
            Method textMethod = targetClass.getMethod(methodName, CharSequence.class);
            hook(resourceMethod)
                    .setId("nauxv.preference." + className + "." + methodName + ".resource")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Context context = (Context) getContext.invoke(chain.getThisObject());
                        String translated = findString(context.getResources(), (Integer) chain.getArg(0));
                        if (translated == null) {
                            return translateResult(chain.proceed());
                        }
                        textMethod.invoke(chain.getThisObject(), translated);
                        return null;
                    });
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook " + className + "." + methodName + " resource", throwable);
        }
    }

    private void hookPreferenceEntriesSetter(ClassLoader classLoader, String className) {
        try {
            Class<?> targetClass = Class.forName(className, false, classLoader);
            Method setEntries = targetClass.getMethod("setEntries", CharSequence[].class);
            hook(setEntries)
                    .setId("nauxv.preference." + className + ".setEntries")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        CharSequence[] translated = translateTextArray((CharSequence[]) chain.getArg(0));
                        if (translated == chain.getArg(0)) {
                            return translateResult(chain.proceed());
                        }
                        return chain.proceed(new Object[]{translated});
                    });

            Method getContext = targetClass.getMethod("getContext");
            Method setEntriesResource = targetClass.getMethod("setEntries", int.class);
            hook(setEntriesResource)
                    .setId("nauxv.preference." + className + ".setEntries.resource")
                    .setExceptionMode(ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Context context = (Context) getContext.invoke(chain.getThisObject());
                        CharSequence[] original = context.getResources().getTextArray((Integer) chain.getArg(0));
                        CharSequence[] translated = translateTextArray(original);
                        if (translated == original) {
                            return translateResult(chain.proceed());
                        }
                        setEntries.invoke(chain.getThisObject(), new Object[]{translated});
                        return null;
                    });
        } catch (Throwable throwable) {
            log(Log.WARN, TAG, "Failed to hook " + className + ".setEntries", throwable);
        }
    }

    private void hookComposeTextClass(ClassLoader classLoader, String className) {
        try {
            Class<?> textClass = Class.forName(className, false, classLoader);
            Method[] methods = textClass.getDeclaredMethods();
            int count = 0;
            for (Method method : methods) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0 || parameterTypes[0] != String.class) {
                    continue;
                }
                String id = "nauxv.compose.text." + className + "." + count++;
                try {
                    hook(method)
                            .setId(id)
                            .setExceptionMode(ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                            String translated = translateText((String) chain.getArg(0));
                            if (translated == null) {
                                return chain.proceed();
                            }
                                Object[] args = chain.getArgs().toArray();
                                args[0] = translated;
                                return chain.proceed(args);
                            });
                } catch (Throwable throwable) {
                    log(Log.WARN, TAG, "Failed to hook Compose text method: " + method, throwable);
                }
            }
            if (count > 0) {
                log(Log.INFO, TAG, "Compose text hooks installed for " + className + ": " + count);
            }
        } catch (ClassNotFoundException ignored) {
            // The app version may not include every Compose text class.
        }
    }

    private String[] translateArray(Resources resources, int id, String[] original) {
        String[] translated = Arrays.copyOf(original, original.length);
        boolean changed = false;
        for (int i = 0; i < translated.length; i++) {
            String item = findArrayItem(resources, id, i);
            if (item == null) {
                item = translateText(original[i]);
            }
            if (item != null) {
                translated[i] = item;
                changed = true;
            }
        }
        return changed ? translated : original;
    }

    private CharSequence[] translateArray(Resources resources, int id, CharSequence[] original) {
        CharSequence[] translated = Arrays.copyOf(original, original.length);
        boolean changed = false;
        for (int i = 0; i < translated.length; i++) {
            String item = findArrayItem(resources, id, i);
            if (item == null) {
                item = translateText(original[i]);
            }
            if (item != null) {
                translated[i] = item;
                changed = true;
            }
        }
        return changed ? translated : original;
    }

    private CharSequence[] translateTextArray(CharSequence[] original) {
        if (original == null) {
            return null;
        }
        CharSequence[] translated = Arrays.copyOf(original, original.length);
        boolean changed = false;
        for (int i = 0; i < translated.length; i++) {
            String item = translateText(original[i]);
            if (item != null) {
                translated[i] = item;
                changed = true;
            }
        }
        return changed ? translated : original;
    }

    private Object translateResult(Object result) {
        if (result instanceof String) {
            String translated = translateText((String) result);
            return translated != null ? translated : result;
        }
        if (result instanceof CharSequence) {
            String translated = translateText((CharSequence) result);
            return translated != null ? translated : result;
        }
        return result;
    }

    private void translateViewTree(View view) {
        if (view == null) {
            return;
        }
        translateViewContentDescription(view);
        if (view instanceof TextView) {
            translateTextView((TextView) view);
        }
        if (!(view instanceof ViewGroup)) {
            return;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            translateViewTree(group.getChildAt(i));
        }
    }

    private void translateTextView(TextView textView) {
        CharSequence text = textView.getText();
        String translatedText = findExactText(text);
        if (translatedText != null) {
            textView.setText(translatedText);
        }

        CharSequence hint = textView.getHint();
        String translatedHint = findExactText(hint);
        if (translatedHint != null) {
            textView.setHint(translatedHint);
        }

    }

    private void translateViewContentDescription(View view) {
        CharSequence description = view.getContentDescription();
        String translated = findExactText(description);
        if (translated != null) {
            view.setContentDescription(translated);
        }
    }

    private String findString(Resources resources, int id) {
        if (!shouldTranslateRuntimeText()) {
            return null;
        }
        return repository.findString(resources, id);
    }

    private String findQuantityString(Resources resources, int id) {
        if (!shouldTranslateRuntimeText()) {
            return null;
        }
        return repository.findQuantityString(resources, id);
    }

    private String findArrayItem(Resources resources, int id, int index) {
        if (!shouldTranslateRuntimeText()) {
            return null;
        }
        return repository.findArrayItem(resources, id, index);
    }

    private String findExactText(CharSequence source) {
        if (!shouldTranslateRuntimeText()) {
            return null;
        }
        return repository.findExactText(source);
    }

    private String translateText(CharSequence source) {
        if (!shouldTranslateRuntimeText()) {
            return null;
        }
        return repository.translateText(source);
    }

    private boolean shouldTranslateRuntimeText() {
        ensureConfigLoaded();
        return config.isRuntimeTextTranslationEnabled();
    }

    private boolean shouldTranslateWebContent() {
        ensureConfigLoaded();
        return config.isWebViewTranslationEnabled();
    }

    private void ensureConfigLoaded() {
        if (config.isLoaded()) {
            return;
        }
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentApplication = activityThread.getMethod("currentApplication");
            Object application = currentApplication.invoke(null);
            if (application instanceof Context) {
                config.refresh((Context) application);
            }
        } catch (Throwable ignored) {
            // Before Application is attached, defaults stay enabled.
        }
    }

    private final class XposedLogger implements TranslationRepository.ModuleLogger {
        @Override
        public void info(String message) {
            log(Log.INFO, TAG, message);
        }

        @Override
        public void warn(String message) {
            log(Log.WARN, TAG, message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            log(Log.ERROR, TAG, message, throwable);
        }
    }
}
