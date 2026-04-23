package ru.hoprik.pillmusic.controller;

import android.util.Log;
import com.exteragram.messenger.plugins.PluginsController;
import com.google.gson.Gson;
import j$.util.G;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SaveManager {
    public static final int SERVICE_STATS_FM = 1;
    public static final int SERVICE_YANDEX = 2;
    public static final int SERVICE_VK = 4;
    public static final int SERVICE_TELEGRAM = 5;
    public static final int SERVICE_LASTFM = 6;
    public static final Gson gson = new Gson();
    private static SaveManager instance;
    private SaveData saveData;

    public static void init() {
        instance = new SaveManager();
        instance.load();
    }

    public static SaveManager getInstance() {
        return instance;
    }

    public SaveData getDefault(){
        SaveData saveData1 = new SaveData();
        saveData1.tokens = new HashMap<>();
        saveData1.enabled = new ArrayList<>();
        saveData1.disabled = new ArrayList<>();
        saveData1.disabled.add(SERVICE_STATS_FM);
        saveData1.disabled.add(SERVICE_YANDEX);
        saveData1.disabled.add(SERVICE_VK);
        saveData1.disabled.add(SERVICE_TELEGRAM);
        saveData1.disabled.add(SERVICE_LASTFM);
        return saveData1;
    }

    public void load(){
        String setting = PluginsController.getInstance().getPluginSettingString("pill_stats_fm","accounts", new Gson().toJson(getDefault()));
        try {
            saveData = (SaveData) gson.fromJson(setting, SaveData.class);
        } catch (Exception e) {
            reset();
        }
    }

    public void save(SaveData data) {
        saveData = data;
        String json = new Gson().toJson(saveData);
        Log.d("LOGS", json);
        PluginsController.getInstance().setPluginSetting("pill_stats_fm", "accounts", json);
    }

    public void reset(){
        save(getDefault());
    }

    public SaveData getSaveData() {
        return saveData;
    }

    public static class SaveData {
        Map<Integer, String> tokens;
        ArrayList<Integer> enabled;
        ArrayList<Integer> disabled;

        public Map<Integer, String> getTokens() {
            return tokens;
        }

        public ArrayList<Integer> getEnabled() {
            return enabled;
        }

        public ArrayList<Integer> getDisabled() {
            return disabled;
        }

        public void setTokens(Map<Integer, String> tokens) {
            this.tokens = tokens;
        }

        public void setEnabled(ArrayList<Integer> enabled) {
            this.enabled = enabled;
        }

        public void setDisabled(ArrayList<Integer> disabled) {
            this.disabled = disabled;
        }
    }
}
