package ru.hoprik.player.ui.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaController;
import org.telegram.ui.ActionBar.Theme;

import ru.hoprik.player.utils.MusicInfo;
import ru.hoprik.player.utils.UpdateManager;

public class PlayerSeekBarView extends LinearLayout {

    private TextView currentTimeView;
    private TextView remainingTimeView;
    private SeekBar seekBar;
    private UpdateManager manager;

    public PlayerSeekBarView(Context context, MusicInfo info) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        LinearLayout.LayoutParams progressContainerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressContainerParams.setMargins(
                AndroidUtilities.dp(0),
                AndroidUtilities.dp(10),
                AndroidUtilities.dp(0),
                AndroidUtilities.dp(0)
        );
        setLayoutParams(progressContainerParams);

        seekBar = new SeekBar(context);
        seekBar.setMax(100);

        GradientDrawable thumbDrawable = new GradientDrawable();
        thumbDrawable.setShape(GradientDrawable.OVAL);
        thumbDrawable.setSize(
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(16)
        );
        thumbDrawable.setColor(Theme.getColor(Theme.key_player_progress));
        seekBar.setThumb(thumbDrawable);

        seekBar.getProgressDrawable().setColorFilter(
                new PorterDuffColorFilter(Theme.getColor(Theme.key_player_progress), PorterDuff.Mode.SRC_IN)
        );

        LinearLayout.LayoutParams seek_bar_params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setThumbOffset(AndroidUtilities.dp(0));
        seek_bar_params.setMargins(0, 0, 0, AndroidUtilities.dp(8));
        seekBar.setLayoutParams(seek_bar_params);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b && manager != null && !manager.isDragging()) {
                    currentTimeView.setText(AndroidUtilities.formatLongDuration((int) ((i / (float) seekBar.getMax()) * info.getAudioProgress())));
                    info.update(MediaController.getInstance().getPlayingMessageObject());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (manager != null) manager.setDragging(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MediaController.getInstance().seekToProgress(info.getMessageObject(), (float) seekBar.getProgress() / seekBar.getMax());
                if (manager != null) manager.setDragging(false);
            }
        });

        if (info.getAudioProgress() > 0) {
            int currentProgress = (int) (((float) info.getCurrentDuration() / info.getAudioProgress()) * 100);
            seekBar.setProgress(currentProgress);
        } else {
            seekBar.setProgress(0);
        }
        addView(seekBar);

        // Timeline
        LinearLayout timeContainer = new LinearLayout(context);
        timeContainer.setOrientation(LinearLayout.HORIZONTAL);
        timeContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        this.currentTimeView = new TextView(context);
        this.currentTimeView.setTextColor(Theme.getColor(Theme.key_player_time));
        this.currentTimeView.setTextSize(12);
        this.currentTimeView.setText(info.getTimeString());

        LinearLayout.LayoutParams currentTimeParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        this.currentTimeView.setLayoutParams(currentTimeParams);

        this.remainingTimeView = new TextView(context);
        this.remainingTimeView.setTextColor(Theme.getColor(Theme.key_player_time));
        this.remainingTimeView.setTextSize(12);
        this.remainingTimeView.setText(info.getAudioProgressString());
        this.remainingTimeView.setGravity(Gravity.RIGHT);

        LinearLayout.LayoutParams remainingTimeParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1
        );
        this.remainingTimeView.setLayoutParams(remainingTimeParams);

        timeContainer.addView(currentTimeView);
        timeContainer.addView(remainingTimeView);

        addView(timeContainer);
    }

    public TextView getCurrentTimeView() {
        return currentTimeView;
    }

    public TextView getRemainingTimeView() {
        return remainingTimeView;
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    public void setManager(UpdateManager manager) {
        this.manager = manager;
    }
}

