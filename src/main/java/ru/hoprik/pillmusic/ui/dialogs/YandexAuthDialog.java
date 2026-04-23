package ru.hoprik.pillmusic.ui.dialogs;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.HashMap;
import java.util.Map;

public class YandexAuthDialog {
    private final Context context;
    private final DialogResponse response;
    private AlertDialog alertDialog; // Выносим в поле класса

    public YandexAuthDialog(Context context, DialogResponse dialogResponse) {
        this.context = context;
        this.response = dialogResponse;
    }

    public void show() {
        // Используем Telegram-стиль диалога
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Авторизация в Яндекс музыке");
        builder.setView(createView());
        builder.setNegativeButton(LocaleController.getString("Cancel", org.telegram.messenger.R.string.Cancel), (d, which) -> {
            if (response != null) response.onGetToken("");
        });

        alertDialog = builder.create();
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            alertDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        alertDialog.show();
    }

    private FrameLayout createView() {
        FrameLayout container = new FrameLayout(context);
        CustomWebView webView = new CustomWebView(context);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                case android.view.MotionEvent.ACTION_UP:
                    if (!v.hasFocus()) {
                        v.requestFocus();
                    }
                    break;
            }
            return false;
        });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.153 Mobile Safari/537.36");
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String currentUrl = request.getUrl().toString();
                if (currentUrl.contains("access_token=")) {
                    String token = extractToken(currentUrl);
                    if (token != null) {
                        if (response != null) response.onGetToken(token);
                        if (alertDialog != null) alertDialog.dismiss();
                    }
                    return true;
                }
                return false;
            }
        });
        webView.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT);
        }, 100);

        // Очистка куки для возможности смены аккаунта
        CookieManager.getInstance().removeAllCookies(null);

        container.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                AndroidUtilities.dp(450)
        ));

        webView.loadUrl("https://oauth.yandex.ru/authorize?response_type=token&client_id=23cabbbdc6cd418abb4b39c32c41195d", extraHeaders);

        return container;
    }

    private String extractToken(String url) {
        try {
            // Более надежный сплит через regex или поиск индекса
            if (url.contains("access_token=")) {
                return url.split("access_token=")[1].split("&")[0];
            }
        } catch (Exception ignored) {}
        return null;
    }
}