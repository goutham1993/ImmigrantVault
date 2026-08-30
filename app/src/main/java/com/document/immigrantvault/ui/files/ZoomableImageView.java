package com.document.immigrantvault.ui.files;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Minimal pinch-to-zoom and pan image view. Double tap toggles between fit and 2x.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 6f;
    private static final float DOUBLE_TAP_SCALE = 2f;

    private final Matrix matrix = new Matrix();
    private final float[] values = new float[9];
    private final RectF drawableRect = new RectF();

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private boolean initialised;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        initialised = false;
        fitToView();
    }

    @Override
    public void setImageDrawable(android.graphics.drawable.Drawable drawable) {
        super.setImageDrawable(drawable);
        initialised = false;
        fitToView();
    }

    /** Centres the image and scales it down to fit, which becomes the 1x baseline. */
    private void fitToView() {
        if (initialised || getDrawable() == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float drawableWidth = getDrawable().getIntrinsicWidth();
        float drawableHeight = getDrawable().getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }
        float scale = Math.min(getWidth() / drawableWidth, getHeight() / drawableHeight);
        matrix.reset();
        matrix.postScale(scale, scale);
        matrix.postTranslate(
                (getWidth() - drawableWidth * scale) / 2f,
                (getHeight() - drawableHeight * scale) / 2f);
        setImageMatrix(matrix);
        initialised = true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        return true;
    }

    private float currentScale() {
        matrix.getValues(values);
        float baseline = baselineScale();
        return baseline == 0 ? 1f : values[Matrix.MSCALE_X] / baseline;
    }

    private float baselineScale() {
        if (getDrawable() == null || getWidth() == 0) {
            return 1f;
        }
        float drawableWidth = getDrawable().getIntrinsicWidth();
        float drawableHeight = getDrawable().getIntrinsicHeight();
        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return 1f;
        }
        return Math.min(getWidth() / drawableWidth, getHeight() / drawableHeight);
    }

    /** Keeps the image from drifting off screen, and re-centres it when smaller than the view. */
    private void constrain() {
        if (getDrawable() == null) {
            return;
        }
        drawableRect.set(0, 0,
                getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        matrix.mapRect(drawableRect);

        float deltaX = 0;
        float deltaY = 0;
        if (drawableRect.width() <= getWidth()) {
            deltaX = (getWidth() - drawableRect.width()) / 2f - drawableRect.left;
        } else if (drawableRect.left > 0) {
            deltaX = -drawableRect.left;
        } else if (drawableRect.right < getWidth()) {
            deltaX = getWidth() - drawableRect.right;
        }
        if (drawableRect.height() <= getHeight()) {
            deltaY = (getHeight() - drawableRect.height()) / 2f - drawableRect.top;
        } else if (drawableRect.top > 0) {
            deltaY = -drawableRect.top;
        } else if (drawableRect.bottom < getHeight()) {
            deltaY = getHeight() - drawableRect.bottom;
        }
        matrix.postTranslate(deltaX, deltaY);
        setImageMatrix(matrix);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scale = currentScale();
            float factor = detector.getScaleFactor();
            if (scale * factor < MIN_SCALE) {
                factor = MIN_SCALE / scale;
            } else if (scale * factor > MAX_SCALE) {
                factor = MAX_SCALE / scale;
            }
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            constrain();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            float target = currentScale() > 1.05f ? MIN_SCALE : DOUBLE_TAP_SCALE;
            float factor = target / currentScale();
            matrix.postScale(factor, factor, event.getX(), event.getY());
            constrain();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent down, MotionEvent current,
                                float distanceX, float distanceY) {
            matrix.postTranslate(-distanceX, -distanceY);
            constrain();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
    }
}
