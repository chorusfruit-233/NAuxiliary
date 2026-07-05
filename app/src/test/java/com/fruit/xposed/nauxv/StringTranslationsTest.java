package com.fruit.xposed.nauxv;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class StringTranslationsTest {
    @Test
    public void load_readsUtf8Properties() throws Exception {
        StringTranslations translations = StringTranslations.loadForTest("app_name=niconico 动画\n");

        assertEquals("niconico 动画", translations.get("app_name"));
        assertNull(translations.get("missing"));
    }

    @Test
    public void format_appliesArguments() throws Exception {
        StringTranslations translations = StringTranslations.loadForTest("message=连接到 %1$s\n");

        assertEquals("连接到 电视", translations.format(translations.get("message"), new Object[]{"电视"}));
    }

    @Test
    public void format_fallsBackToTemplateForBadArguments() throws Exception {
        StringTranslations translations = StringTranslations.loadForTest("message=连接到 %1$s\n");

        assertEquals("连接到 %1$s", translations.format(translations.get("message"), new Object[]{}));
    }
}
