package ru.hoprik.player.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import org.telegram.messenger.*;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BackupImageView;

public class ImageHelper {

    public static int getDominantColor(BitmapDrawable bitmapDrawable){
        return getDominantColor(bitmapDrawable.getBitmap());
    }

    public static int getDominantColor(Bitmap bitmap) {
        if (bitmap != null){
            return AndroidUtilities.getDominantColor(bitmap);
        }
        return Color.parseColor("#525252");
    }

    public static int darkenColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }
}
