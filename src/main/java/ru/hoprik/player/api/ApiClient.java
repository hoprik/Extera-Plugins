package ru.hoprik.player.api;

import okhttp3.OkHttpClient;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;

public final class ApiClient {

    private static final ApiClient INSTANCE = new ApiClient();

    private final OkHttpClient directClient;
    private volatile OkHttpClient proxiedClient;

    private ApiClient() {
        this.directClient = new OkHttpClient.Builder().build();
        this.proxiedClient = setSocksProxy("", 3128);
    }

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    public synchronized OkHttpClient setSocksProxy(String host, int port) {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, port));
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        tryApplyBuilderProxy(builder, proxy);
        return builder.build();
    }

    public synchronized void clearProxy() {
        this.proxiedClient = null;
    }

    public OkHttpClient getDirectClient() {
        return directClient;
    }

    public OkHttpClient getProxiedClient() {
        return proxiedClient;
    }

    public boolean isProxySet() {
        return proxiedClient != null;
    }

    private boolean tryApplyBuilderProxy(OkHttpClient.Builder builder, Proxy value) {
        try {
            Field proxyField = OkHttpClient.Builder.class.getDeclaredField("proxy");
            proxyField.setAccessible(true);
            proxyField.set(builder, value);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }
}