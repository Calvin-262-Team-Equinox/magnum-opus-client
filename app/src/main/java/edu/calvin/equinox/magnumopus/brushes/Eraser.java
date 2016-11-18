package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Bitmap;
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
 * Eraser.
 */

public class Eraser extends Brush
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

    public Eraser(Canvas canvas)
    {
        m_previewLayer = Bitmap.createBitmap(
                Tile.TILE_SIZE, Tile.TILE_SIZE,
                Bitmap.Config.ARGB_8888
        );
        m_previewLayerCanvas = new Canvas(m_previewLayer);

        m_canvas = canvas;

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(100);
        m_paint.setPathEffect(new CornerPathEffect(100));
        m_paint.setStrokeCap(Paint.Cap.ROUND);
        m_paint.setAntiAlias(true);
        m_paint.setDither(true);
        m_paint.setColor(Color.argb(255, 255, 255, 255));

        m_stroke = new Path();

        m_drawTrack = new ArrayList<>(48);
    }

    @Override
    public void setColor(int color)
    {

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
        }
        m_drawTrack.add(new Coordinate<>(x, y));

        if (m_drawTrack.size() < 48)
        {
            return;
        }

        // For performance, periodically apply the stroke.
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

        if (m_drawTrack.size() == 1)
        {
            // Only tapped? Draw a dot.
            Coordinate<Float> first = m_drawTrack.get(0);
            m_paint.setStyle(Paint.Style.FILL);
            m_previewLayerCanvas.drawCircle(first.x, first.y, 50, m_paint);
            m_paint.setStyle(Paint.Style.STROKE);
            return m_previewLayer;
        }

        Coordinate<Float> coord = m_drawTrack.get(0);
        Coordinate<Float> prev;

        // Draw the eraser stroke.
        m_stroke.reset();
        m_stroke.moveTo(coord.x, coord.y);
        for (int j = 1; j < m_drawTrack.size(); ++j)
        {
            prev = coord;
            coord = m_drawTrack.get(j);
            float anchX = (prev.x + coord.x) / 2;
            float anchY = (prev.y + coord.y) / 2;
            m_stroke.quadTo(prev.x, prev.y, anchX, anchY);
        }
        m_stroke.lineTo(coord.x, coord.y);
        m_previewLayerCanvas.drawPath(m_stroke, m_paint);

        return m_previewLayer;
    }
}
