package ru.hoprik.player.api.interfaces;

import okhttp3.*;

import java.net.URI;
import java.net.URL;

public abstract class Provider {
    private final OkHttpClient client;
    public Provider(OkHttpClient client){
        this.client = client;
    }

    public abstract String getBaseUrl();

    public String getUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    }

    public void makeReq(String relativeUrl,String method, RequestBody body, Callback callback){
        Request request = new Request.Builder()
                .url(URI.create(getBaseUrl()).resolve(relativeUrl).toString())
                .header("User-Agent", getUserAgent())
                .method(method, body)
                .build();


        client.newCall(request).enqueue(callback);
    }
}
