package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;

import java.util.ArrayList;

import edu.calvin.equinox.magnumopus.Coordinate;
import edu.calvin.equinox.magnumopus.Tile;

/**
 * Soft bristled paint brush.
 */

public class PaintBrush extends Brush
{
    /**
     * Bitmap of active user drawings.
     */
    private Bitmap m_previewLayer;
    /**
     * Canvas for active drawing.
     */
    private Canvas m_previewLayerCanvas;

    /**
     * Canvas this brush paints on.
     */
    private Canvas m_canvas;

    /**
     * Color this brush paints with.
     */
    private Paint m_paint;

    /**
     * Cached path instance to paint along.
     */
    private Path m_stroke;

    /**
     * History of all motion coordinates from the current draw cycle.
     */
    private ArrayList<Coordinate<Float>> m_drawTrack;

    /**
     * Relative coordinates of each bristle on this brush.
     */
    private ArrayList<Coordinate<Float>> m_bristles;

    public PaintBrush(Canvas canvas)
    {
        m_previewLayer = Bitmap.createBitmap(
                Tile.TILE_SIZE, Tile.TILE_SIZE,
                Bitmap.Config.ARGB_8888
        );
        m_previewLayerCanvas = new Canvas(m_previewLayer);

        m_canvas = canvas;

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(10);
        m_paint.setStrokeCap(Paint.Cap.ROUND);
        m_paint.setPathEffect(new CornerPathEffect(20));
        m_paint.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));
        m_paint.setAntiAlias(true);
        m_paint.setDither(true);
        m_paint.setColor(Color.argb(100, 0, 0, 200));

        m_stroke = new Path();

        m_drawTrack = new ArrayList<>(48);

        // Define bristle locations.
        m_bristles = new ArrayList<>();
        m_bristles.add(new Coordinate<>(  0f,   0f));
        m_bristles.add(new Coordinate<>( 21f,   2f));
        m_bristles.add(new Coordinate<>( -1f,  19f));
        m_bristles.add(new Coordinate<>(-20f,  -2f));
        m_bristles.add(new Coordinate<>(  1f, -22f));
        m_bristles.add(new Coordinate<>( 14f,  11f));
        m_bristles.add(new Coordinate<>( -9f,  17f));
        m_bristles.add(new Coordinate<>(-13f, -10f));
        m_bristles.add(new Coordinate<>( 18f,  -7f));
        m_bristles.add(new Coordinate<>(  5f, -11f));
    }

    @Override
    public void onTouchMove(float x, float y)
    {
        if (!m_drawTrack.isEmpty())
        {
            Coordinate<Float> prev = m_drawTrack.get(m_drawTrack.size() - 1);
            if (Math.abs(x - prev.x) < 6 && Math.abs(y - prev.y) < 6)
            {
                return;
            }

            if (m_drawTrack.size() == 1)
            {
                // This shift helps reduce the restart 'blip'.
                double ang = Math.atan2(y - prev.y, x - prev.x);
                float r = 6;
                float newX = prev.x + r * (float)Math.cos(ang);
                float newY = prev.y + r * (float)Math.sin(ang);
                Coordinate<Float> shifted = new Coordinate<>(newX, newY);
                m_drawTrack.set(0, shifted);
            }
        }
        m_drawTrack.add(new Coordinate<>(x, y));

        if (m_drawTrack.size() < 48)
        {
            return;
        }

        // For performance, periodically apply the stroke. It does leave a
        // 'blip' on each restart, so do this as infrequently as possible.
        Bitmap preview = getPreview();
        m_canvas.drawBitmap(preview, 0, 0, null);
        m_drawTrack.clear();
        m_drawTrack.add(new Coordinate<>(x, y));
    }

    @Override
    public void onTouchRelease()
    {
        Bitmap preview = getPreview();
        if (preview != null)
        {
            m_canvas.drawBitmap(preview, 0, 0, null);
        }
        m_drawTrack.clear();
    }

    @Override
    public Bitmap getPreview()
    {
        if (m_drawTrack.isEmpty())
        {
            return null;
        }

        m_previewLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Coordinate<Float> first = m_drawTrack.get(0);
        for (Coordinate<Float> bristle : m_bristles)
        {
            m_stroke.reset();

            m_stroke.moveTo(first.x + bristle.x, first.y + bristle.y);
            for (Coordinate<Float> coord : m_drawTrack)
            {
                m_stroke.lineTo(coord.x + bristle.x, coord.y + bristle.y);
            }
            m_previewLayerCanvas.drawPath(m_stroke, m_paint);
        }

        return m_previewLayer;
    }
}
