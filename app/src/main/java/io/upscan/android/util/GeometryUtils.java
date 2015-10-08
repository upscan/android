package io.upscan.android.util;

import android.graphics.Point;
import android.graphics.PointF;

import io.upscan.android.BuildConfig;

/**
 *
 */
public class GeometryUtils {

    private GeometryUtils() {
        // N/A
    }

    /**
     * Returns true if all points of {@code shape} are within {@code rectangle}.
     *
     * @param rectangle corner points of rectangle: top left, top right, bottom right
     */
    public static boolean isWithInRectangle(PointF[] rectangle, Point...shape) {

        int topLeftX = (int) rectangle[0].x;
        int topLeftY = (int) rectangle[0].y;
        int topRightX = (int) rectangle[1].x;
        int bottomRightY = (int) rectangle[2].y;

        for (Point p : shape) {
            if (p.x < topLeftX
                    || p.x > topRightX
                    || p.y < topLeftY
                    || p.y > bottomRightY) {
                return false;
            }
        }
        return true;

    }
}
