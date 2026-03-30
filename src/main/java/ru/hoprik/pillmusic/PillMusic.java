package ru.hoprik.pillmusic;

import android.content.Context;
import android.graphics.Color;
import com.exteragram.messenger.pillstack.core.PillRegistry;
import com.exteragram.messenger.pillstack.core.PillStackConfig;
import com.exteragram.messenger.pillstack.ui.pills.BasePill;
import com.exteragram.messenger.plugins.PluginsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import ru.hoprik.pillmusic.pill.MusicPill;


import java.util.HashMap;
import java.util.Map;

public class PillMusic {
    private static final PillMusic instance = new PillMusic();
    private Map<String, Map<String, String>> localizations;
    private Map<String, Boolean> settings = new HashMap<>();

    public PillMusic() {
    }

    public static PillMusic getInstance() {
        return instance;
    }

    public void register() {
        PillRegistry.register(new PillRegistry.PillInfo(71369790, "Stats FM", R.drawable.files_music, Color.parseColor("#FFEFA612"), Color.parseColor("#FFE77512"), new PillRegistry.PillCreator() {
            @Override
            public BasePill create(Context context, Theme.ResourcesProvider resourcesProvider) {
                return new MusicPill(context, resourcesProvider);
            }
        }));
        if (PluginsController.getInstance().getPluginSettingBoolean("pill_stats_fm", "enable_pill", true)){
            PillRegistry.activatePill(71369790);
        }
    }

    public void unregister() {
        PillRegistry.unregister(71369790);
    }

    public void setLocalizations(Map<String, Map<String, String>> localizations) {
        this.localizations = localizations;
    }

    public String getString(String key) {
        if (localizations == null) return key;

        String lang = LocaleController.getInstance().getCurrentLocale().getLanguage();
        if (lang.contains("_")) lang = lang.split("_")[0];

        Map<String, String> langMap = localizations.get(lang);

        if (langMap == null) {
            langMap = localizations.get("en");
        }

        if (langMap != null && langMap.containsKey(key)) {
            return langMap.get(key);
        }

        Map<String, String> enMap = localizations.get("en");
        if (enMap != null && enMap.containsKey(key)) {
            return enMap.get(key);
        }

        return key;
    }


    public void setSettings(Map<String, Boolean> settings) {
        this.settings = settings;
    }

    public boolean isFeatureEnabled(String key, boolean defaultValue) {
        if (settings == null) return defaultValue;

        Boolean val = settings.get(key);
        return val != null ? val : defaultValue;
    }

}