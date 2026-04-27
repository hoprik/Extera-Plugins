package ru.hoprik.player.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
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

    public static ImageLocation getArtworkThubImageLocation(MessageObject object) {
        try {
            TLRPC.Document document = object.getDocument();
            if (document != null && document.thumbs != null && !document.thumbs.isEmpty()) {
                TLRPC.PhotoSize thumb = document.thumbs.get(document.thumbs.size() - 1);

                return ImageLocation.getForDocument(thumb, document);
            }
        } catch (Exception e) {

        }
        return null;
    }

    public static void updateCover(MessageObject object, BackupImageView imageView, boolean isBackground) {
        try {
            AudioInfo audioInfo = MediaController.getInstance().getAudioInfo();

            if (audioInfo != null && audioInfo.getCover() != null) {
                imageView.setImageBitmap(audioInfo.getCover());
            }

            String artworkUrl = object.getArtworkUrl(false);
            ImageLocation location = getArtworkThubImageLocation(object);

            if (isBackground){
                imageView.setAspectFit(false);
            }

            if (!TextUtils.isEmpty(artworkUrl)) {
                imageView.setImage(
                        ImageLocation.getForPath(artworkUrl),
                        null,
                        location,
                        null,
                        null,
                        0,
                        1,
                        object
                );
            }else if (location != null){
                imageView.setImage(
                        null,
                        null,
                        location,
                        null,
                        null,
                        0,
                        1,
                        object
                );
            }else{
                Bitmap albumArtPlaceholder = Bitmap.createBitmap(AndroidUtilities.dp(102), AndroidUtilities.dp(102), Bitmap.Config.ARGB_8888);
                Drawable placeholder = imageView.getContext().getDrawable(R.drawable.nocover);
                placeholder.setBounds(0, 0, albumArtPlaceholder.getWidth(), albumArtPlaceholder.getHeight());
                placeholder.draw(new Canvas(albumArtPlaceholder));
                imageView.setImage(null, null, albumArtPlaceholder, imageView);
                if (isBackground){
                    imageView.setAspectFit(true);
                }
            }

            imageView.invalidate();

        } catch (Exception e) {

        }
    }
}
