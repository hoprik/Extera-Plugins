package ru.hoprik.player.ui.components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BackupImageView;

public class CoverLayout extends BackupImageView {
    MessageObject messageObject;
    private GestureDetector gestureDetector;
    private Runnable onNext;
    private Runnable onPrevious;
    private final boolean isMiniCover;

    public void setOnSwipeListeners(Runnable onNext, Runnable onPrevious) {
        this.onNext = onNext;
        this.onPrevious = onPrevious;
    }

    public CoverLayout(Context context) {
        super(context);
        setClickable(true);
        setupGestures();
        this.isMiniCover = false;
    }

    public CoverLayout(Context context, boolean isMiniCover) {
        super(context);
        this.isMiniCover = isMiniCover;
        setClickable(true);
        setupGestures();
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

    private void setup(){

    }

    private ImageLocation getArtworkThubImageLocation(MessageObject object) {
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

    private boolean isPlaceHolder(MessageObject object){
        if (object == null) return true;
        if (getArtworkThubImageLocation(object) != null) return true;
        if (object.getArtworkUrl(false) != null) {
            return TextUtils.isEmpty(object.getArtworkUrl(false));
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (imageReceiver.getThumbKey() == null && imageReceiver.getImageKey() != null ){
            Drawable drawable = ImageLoader.getInstance().getFromMemCache(imageReceiver.getImageKey());
            if (drawable == null){
                Bitmap albumArtPlaceholder = Bitmap.createBitmap(AndroidUtilities.dp(102), AndroidUtilities.dp(102), Bitmap.Config.ARGB_8888);
                Drawable placeholder = getContext().getDrawable(R.drawable.nocover);
                placeholder.setBounds(0, 0, albumArtPlaceholder.getWidth(), albumArtPlaceholder.getHeight());
                placeholder.draw(new Canvas(albumArtPlaceholder));
                setImage(null, null, albumArtPlaceholder, this);
            }
        }
        if (imageReceiver.getImageKey() != null){
            Log.d("KEY", imageReceiver.getImageKey());
        }
        if (imageReceiver.getThumbKey() != null){
            Log.d("KEYF", imageReceiver.getThumbKey());
        }
        Log.d("SIZE", imageReceiver.getSize() + "");

        super.onDraw(canvas);
    }

    public void update(MessageObject object){
        this.messageObject = object;
    }
}
