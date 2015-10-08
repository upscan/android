package io.upscan.android.util;

import android.graphics.Point;
import android.graphics.PointF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import io.upscan.android.BuildConfig;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class GeometryUtilsTest {

    @Test
    public void testIsWithInRectangle() throws Exception {

        PointF[] rectangle = new PointF[]{
                new PointF(100f, 100f), // top left
                new PointF(500, 100f), // top right
                new PointF(500f, 500f), // bottom right
                new PointF(100f, 500f) // bottom left
        };

        assertTrue(GeometryUtils.isWithInRectangle(rectangle,
                new Point(100, 100),
                new Point(150, 500),
                new Point(500, 500),
                new Point(400, 100)));

        assertFalse(GeometryUtils.isWithInRectangle(rectangle,
                new Point(100, 100),
                new Point(500, 501),
                new Point(250, 250)));

    }
}