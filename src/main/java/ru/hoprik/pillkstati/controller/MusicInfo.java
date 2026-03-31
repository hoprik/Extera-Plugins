package ru.hoprik.pillkstati.controller;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;

public class MusicInfo {

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public StreamResponse fetchCurrentStream(String name) throws Exception {
        String URL = String.format("https://api.stats.fm/api/v1/users/%s/streams/current", name);
        Request request = new Request.Builder()
                .url(URL)
                .header("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unexpected code " + response);
            }
            String json = response.body().string();
            return (StreamResponse) gson.fromJson(json, StreamResponse.class);
        }
    }

}