
/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.upscan.android.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

import io.upscan.android.util.GeometryUtils;

/**
 * A view which renders a series of custom graphics to be overlayed on top of an associated preview
 * (i.e., the camera preview).  The creator can add graphics objects, update the objects, and remove
 * them, triggering the appropriate drawing and invalidation within the view.<p>
 * <p/>
 * Supports scaling and mirroring of the graphics relative the camera's preview properties.  The
 * idea is that detection items are expressed in terms of a preview size, but need to be scaled up
 * to the full view size, and also mirrored in the case of the front-facing camera.<p>
 * <p/>
 * Associated {@link Graphic} items should use the following methods to convert to view coordinates
 * for the graphics that are drawn:
 * <ol>
 * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of the
 * supplied value from the preview scale to the view scale.</li>
 * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the coordinate
 * from the preview's coordinate system to the view coordinate system.</li>
 * </ol>
 */
public class GraphicOverlay<T extends GraphicOverlay.Graphic> extends View {

    private static final String TAG = "UpScan";

    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<T> mGraphics = new HashSet<>();
    private T mFirstGraphic;

    private PointF mViewFinderTopLeft;
    private PointF mViewFinderTopRight;
    private PointF mViewFinderBottomLeft;
    private PointF mViewFinderBottomRight;
    private Paint mViewFinderPaint;


    public PointF[] getActiveArea() {
        return new PointF[]{
                mViewFinderTopLeft,
                mViewFinderTopRight,
                mViewFinderBottomLeft,
                mViewFinderBottomRight
        };
    }

    /**
     * Base class for a custom graphics object to be rendered within the graphic overlay.  Subclass
     * this and implement the {@link Graphic#draw(Canvas)} method to define the
     * graphics element.  Add instances to the overlay using {@link GraphicOverlay#add(Graphic)}.
     */
    public static abstract class Graphic {
        private GraphicOverlay mOverlay;

        public Graphic(GraphicOverlay overlay) {
            mOverlay = overlay;
        }

        /**
         * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
         * convert to view coordinates for the graphics that are drawn:
         * <ol>
         * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
         * the supplied value from the preview scale to the view scale.</li>
         * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
         * coordinate from the preview's coordinate system to the view coordinate system.</li>
         * </ol>
         *
         * @param canvas drawing canvas
         */
        public abstract void draw(Canvas canvas);

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        mViewFinderPaint = new Paint();
        mViewFinderPaint.setColor(Color.BLUE);
        mViewFinderPaint.setAntiAlias(true);
        mViewFinderPaint.setStrokeWidth(4f);
    }

    /**
     * Removes all graphics from the overlay.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
            mFirstGraphic = null;
        }
        postInvalidate();
    }

    /**
     * Adds a graphic to the overlay.
     */
    public void add(T graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
            if (mFirstGraphic == null) {
                mFirstGraphic = graphic;
            }
        }
        postInvalidate();
    }

    /**
     * Removes a graphic from the overlay.
     */
    public void remove(T graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
            if (mFirstGraphic != null && mFirstGraphic.equals(graphic)) {
                mFirstGraphic = null;
            }
        }
        postInvalidate();
    }

    /**
     * Returns the first (oldest) graphic added.  This is used
     * to get the barcode that was detected first.
     *
     * @return graphic containing the barcode, or null if no barcodes are detected.
     */
    public T getFirstGraphic() {
        synchronized (mLock) {
            return mFirstGraphic;
        }
    }

    /**
     * Sets the camera attributes for size and facing direction, which informs how to transform
     * image coordinates later.
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }

        drawViewFinder(canvas);


    }


    public boolean isInsideViewFinder(Point[] cornerPoints, Graphic graphic) {

        PointF[] rectangle = new PointF[]{
                mViewFinderTopLeft,
                mViewFinderTopRight,
                mViewFinderBottomRight,
                mViewFinderBottomLeft
        };

        for (Point point : cornerPoints) {
            if (!GeometryUtils.isWithInRectangle(rectangle,
                    new Point((int) graphic.translateX(point.x),
                            (int) graphic.translateY(point.y)))) {
                return false;
            }
        }
        return true;
    }


    private void drawViewFinder(Canvas canvas) {

        if (mViewFinderTopLeft == null) {
            final int width = canvas.getWidth();
            final int margin = 100;

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);

            float aspectRatio = ((float) metrics.widthPixels) / ((float) metrics.heightPixels);
            float viewFinderHeight = (width - (2 * margin)) * aspectRatio;

            mViewFinderTopLeft = new PointF(margin, margin);
            mViewFinderTopRight = new PointF(width - margin, margin);
            mViewFinderBottomLeft = new PointF(margin, margin + viewFinderHeight);
            mViewFinderBottomRight = new PointF(width - margin, margin + viewFinderHeight);
        }

        canvas.drawLine(mViewFinderTopLeft.x, mViewFinderTopLeft.y,
                mViewFinderTopRight.x, mViewFinderTopRight.y, mViewFinderPaint);

        canvas.drawLine(mViewFinderTopLeft.x, mViewFinderTopLeft.y,
                mViewFinderBottomLeft.x, mViewFinderBottomLeft.y, mViewFinderPaint);

        canvas.drawLine(mViewFinderBottomLeft.x, mViewFinderBottomLeft.y,
                mViewFinderBottomRight.x, mViewFinderBottomRight.y, mViewFinderPaint);

        canvas.drawLine(mViewFinderBottomRight.x, mViewFinderBottomRight.y,
                mViewFinderTopRight.x, mViewFinderTopRight.y, mViewFinderPaint);

    }
}