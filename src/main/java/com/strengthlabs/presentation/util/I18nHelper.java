package com.strengthlabs.presentation.util;

import java.util.Locale;

public final class I18nHelper {
    private I18nHelper() {}

    public static String pick(String en, String es, Locale locale) {
        return "es".equals(locale.getLanguage()) && es != null && !es.isBlank() ? es : en;
    }
}
