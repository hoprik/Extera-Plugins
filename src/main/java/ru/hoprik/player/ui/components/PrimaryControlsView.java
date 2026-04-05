package ru.hoprik.player.ui.components;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.PlayPauseDrawable;
import org.telegram.ui.Components.RLottieImageView;

public class PrimaryControlsView extends LinearLayout {

    private ImageView repeatButtonView;
    private ImageView shuffleButtonView;
    private PlayPauseDrawable playPauseDrawable;

    public interface ControlsListener {
        void onPlaylistUpdated();
    }

    private ControlsListener listener;

    public PrimaryControlsView(Context context, boolean enableShuffle, ControlsListener listener) {
        super(context);
        this.listener = listener;

        setOrientation(LinearLayout.HORIZONTAL);
        setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams controllerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        controllerParams.topMargin = AndroidUtilities.dp(15);
        setLayoutParams(controllerParams);

        int iconColor = Theme.getColor(Theme.key_player_button);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(50),
                AndroidUtilities.dp(50)
        );
        buttonParams.setMargins(
                AndroidUtilities.dp(3), 0,
                AndroidUtilities.dp(3), 0
        );

        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                AndroidUtilities.dp(64),
                AndroidUtilities.dp(64)
        );
        playParams.setMargins(
                AndroidUtilities.dp(5), 0,
                AndroidUtilities.dp(5), 0
        );

        if (enableShuffle) {
            this.repeatButtonView = new ImageView(context);
            this.repeatButtonView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.repeatButtonView.setImageResource(R.drawable.player_new_repeatall);
            this.repeatButtonView.setColorFilter(iconColor);
            this.repeatButtonView.setLayoutParams(buttonParams);
            this.repeatButtonView.setAlpha(0.5f);
            this.repeatButtonView.setOnClickListener(view -> {
                if (SharedConfig.repeatMode == 2) {
                    SharedConfig.setRepeatMode(0);
                } else {
                    SharedConfig.setRepeatMode(2);
                }
                updateRepeatButtons();
            });
            addView(repeatButtonView);
        }

        RLottieImageView prevButton = new RLottieImageView(context);
        prevButton.setScaleType(ImageView.ScaleType.CENTER);
        prevButton.setAnimation(R.raw.player_prev, 40, 40);
        prevButton.setLayerColor("Triangle 3.**", iconColor);
        prevButton.setLayerColor("Triangle 4.**", iconColor);
        prevButton.setLayerColor("Rectangle 4.**", iconColor);
        prevButton.setLayoutParams(buttonParams);

        this.playPauseDrawable = new PlayPauseDrawable(50);
        this.playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);

        prevButton.setOnClickListener(view -> {
            RLottieImageView v = (RLottieImageView) view;
            MediaController.getInstance().playPreviousMessage();
            v.setProgress(0.0f);
            v.playAnimation();
            if (playPauseDrawable != null) {
                playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
            }
            if (this.listener != null) this.listener.onPlaylistUpdated();
        });

        addView(prevButton);

        ImageView playButton = new ImageView(context);
        playButton.setScaleType(ImageView.ScaleType.CENTER);
        playButton.setImageDrawable(playPauseDrawable);
        playButton.setColorFilter(iconColor);
        playButton.setLayoutParams(playParams);

        playButton.setOnClickListener(view -> {
            if (MediaController.getInstance().isDownloadingCurrentMessage()) {
                return;
            }
            if (MediaController.getInstance().isMessagePaused()) {
                MediaController.getInstance().playMessage(MediaController.getInstance().getPlayingMessageObject());
            } else {
                MediaController.getInstance().pauseMessage(MediaController.getInstance().getPlayingMessageObject());
            }
            playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
        });

        addView(playButton);

        RLottieImageView nextButton = new RLottieImageView(context);
        nextButton.setScaleType(ImageView.ScaleType.CENTER);
        nextButton.setAnimation(R.raw.player_prev, 40, 40);
        nextButton.setLayerColor("Triangle 3.**", iconColor);
        nextButton.setLayerColor("Triangle 4.**", iconColor);
        nextButton.setLayerColor("Rectangle 4.**", iconColor);
        nextButton.setRotation(180);
        nextButton.setLayoutParams(buttonParams);

        nextButton.setOnClickListener(view -> {
            RLottieImageView v = (RLottieImageView) view;
            MediaController.getInstance().playNextMessage();
            v.setProgress(0.0f);
            v.playAnimation();
            if (playPauseDrawable != null) {
                playPauseDrawable.setPause(!MediaController.getInstance().isMessagePaused(), false);
            }
            if (this.listener != null) this.listener.onPlaylistUpdated();
        });
        addView(nextButton);

        if (enableShuffle) {
            this.shuffleButtonView = new ImageView(context);
            this.shuffleButtonView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            this.shuffleButtonView.setImageResource(R.drawable.player_new_shuffle);
            this.shuffleButtonView.setColorFilter(iconColor);
            this.shuffleButtonView.setLayoutParams(buttonParams);
            this.shuffleButtonView.setAlpha(0.5f);

            this.shuffleButtonView.setOnClickListener(view -> {
                if (SharedConfig.shuffleMusic) {
                    MediaController.getInstance().setPlaybackOrderType(0);
                } else {
                    MediaController.getInstance().setPlaybackOrderType(2);
                }
                updateRepeatButtons();
            });
            addView(this.shuffleButtonView);
        }

        updateRepeatButtons();
    }

    public void updateRepeatButtons() {
        if (this.repeatButtonView != null) {
            if (SharedConfig.repeatMode == 2) {
                this.repeatButtonView.setAlpha(1.0f);
            } else {
                this.repeatButtonView.setAlpha(0.5f);
            }
        }

        if (this.shuffleButtonView != null) {
            if (SharedConfig.shuffleMusic) {
                this.shuffleButtonView.setAlpha(1.0f);
            } else {
                this.shuffleButtonView.setAlpha(0.5f);
            }
        }
    }

    public PlayPauseDrawable getPlayPauseDrawable() {
        return playPauseDrawable;
    }

    public void setPlayPauseState(boolean pause, boolean animated) {
        if (playPauseDrawable != null) {
            playPauseDrawable.setPause(pause, animated);
        }
    }
}


