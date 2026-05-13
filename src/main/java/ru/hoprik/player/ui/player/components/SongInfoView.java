package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.AudioPlayer;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Track;

public class SongInfoView extends LinearLayout implements NotificationCenter.NotificationCenterDelegate {

    private SimpleTextView songView;
    private LinearLayout authorView;

    public SongInfoView(Context context, Track track) {
        super(context);
        register();
        setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
        setLayoutParams(layoutParams);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, AndroidUtilities.dp(5), 0, 0);

        this.songView = new SimpleTextView(context);
        this.songView.setTextColor(Theme.getColor(Theme.key_player_actionBarTitle));
        this.songView.setTextSize(20);
        this.songView.setTypeface(AndroidUtilities.bold());
        this.songView.setText(track.getName());
        this.songView.setLayoutParams(textParams);
        this.songView.setScrollNonFitText(true);

        if (!track.getArtists().isEmpty()) {
            this.authorView = new LinearLayout(context);
            this.authorView.setOrientation(LinearLayout.HORIZONTAL);
            this.authorView.setLayoutParams(textParams);
            for (Artist artist : track.getArtists()) {
                this.authorView.addView(new ArtistTextView(context, artist));
            }
        }

        addView(this.songView);
        addView(this.authorView);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregister();
    }

    public void register() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.messagePlayingDidStart);
    }

    private void unregister() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidReset);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.messagePlayingDidStart);
    }

    private void update() {
        AudioPlayer player = MusicPlayer.getInstance().getAudioPlayer();
        if (player == null) return;
        AudioElement audioElement = player.getAudioElement();
        if (audioElement == null) return;
        Track track = audioElement.getTrack();

        songView.setText(track.getName());

        if (authorView == null) {
            authorView = new LinearLayout(getContext());
            authorView.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, AndroidUtilities.dp(5), 0, 0);
            authorView.setLayoutParams(params);
            addView(authorView);
        } else {
            authorView.removeAllViews();
        }

        if (track.getArtists() != null && !track.getArtists().isEmpty()) {
            authorView.setVisibility(VISIBLE);
            for (Artist artist : track.getArtists()) {
                authorView.addView(new ArtistTextView(getContext(), artist));
            }
        } else {
            TextView unknownArtist = new TextView(getContext());
            unknownArtist.setText("Unknown artist");
            unknownArtist.setTextColor(Theme.getColor(Theme.key_player_actionBarSubtitle));
            unknownArtist.setTextSize(16);
            authorView.addView(unknownArtist);
            authorView.setVisibility(VISIBLE);
        }
    }

    @Override
    public void didReceivedNotification(int i, int i1, Object... objects) {
        if (i == NotificationCenter.messagePlayingDidStart || i == NotificationCenter.messagePlayingDidReset || i == NotificationCenter.messagePlayingPlayStateChanged){
            update();
        }
    }
}

