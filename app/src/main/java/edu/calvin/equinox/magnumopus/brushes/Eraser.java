package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;

import edu.calvin.equinox.magnumopus.Coordinate;
import edu.calvin.equinox.magnumopus.Tile;

/**
 * Eraser.
 */

public class Eraser extends Brush
{
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
    public boolean onTouchMove(float x, float y)
    {
        if (!m_drawTrack.isEmpty())
        {
            Coordinate<Float> prev = m_drawTrack.get(m_drawTrack.size() - 1);
            if (Math.abs(x - prev.x) < 6 && Math.abs(y - prev.y) < 6)
            {
                return false;
            }
        }
        m_drawTrack.add(new Coordinate<>(x, y));

        return m_drawTrack.size() > 1 && doDraw(m_canvas, true);
    }

    @Override
    public boolean onTouchRelease()
    {
        boolean isDirty = doDraw(m_canvas, false);
        m_drawTrack.clear();
        return isDirty;
    }

    @Override
    public void drawPreview(Canvas previewCanvas)
    {
        doDraw(previewCanvas, false);
    }

    /**
     * Draw most recent segment to the canvas.
     *
     * @param canvas
     *  Canvas to draw on.
     * @param partial
     *  Should stroke be treated as in-progress.
     *
     * @return
     *  True if canvas was modified.
     */
    private boolean doDraw(Canvas canvas, boolean partial)
    {
        if (m_drawTrack.isEmpty())
        {
            return false;
        }

        int buffer = Tile.TILE_SIZE / 4;

        if (m_drawTrack.size() == 1)
        {
            // Only tapped? Draw a dot.
            Coordinate<Float> first = m_drawTrack.get(0);
            if (first.x < -buffer || first.y < -buffer
                    || first.x > Tile.TILE_SIZE + buffer || first.y > Tile.TILE_SIZE + buffer)
            {
                return false;
            }
            m_paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(first.x, first.y, 50, m_paint);
            m_paint.setStyle(Paint.Style.STROKE);
            return true;
        }

        int numPoints = m_drawTrack.size();
        Coordinate<Float> coord = m_drawTrack.get(numPoints - 1);
        Coordinate<Float> prev = m_drawTrack.get(numPoints - 2);
        float anchX = (prev.x + coord.x) / 2;
        float anchY = (prev.y + coord.y) / 2;

        m_stroke.reset();
        if (numPoints == 2)
        {
            // Straight line at beginning of stroke.
            if (!partial)
            {
                // To the end of the stroke.
                anchX = coord.x;
                anchY = coord.y;
            }
            m_stroke.moveTo(prev.x, prev.y);
            m_stroke.lineTo(anchX, anchY);
        }
        else if (partial)
        {
            // Curved line in middle of stroke.
            Coordinate<Float> prevPrev = m_drawTrack.get(numPoints - 3);
            float prevAnchX = (prevPrev.x + prev.x) / 2;
            float prevAnchY = (prevPrev.y + prev.y) / 2;

            m_stroke.moveTo(prevAnchX, prevAnchY);
            m_stroke.quadTo(prev.x, prev.y, anchX, anchY);
        }
        else
        {
            // Straight line at end of stroke.
            m_stroke.moveTo(anchX, anchY);
            m_stroke.lineTo(coord.x, coord.y);
        }

        RectF bounds = new RectF();
        m_stroke.computeBounds(bounds, true);
        if (!bounds.intersect(-buffer, -buffer, Tile.TILE_SIZE + buffer, Tile.TILE_SIZE + buffer))
        {
            return false;
        }

        canvas.drawPath(m_stroke, m_paint);

        return true;
    }
}
