package ru.hoprik.player.audio;

import android.graphics.Bitmap;

import java.io.File;

public class Cover {
    private final File file;
    private final String url;
    private final Bitmap bitmap;

    public Cover(File file) {
        this.file = file;
        this.url = null;
        this.bitmap = null;
    }

    public Cover(String url) {
        this.url = url;
        this.file = null;
        this.bitmap = null;
    }

    public Cover(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.file = null;
        this.url = null;
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
}
