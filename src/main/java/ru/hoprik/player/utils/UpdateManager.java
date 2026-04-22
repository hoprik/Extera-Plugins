package ru.hoprik.player.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.BackupImageView;
import ru.hoprik.player.MusicPlayer;

public class UpdateManager {

    private MusicInfo info;
    private SimpleTextView songNameView;
    private TextView authorName;
    private BackupImageView avatarView;
    private BackupImageView backgroundView;
    private GradientDrawable overlayColor;
    private TextView currentDurationView;
    private TextView audioTimeView;
    private SeekBar seekBar;
    private boolean isDragging;
    private Runnable startTrackRefreshCallback;

    private static final int START_TRACK_REFRESH_SECONDS = 10;
    private long trackedMessageId = Long.MIN_VALUE;
    private int lastRefreshedSecond = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isRunning = false;

    public UpdateManager(MusicInfo info, SimpleTextView songNameView, TextView authorName, BackupImageView avatarView, BackupImageView backgroundView, GradientDrawable overlayColor, TextView currentDurationView, TextView audioTimeView, SeekBar seekBar, Runnable startTrackRefreshCallback) {
        this.info = info;
        this.songNameView = songNameView;
        this.authorName = authorName;
        this.avatarView = avatarView;
        this.backgroundView = backgroundView;
        this.overlayColor = overlayColor;
        this.currentDurationView = currentDurationView;
        this.audioTimeView = audioTimeView;
        this.seekBar = seekBar;
        this.startTrackRefreshCallback = startTrackRefreshCallback;
    }

    public void startUpdater() {
        if (isRunning) return;

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
                if (isRunning) {
                    handler.postDelayed(this, 1000);
                }
            }
        };

        isRunning = true;
        handler.post(updateRunnable);
    }

    public void stopUpdater() {
        isRunning = false;
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    public void updateUI() {
        MessageObject playingMessage = MediaController.getInstance().getPlayingMessageObject();
        info.update(playingMessage);

        long currentMessageId = resolveMessageId(playingMessage);
        if (trackedMessageId != currentMessageId) {
            trackedMessageId = currentMessageId;
            lastRefreshedSecond = -1;
        }

        if (info.isShouldUpdate()) {
            songNameView.setText(info.getCurrentTitle());
            authorName.setText(info.getCurrentAuthor());
            ImageHelper.updateCover(info.getMessageObject(), avatarView, false);
            ImageHelper.updateCover(info.getMessageObject(), backgroundView, true);
            if (startTrackRefreshCallback != null) {
                startTrackRefreshCallback.run();
            }
        }

        if (playingMessage != null) {
            int progressSec = Math.max(0, playingMessage.audioProgressSec);
            if (progressSec < START_TRACK_REFRESH_SECONDS && lastRefreshedSecond != progressSec) {
                lastRefreshedSecond = progressSec;
                ImageHelper.updateCover(playingMessage, avatarView, false);
                ImageHelper.updateCover(playingMessage, backgroundView, true);
                if (startTrackRefreshCallback != null) {
                    startTrackRefreshCallback.run();
                }
            }
        }

        if (!isDragging) {
            currentDurationView.setText(info.getTimeString());
            audioTimeView.setText(info.getAudioProgressString());

            long current = info.getCurrentDuration();
            long total = info.getAudioProgress();

            if (total > 0) {
                int progress = (int) (((float) current / total) * 100);
                seekBar.setProgress(progress);
            } else {
                seekBar.setProgress(0);
            }
        }
    }

    private void applyColors(Bitmap bitmap, GradientDrawable overlayColor) {
        if (overlayColor == null) return;
        int dominant = ImageHelper.getDominantColor(bitmap);
        overlayColor.setColors(new int[]{dominant, ImageHelper.darkenColor(dominant, 0.6f)});
    }

    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }

    public boolean isDragging() {
        return isDragging;
    }

    private long resolveMessageId(MessageObject messageObject) {
        if (messageObject == null) {
            return Long.MIN_VALUE;
        }
        if (messageObject.getDocument() != null) {
            return messageObject.getDocument().id;
        }
        return messageObject.getId();
    }
}