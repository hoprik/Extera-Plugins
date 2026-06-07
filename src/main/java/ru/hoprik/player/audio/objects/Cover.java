package ru.hoprik.player.audio.objects;

import android.graphics.Bitmap;
import org.telegram.messenger.ImageLocation;

import java.io.File;

public class Cover {
    private final File file;
    private final String url;
    private final Bitmap bitmap;
    private final ImageLocation location;


    public Cover(File file) {
        this.file = file;
        this.url = null;
        this.bitmap = null;
        this.location = null;
    }

    public Cover(String url) {
        this.url = url;
        this.file = null;
        this.bitmap = null;
        this.location = null;
    }

    public Cover(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.file = null;
        this.url = null;
        this.location = null;
    }

    public Cover(ImageLocation location, String artwork) {
        this.url = artwork;
        this.location = location;
        this.file = null;
        this.bitmap = null;
    }

    public File getFile() {
        return file;
    }

    public String getUrl() {
        return url;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public ImageLocation getLocation() {
        return location;
    }
}
