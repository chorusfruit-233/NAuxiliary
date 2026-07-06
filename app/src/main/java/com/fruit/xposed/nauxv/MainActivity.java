package com.fruit.xposed.nauxv;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {
    private static final String TARGET_PACKAGE = "jp.nicovideo.android";
    private static final String[] TRANSLATION_ASSETS = {
            "translations/zh-CN/strings.properties",
            "translations/zh-CN/exact.properties",
            "translations/zh-CN/phrases.properties"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        bindStatusInfo();
        bindActions();
    }

    private void bindStatusInfo() {
        setText(R.id.moduleVersionValue, getModuleVersionText());
        setText(R.id.targetAppValue, getTargetAppText());
        setText(R.id.translationAssetsValue, getString(
                R.string.translation_assets_value,
                countTranslationEntries()
        ));
    }

    private String getModuleVersionText() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = info.versionName != null ? info.versionName : "unknown";
            long versionCode = PackageInfoCompat.getLongVersionCode(info);
            return getString(R.string.module_version_value, versionName, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.module_version_value, "unknown", 0);
        }
    }

    private String getTargetAppText() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(TARGET_PACKAGE, 0);
            String versionName = info.versionName != null ? info.versionName : TARGET_PACKAGE;
            return getString(R.string.target_app_installed, versionName);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.target_app_missing);
        }
    }

    private int countTranslationEntries() {
        int count = 0;
        for (String assetPath : TRANSLATION_ASSETS) {
            count += countProperties(assetPath);
        }
        return count;
    }

    private int countProperties(String assetPath) {
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(
                getAssets().open(assetPath),
                StandardCharsets.UTF_8
        )) {
            properties.load(reader);
            return properties.size();
        } catch (IOException e) {
            return 0;
        }
    }

    private void bindActions() {
        findViewById(R.id.openTargetButton).setOnClickListener(v ->
                openPackage(TARGET_PACKAGE, R.string.toast_target_missing));
    }

    private void openPackage(String packageName, int missingToast) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent == null) {
            Toast.makeText(this, missingToast, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void setText(int viewId, String value) {
        TextView view = findViewById(viewId);
        view.setText(value);
    }
}
