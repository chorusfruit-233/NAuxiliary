package com.fruit.xposed.nauxv;

import android.app.Activity;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import io.github.libxposed.api.XposedModule;

public final class NAuxiliaryModule extends XposedModule {
    private static final String TAG = "NAuxiliary";
    private static final String TARGET_PACKAGE = "jp.nicovideo.android";
    private static final String COPYRIGHT_ASSET_URL = "file:///android_asset/copyright/copyright.html";
    private static final String COPYRIGHT_BASE_URL = "file:///android_asset/copyright/";
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
            hookComposeTextMethods(classLoader);
            hookPreferenceMethods(classLoader);
        } catch (Throwable throwable) {
            log(Log.ERROR, TAG, "Failed to install app translation hooks", throwable);
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
                    String translated = repository.findString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getStringFormat = Resources.class.getMethod("getString", int.class, Object[].class);
        hook(getStringFormat)
                .setId("nauxv.resources.getString.format")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = repository.findString(resources, id);
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
                    String translated = repository.findString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getTextDefault = Resources.class.getMethod("getText", int.class, CharSequence.class);
        hook(getTextDefault)
                .setId("nauxv.resources.getText.default")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = repository.findString(resources, id);
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
                    String translated = repository.findQuantityString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getQuantityString = Resources.class.getMethod("getQuantityString", int.class, int.class);
        hook(getQuantityString)
                .setId("nauxv.resources.getQuantityString")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = repository.findQuantityString(resources, id);
                    return translated != null ? translated : translateResult(chain.proceed());
                });

        Method getQuantityStringFormat = Resources.class.getMethod("getQuantityString", int.class, int.class, Object[].class);
        hook(getQuantityStringFormat)
                .setId("nauxv.resources.getQuantityString.format")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Resources resources = (Resources) chain.getThisObject();
                    int id = (Integer) chain.getArg(0);
                    String translated = repository.findQuantityString(resources, id);
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
        return repository.findString(typedArray.getResources(), resourceId);
    }

    private void hookTextViewMethods() throws NoSuchMethodException {
        Method setText = TextView.class.getMethod("setText", CharSequence.class);
        hook(setText)
                .setId("nauxv.textView.setText")
                .setExceptionMode(ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                    String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                    String translated = repository.findString(textView.getResources(), (Integer) chain.getArg(0));
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
                    String translated = repository.findString(textView.getResources(), (Integer) chain.getArg(0));
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
                    String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                    String translated = repository.findString(textView.getResources(), (Integer) chain.getArg(0));
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
                    String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                    String translated = repository.findExactText((CharSequence) chain.getArg(1));
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
                    String translated = repository.findString(context.getResources(), (Integer) chain.getArg(1));
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
        if (!COPYRIGHT_ASSET_URL.equals(url)) {
            return false;
        }
        webView.loadDataWithBaseURL(COPYRIGHT_BASE_URL, TRANSLATED_COPYRIGHT_HTML, "text/html", "UTF-8", null);
        return true;
    }

    private String translateWebContent(String data) {
        if (data == null) {
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
                    String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                    String translated = repository.findString(context.getResources(), (Integer) chain.getArg(0));
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
                        String translated = repository.findExactText((CharSequence) chain.getArg(0));
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
                        String translated = repository.findString(context.getResources(), (Integer) chain.getArg(0));
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
                            String translated = repository.translateText((String) chain.getArg(0));
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
            String item = repository.findArrayItem(resources, id, i);
            if (item == null) {
                item = repository.translateText(original[i]);
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
            String item = repository.findArrayItem(resources, id, i);
            if (item == null) {
                item = repository.translateText(original[i]);
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
            String item = repository.translateText(original[i]);
            if (item != null) {
                translated[i] = item;
                changed = true;
            }
        }
        return changed ? translated : original;
    }

    private Object translateResult(Object result) {
        if (result instanceof String) {
            String translated = repository.translateText((String) result);
            return translated != null ? translated : result;
        }
        if (result instanceof CharSequence) {
            String translated = repository.translateText((CharSequence) result);
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
        String translatedText = repository.findExactText(text);
        if (translatedText != null) {
            textView.setText(translatedText);
        }

        CharSequence hint = textView.getHint();
        String translatedHint = repository.findExactText(hint);
        if (translatedHint != null) {
            textView.setHint(translatedHint);
        }
    }

    private void translateViewContentDescription(View view) {
        CharSequence description = view.getContentDescription();
        String translated = repository.findExactText(description);
        if (translated != null) {
            view.setContentDescription(translated);
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
