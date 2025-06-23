// app/java/fr/lotfirais/berryai/util/AppSettingsManager.java
package fr.lotfirais.bbchat.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettingsManager {

    private static final String PREFS_NAME = "BerryAISettings";
    private static final String GEMINI_API_KEY_PREF = "gemini_api_key";
    private static final String GEMINI_MODEL_PREF = "gemini_model";
    private static final String GOOGLESEARCH_ENABLED_PREF = "googlesearch_enabled";
    public static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"; // Made public static final

    private final SharedPreferences prefs;

    public AppSettingsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getGeminiApiKey() {
        return prefs.getString(GEMINI_API_KEY_PREF, "");
    }

    public void saveGeminiApiKey(String apiKey) {
        prefs.edit().putString(GEMINI_API_KEY_PREF, apiKey).apply();
    }

    public String getGeminiModel() {
        return prefs.getString(GEMINI_MODEL_PREF, DEFAULT_GEMINI_MODEL);
    }

    public void saveGeminiModel(String model) {
        prefs.edit().putString(GEMINI_MODEL_PREF, model).apply();
    }

    public boolean isGoogleSearchEnabled() {
        return prefs.getBoolean(GOOGLESEARCH_ENABLED_PREF, false);
    }

    public void setGoogleSearchEnabled(boolean enabled) {
        prefs.edit().putBoolean(GOOGLESEARCH_ENABLED_PREF, enabled).apply();
    }
}