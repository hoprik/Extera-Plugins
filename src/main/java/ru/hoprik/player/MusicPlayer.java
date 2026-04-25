package ru.hoprik.player;

import android.util.Log;
import okhttp3.OkHttpClient;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.api.ApiClient;
import ru.hoprik.player.api.interfaces.ICallback;
import ru.hoprik.player.api.objects.FindMusicInfo;
import ru.hoprik.player.api.providers.StatsFM;
import ru.hoprik.player.ui.player.MusicPlayerUI;
import ru.hoprik.player.ui.settings.components.CustomHeader;

import java.util.HashMap;
import java.util.Map;

public class MusicPlayer {
    private static final MusicPlayer instance = new MusicPlayer();
    private Class<?> lyricsClass;
    private Map<String, Map<String, String>> localizations;
    private Map<String, Boolean> settings = new HashMap<>();

    public MusicPlayer() {
        ApiClient.getInstance().setSocksProxy("127.0.0.1", 25565);

    }

    public static MusicPlayer getInstance() {
        return instance;
    }


    public void startPlayerUI(BaseFragment baseFragment) {
        OkHttpClient client = ApiClient.getInstance().getProxiedClient();
        new StatsFM(client).findMusicInfo("hoprik", new ICallback<FindMusicInfo>() {
            @Override
            public void onSuccess(FindMusicInfo item) {
                Log.e("MusicPlayer", "onSuccess: " + item.getArtists().get(0).getId());
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MusicPlayer", "onError: ", throwable);
            }
        });
        baseFragment.presentFragment(new MusicPlayerUI());
    }

    public void openBrowser(BaseFragment fragment){
        Browser.openUrl(fragment.getContext(), "https://t.me/PESSDES_Plugins/109");
    }

    public CustomHeader createHeader(BaseFragment fragment){
        return new CustomHeader(fragment.getContext(), fragment);
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

    public void setLyricsClass(Class<?> lyricsClass) {
        this.lyricsClass = lyricsClass;
    }

    public void setSettings(Map<String, Boolean> settings) {
        this.settings = settings;
    }

    public boolean isFeatureEnabled(String key, boolean defaultValue) {
        if (settings == null) return defaultValue;

        Boolean val = settings.get(key);
        return val != null ? val : defaultValue;
    }

    public Class<?> getLyricsClass() {
        return lyricsClass;
    }

}