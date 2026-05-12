package ru.hoprik.player;

import android.util.Log;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import ru.hoprik.player.api.ApiClient;
import ru.hoprik.player.audio.AudioPlayer;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.holder.AudioSource;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.hooks.HookRegister;
import ru.hoprik.player.ui.player.MusicPlayerUI;
import ru.hoprik.player.ui.settings.components.CustomHeader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicPlayer {
    private static final MusicPlayer instance = new MusicPlayer();
    private Class<?> lyricsClass;
    private Map<String, Map<String, String>> localizations;
    private Map<String, Boolean> settings = new HashMap<>();
    private HookRegister register;
    private AudioPlayer audioPlayer;

    public MusicPlayer() {
        ApiClient.getInstance().setSocksProxy("127.0.0.1", 25565);
    }

    public void load() {
        Log.d("MusicPlayer", "MusicPlayer created");
        audioPlayer = new AudioPlayer();
        register = new HookRegister();
        try {
            register.registerHooks();
        } catch (NoSuchMethodException e) {
            Log.e("MusicPlayer", "MusicPlayer HOOK REGISTER ERROR: ", e);
        }
    }

    public static MusicPlayer getInstance() {
        return instance;
    }

    public void destroy() {
        if (register != null) {
            register.unregisterHooks();
            register = null;
        }
        if (audioPlayer != null) {
            audioPlayer = null;
        }
    }

    public void startPlayerUI(BaseFragment baseFragment) {
        test();
        baseFragment.presentFragment(new MusicPlayerUI());
    }

    private void test(){
        List<Artist> artists = new ArrayList<>();
        artists.add(new Artist("0", "Mayot", null, new ArrayList<>(), new ArrayList<>(), false, true));
        List<AudioElement> audioElements = new ArrayList<>();
        audioElements.add(new AudioElement(AudioSource.ofUrl("https://fine.sunproxy.net/file/aXRqYUJQRDYzUVpnNkpSbzV3eXpqbU80Snh1VHVGYlNENnB4aElzTTFIbHl1RDRHbTM3UVlCbVpuVCtGSnhzdXFYUTBFbEErMUVOSjRuSWVpdXRsbVJ1S3VQd3lsK1FhbVhlR0pseDNaMnc9/MAYOT_-_Logika_(GuruMP3.com).mp3"),
                new Track("0", "Логика", artists, 100, 0, null, null, false, true)));
        audioElements.add(new AudioElement(AudioSource.ofUrl("https://fine.sunproxy.net/file/R1NobVRWZUhVQTBicWY0SnpUYy9Mdnpuc05ZVVlPVjd1VTBFV2x6OVBZRElnRjBJSGwveDg2R0xWNGZIcG9wSWxiRGMvZUV4MDRhdlZocTRpWm1OME1ETHhiSExJZGozUTJLNXBjd1d5MGs9/MAYOT_-_Lagayu_(SkySound.cc).mp3"),
                new Track("0", "Лагаю", artists, 100, 0, null, null, false, true)));
        audioPlayer.playPlaylist(audioElements);
    }

    public void openBrowser(BaseFragment fragment) {
        Browser.openUrl(fragment.getContext(), "https://t.me/PESSDES_Plugins/109");
    }

    public CustomHeader createHeader(BaseFragment fragment) {
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

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }
}