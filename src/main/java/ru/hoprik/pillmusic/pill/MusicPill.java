package ru.hoprik.pillmusic.pill;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.exteragram.messenger.pillstack.core.PillStackConfig;
import com.exteragram.messenger.pillstack.ui.PillStackPreferencesActivity;
import com.exteragram.messenger.pillstack.ui.pills.BasePill;

import com.exteragram.messenger.plugins.PluginsController;
import com.exteragram.messenger.plugins.ui.PluginSettingsActivity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.LaunchActivity;

import ru.hoprik.pillmusic.PillMusic;
import ru.hoprik.pillmusic.controller.MusicInfo;
import ru.hoprik.pillmusic.controller.StreamResponse;

import java.util.stream.Collectors;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static org.telegram.messenger.AndroidUtilities.dp;

@SuppressLint("ViewConstructor")
public class MusicPill extends BasePill implements NotificationCenter.NotificationCenterDelegate {

    private final ImageView iconView;
    private final SimpleTextView textView;
    private final LinearLayout layout;

    private StreamResponse cachedResponse;
    private boolean requestInFlight = false;
    private String authToken;

    private static final int PILL_WIDTH_DP = 130;
    private static final int PILL_HEIGHT_DP = 28;
    private static final int TEXT_WIDTH_DP = 100;

    public MusicPill(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
        refreshUsernameIfChanged();

        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setMinimumWidth(dp(48));
        layout.setPadding(dp(6), 0, dp(10), 0);
        addView(layout, LayoutHelper.createFrame(PILL_WIDTH_DP, PILL_HEIGHT_DP, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT)));

        iconView = new ImageView(context);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setImageResource(R.drawable.files_music);
        layout.addView(iconView, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 0, 0, 4, 0));

        textView = new SimpleTextView(context);
        textView.setTextSize(dp(4));
        textView.setScrollNonFitText(true);
        textView.setTypeface(AndroidUtilities.bold());
        textView.setLayoutParams(new ViewGroup.LayoutParams(dp(TEXT_WIDTH_DP), WRAP_CONTENT));
        layout.addView(textView, LayoutHelper.createLinear(TEXT_WIDTH_DP, WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        setLoadingTargetView(layout);
        updateColors();
        ScaleStateListAnimator.apply(layout);
    }

    @Override
    public long getRefreshInterval() {
        return getCooldown();
    }

    @Override
    public int getPillId() {
        return 71369790;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (PillStackConfig.checkAndClearPendingUpdate(getPillId()) ||
                cachedResponse == null ||
                isRefreshDue()) {
            onUpdateData(true);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    public void onUpdateData(boolean force) {
        if (requestInFlight) return;

        refreshUsernameIfChanged();
        if (TextUtils.isEmpty(authToken)) {
            setErrorState(false);
            return;
        }

        if (!force && cachedResponse != null && !isRefreshDue()) {
            setData(cachedResponse, false);
            return;
        }

        requestInFlight = true;
        if (force) animateSizeChange();
        startLoading();

        new Thread(() -> {
            try {
                StreamResponse response = new MusicInfo().fetchCurrentStream(authToken);
                AndroidUtilities.runOnUIThread(() -> {
                    requestInFlight = false;
                    if (response != null && response.getItem() == null) {
                        setErrorState(PillMusic.getInstance().getString("turn_on_player"), true);
                    } else if (response != null && response.getItem() != null && response.getItem().isPlaying()) {
                        cachedResponse = response;
                        setData(response, true);
                        markDataUpdated();
                    } else {
                        setErrorState(true);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                AndroidUtilities.runOnUIThread(() -> {
                    requestInFlight = false;
                    setErrorState(true);
                });
            }
        }).start();
    }

    private long getCooldown(){
        String cooldown = PluginsController.getInstance().getPluginSettingString("pill_stats_fm", "cooldown", "10");
        int cd = Integer.parseInt(cooldown);
        if (cooldown == null || cd <= 0) {
            return cd*1000;
        }
        return 10*1000;
    }

    private void refreshUsernameIfChanged() {
        String latestUsername = PluginsController.getInstance().getPluginSettingString("pill_stats_fm", "username", "");
        if (latestUsername == null) {
            latestUsername = "";
        }

        if (!TextUtils.equals(authToken, latestUsername)) {
            authToken = latestUsername;
            cachedResponse = null;
        }
    }

    private void setData(StreamResponse response, boolean animated) {
        stopLoading();
        if (animated) animateSizeChange();
        iconView.setVisibility(VISIBLE);
        textView.setText(formatTrackText(response), animated);
        textView.setVisibility(VISIBLE);
        iconView.setImageResource(R.drawable.files_music);
    }

    private String formatTrackText(StreamResponse response) {
        if (response == null || response.getItem() == null || response.getItem().getTrack() == null) {
            return "Unknown";
        }

        StreamResponse.Track track = response.getItem().getTrack();
        String artists = "";
        if (track.getArtists() != null) {
            artists = track.getArtists().stream()
                    .map(StreamResponse.Artist::getName)
                    .filter(name -> !TextUtils.isEmpty(name))
                    .collect(Collectors.joining(", "));
        }

        String trackName = track.getName();
        if (TextUtils.isEmpty(artists)) {
            return TextUtils.isEmpty(trackName) ? "Unknown" : trackName;
        }
        if (TextUtils.isEmpty(trackName)) {
            return artists;
        }
        return artists + " - " + trackName;
    }

    private void setErrorState(boolean animated) {
        setErrorState(LocaleController.getString(R.string.Retry), animated);
    }

    private void setErrorState(String errorText, boolean animated) {
        stopLoading();
        if (animated) animateSizeChange();
        iconView.setImageResource(R.drawable.msg_retry);
        iconView.setVisibility(VISIBLE);
        textView.setText(errorText, animated);
        textView.setVisibility(VISIBLE);
    }

    @Override
    public void onPillClicked() {
        onUpdateData(true);
    }

    @Override
    public boolean onPillLongClicked() {
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();

        if (fragment != null) {
            ItemOptions options = ItemOptions.makeOptions(fragment, this)
                    .setDrawScrim(true)
                    .setDimAlpha(1);

            if (PluginsController.getInstance().plugins.get("pill_stats_fm") != null) {
                options.add(R.drawable.msg_plugins, PillMusic.getInstance().getString("plugin_settings"), () ->
                        fragment.presentFragment(new PluginSettingsActivity(PluginsController.getInstance().plugins.get("pill_stats_fm"))));
            }

            options
                    .add(R.drawable.msg_settings, LocaleController.getString(R.string.Settings), () -> fragment.presentFragment(new PillStackPreferencesActivity()))
                    .show();
            return true;
        }
        return false;
    }

    @Override
    public void updateColors() {
        int textColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText, 0.75f);
        int backgroundColor = Theme.isCurrentThemeDark() ? getThemedColor(Theme.key_windowBackgroundWhite) : Theme.multAlpha(textColor, 0.09f);
        layout.setBackground(Theme.createSimpleSelectorRoundRectDrawable(dp(14), backgroundColor, Theme.multAlpha(textColor, 0.1f)));
        textView.setTextColor(textColor);
        updateLoadingColors();
    }

    @Override
    protected void updateLoadingColors() {
        if (loadingDrawable != null) {
            int textColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
            loadingDrawable.setColors(Theme.multAlpha(textColor, 0.05f), Theme.multAlpha(textColor, 0.15f));
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        if (loading) pressed = false;
        super.setPressed(pressed);
        layout.setPressed(pressed);
    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {

    }
}