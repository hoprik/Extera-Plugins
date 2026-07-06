package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.*;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.MotionBackgroundDrawable;

import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.AudioPlayer;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.objects.Cover;
import ru.hoprik.player.audio.objects.Track;
import ru.hoprik.player.helpers.ImageHelper;

import java.io.File;

public class PlayerBackgroundView extends FrameLayout{

    private CoverLayout backgroundImage;
    private MotionBackgroundDrawable motionBackgroundDrawable;

    public PlayerBackgroundView(Context context) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        this.backgroundImage = new CoverLayout(context, false, true);
        this.backgroundImage.setAspectFit(false);
        this.backgroundImage.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        motionBackgroundDrawable = new MotionBackgroundDrawable();

        this.backgroundImage.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
            @Override
            public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {
                if (imageReceiver.getBitmap() != null) {
                    int dominant = ImageHelper.getDominantColor(imageReceiver.getBitmap());
                    int darkened = ImageHelper.darkenColor(dominant, 0.4f);
                    motionBackgroundDrawable.setColors(dominant, darkened, dominant, darkened);
                    return;
                }
                motionBackgroundDrawable.setColors(0xff333333, 0xff000000, 0xff333333, 0xff000000);
            }

            @Override
            public void didSetImageBitmap(int i, String s, Drawable drawable) {
                if (drawable instanceof BitmapDrawable) {
                    int dominant = ImageHelper.getDominantColor((BitmapDrawable) drawable);
                    int darkened = ImageHelper.darkenColor(dominant, 0.4f);
                    motionBackgroundDrawable.setColors(dominant, darkened, dominant, darkened);
                    return;
                }
                motionBackgroundDrawable.setColors(Color.parseColor("#525252"), Color.rgb(0, 0, 0), Color.parseColor("#525252"), Color.rgb(0, 0, 0));
            }

            @Override
            public void onAnimationReady(ImageReceiver imageReceiver) {
            }
        });

        FrameLayout overlayFrame = new FrameLayout(context);
        overlayFrame.setBackground(motionBackgroundDrawable);
        overlayFrame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        addView(backgroundImage);
        addView(overlayFrame);
    }

    public void setupBackgroundCover(Cover cover) {
        if (cover == null) return;
        this.backgroundImage.setupCover(cover);
    }
}

