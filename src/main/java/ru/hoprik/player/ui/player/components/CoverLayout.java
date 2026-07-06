package ru.hoprik.player.ui.player.components;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.*;
import org.telegram.messenger.audioinfo.AudioInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.BackupImageView;
import ru.hoprik.player.MusicPlayer;
import ru.hoprik.player.audio.AudioPlayer;
import ru.hoprik.player.audio.holder.AudioElement;
import ru.hoprik.player.audio.objects.Artist;
import ru.hoprik.player.audio.objects.Cover;
import ru.hoprik.player.audio.objects.Track;

import java.io.File;

public class CoverLayout extends BackupImageView {
    Track messageObject;
    private GestureDetector gestureDetector;
    private Runnable onNext;
    private Runnable onPrevious;

    private final boolean isMiniCover;

    private boolean isPlaceholder;
    private Bitmap placeholder;



    public void setOnSwipeListeners(Runnable onNext, Runnable onPrevious) {
        this.onNext = onNext;
        this.onPrevious = onPrevious;
    }

    public CoverLayout(Context context) {
        this(context, false, false);
    }

    public CoverLayout(Context context, boolean isMiniCover, boolean isBackground) {
        super(context);
        this.isMiniCover = isMiniCover;
        setupPlaceholder();
        setClickable(true);
        setupGestures();
        if (!isBackground) {
            super.getImageReceiver().setDelegate(getDelegate());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    private void setupGestures() {
        if (isMiniCover) return;
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                int width = getWidth();
                if (e.getX() > width / 2f) {
                    MediaController.getInstance().playNextMessage();
                    if (onNext != null) onNext.run();
                } else {
                    MediaController.getInstance().playPreviousMessage();
                    if (onPrevious != null) onPrevious.run();
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY) && Math.abs(velocityX) > 500) {
                    if (e1 != null && e2 != null) {
                        if (e1.getX() - e2.getX() > 100) {
                            MediaController.getInstance().playNextMessage();
                            if (onNext != null) onNext.run();
                            return true;
                        } else if (e2.getX() - e1.getX() > 100) {
                            MediaController.getInstance().playPreviousMessage();
                            if (onPrevious != null) onPrevious.run();
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    private float startX;
    private float startY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float dx = Math.abs(event.getX() - startX);
            float dy = Math.abs(event.getY() - startY);
            if (dx > dy && dx > 10) { // Horizontal swipe threshold
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        }

        if (gestureDetector != null && gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    public static ImageLocation getArtworkThumbImageLocation(MessageObject messageObject) {
        final TLRPC.Document document = messageObject.getDocument();
        TLRPC.PhotoSize thumb = document != null ? FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 360) : null;
        if (!(thumb instanceof TLRPC.TL_photoSize) && !(thumb instanceof TLRPC.TL_photoSizeProgressive)) {
            thumb = null;
        }
        if (thumb != null) {
            return ImageLocation.getForDocument(thumb, document);
        }
        final String smallArtworkUrl = messageObject.getArtworkUrl(true);
        if (smallArtworkUrl != null) {
            return ImageLocation.getForPath(smallArtworkUrl);
        }
        return null;
    }

    private boolean isPlaceHolder(MessageObject object) {
        if (object == null) return true;
        if (getArtworkThumbImageLocation(object) != null) return true;
        if (object.getArtworkUrl(false) != null) {
            return TextUtils.isEmpty(object.getArtworkUrl(false));
        }
        return false;
    }

    private void setupPlaceholder() {
        Bitmap albumArtPlaceholder = Bitmap.createBitmap(AndroidUtilities.dp(102), AndroidUtilities.dp(102), Bitmap.Config.ARGB_8888);
        Drawable placeholder = getContext().getDrawable(R.drawable.nocover);
        placeholder.setBounds(0, 0, albumArtPlaceholder.getWidth(), albumArtPlaceholder.getHeight());
        placeholder.draw(new Canvas(albumArtPlaceholder));
        this.placeholder = albumArtPlaceholder;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isPlaceholder) {
            int[] radii = getRoundRadius(); // получаем массив [tl, tr, bl, br]
            Path clipPath = new Path();
            RectF rect = new RectF(0, 0, getWidth(), getHeight());
            float[] radiiArray = new float[8];
            for (int i = 0; i < 4; i++) {
                radiiArray[i*2] = radii[i];
                radiiArray[i*2+1] = radii[i];
            }
            clipPath.addRoundRect(rect, radiiArray, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipPath);

            Matrix matrix = new Matrix();
            matrix.setScale((float) getWidth() / placeholder.getWidth(),
                    (float) getHeight() / placeholder.getHeight());
            canvas.drawBitmap(placeholder, matrix, new Paint());
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }

    public void update(Track object) {
        this.messageObject = object;
    }

    public void setupCover(Cover cover) {
        // Сразу показываем placeholder до загрузки нового изображения
        isPlaceholder = true;
        invalidate();

        Log.d("CoverSetup", "=== setupCover called ===");
        Log.d("CoverSetup", "Initial Cover state: " + (cover != null ? cover.toString() : "null"));

        if (cover == null) {
            return; // placeholder уже показан
        }

        if (cover.getLocation() == null && cover.getUrl() == null && cover.getBitmap() == null && cover.getFile() == null) {
            Log.w("CoverSetup", "Branch: ALL NULL. Placeholder remains.");
            return;
        }

        if (cover.getBitmap() != null) {
            Log.d("CoverSetup", "Branch: BITMAP. Loading from Bitmap object.");
            setImageBitmap(cover.getBitmap());
            return;
        }

        if (cover.getFile() != null) {
            File coverFile = cover.getFile();
            String path = coverFile.getAbsolutePath();
            Log.d("CoverSetup", "Branch: FILE. Loading from local path: " + path);
            ImageLocation imageLocation = ImageLocation.getForPath(path);
            setImage(imageLocation, null, null, null, null, 0, 1, this);
            return;
        }

        if (cover.getUrl() != null && cover.getLocation() == null) {
            Log.d("CoverSetup", "Branch: URL ONLY. Loading from URL: " + cover.getUrl());
            setImage(ImageLocation.getForPath(cover.getUrl()), null, null, null, null, 0, 1, this);
            return;
        }

        if (cover.getLocation() != null) {
            Log.d("CoverSetup", "Branch: LOCATION. Loading from ImageLocation object.");
            setImage(cover.getLocation(), null, null, null, null, 0, 1, this);
            return;
        }

        Log.w("CoverSetup", "Branch: FALLBACK ELSE. Placeholder remains.");
    }

    private ImageReceiver.ImageReceiverDelegate getDelegate() {
        return new ImageReceiver.ImageReceiverDelegate() {
            @Override
            public void didSetImage(ImageReceiver imageReceiver, boolean b, boolean b1, boolean b2) {
                Drawable drawable = imageReceiver.getDrawable();
                if (drawable != null) {
                    isPlaceholder = false;
                    Log.d("CoverSetup", drawable.getBounds().toString());
                    Log.d("CoverSetup", drawable.getAlpha()+"");
                    Log.d("CoverSetup", drawable.getConstantState().toString());
                }

            }

            @Override
            public void didSetImageBitmap(int i, String s, Drawable drawable) {
                isPlaceholder = false;
            }

            @Override
            public void onAnimationReady(ImageReceiver imageReceiver) {

            }
        };
    }

    private void update() {
        AudioPlayer player = MusicPlayer.getInstance().getAudioPlayer();
        if (player == null) return;
        AudioElement audioElement = player.getAudioElement();
        if (audioElement == null) return;
        Track track = audioElement.getTrack();

        if (track == null || track.getCover() == null) {
            this.isPlaceholder = true;
            return;
        }

        Cover cover = track.getCover();
        setupCover(cover);
    }
}
