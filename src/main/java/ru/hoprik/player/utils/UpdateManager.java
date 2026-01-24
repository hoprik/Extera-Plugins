package ru.hoprik.player.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.TextView;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.Components.BackupImageView;
import ru.hoprik.player.MusicPlayer;

public class UpdateManager {

    private MusicInfo info;
    private TextView songNameView;
    private TextView authorName;
    private BackupImageView avatarView;
    private BackupImageView backgroundView;
    private GradientDrawable overlayColor;
    private TextView currentDurationView;
    private TextView audioTimeView;
    private SeekBar seekBar;
    private boolean isDragging;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isRunning = false;

    public UpdateManager(MusicInfo info, TextView songNameView, TextView authorName, BackupImageView avatarView, BackupImageView backgroundView, GradientDrawable overlayColor, TextView currentDurationView, TextView audioTimeView, SeekBar seekBar) {
        this.info = info;
        this.songNameView = songNameView;
        this.authorName = authorName;
        this.avatarView = avatarView;
        this.backgroundView = backgroundView;
        this.overlayColor = overlayColor;
        this.currentDurationView = currentDurationView;
        this.audioTimeView = audioTimeView;
        this.seekBar = seekBar;
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
        info.update();

        if (info.isShouldUpdate()) {
            songNameView.setText(info.getCurrentTitle());
            authorName.setText(info.getCurrentAuthor());
            ImageHelper.updateCover(info.getMessageObject(), avatarView, false);
            ImageHelper.updateCover(info.getMessageObject(), backgroundView, true);

            backgroundView.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
                @Override
                public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {
                    if (MusicPlayer.getInstance().isFeatureEnabled("enable_background_dominant", true)) {
                        if (imageReceiver.getBitmap() != null) {
                            applyColors(imageReceiver.getBitmap(), overlayColor);
                            return;
                        }
                        overlayColor.setColors(new int[]{Color.parseColor("#525252"), ImageHelper.darkenColor(Color.parseColor("#525252"), 0.6f)});
                    }
                }

                @Override
                public void didSetImageBitmap(int i, String s, Drawable drawable) {
                    if (MusicPlayer.getInstance().isFeatureEnabled("enable_background_dominant", true)) {
                        if (drawable instanceof BitmapDrawable) {
                            applyColors(((BitmapDrawable) drawable).getBitmap(), overlayColor);
                            return;
                        }
                        overlayColor.setColors(new int[]{Color.parseColor("#525252"), ImageHelper.darkenColor(Color.parseColor("#525252"), 0.6f)});
                    }
                }

                @Override
                public void onAnimationReady(ImageReceiver imageReceiver) {
                }
            });
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
}