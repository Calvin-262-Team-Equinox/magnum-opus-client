package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;

import edu.calvin.equinox.magnumopus.Coordinate;
import edu.calvin.equinox.magnumopus.Tile;

/**
 * Sharp calligraphy pen.
 */

public class PenBrush extends Brush
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

    public PenBrush(Canvas canvas)
    {
        m_canvas = canvas;

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(4);
        m_paint.setPathEffect(new CornerPathEffect(1));
        m_paint.setAntiAlias(true);
        m_paint.setDither(true);

        m_stroke = new Path();
        m_stroke.setFillType(Path.FillType.WINDING);

        m_drawTrack = new ArrayList<>(48);
    }

    @Override
    public void setColor(int color)
    {
        m_paint.setColor(color);
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

        if (m_drawTrack.size() < 48)
        {
            return false;
        }

        // For performance, periodically apply the stroke.
        boolean isDirty = doDraw(m_canvas);
        m_drawTrack.clear();
        m_drawTrack.add(new Coordinate<>(x, y));
        return isDirty;
    }

    @Override
    public boolean onTouchRelease()
    {
        boolean isDirty = doDraw(m_canvas);
        m_drawTrack.clear();
        return isDirty;
    }

    @Override
    public void drawPreview(Canvas previewCanvas)
    {
        doDraw(previewCanvas);
    }

    private boolean doDraw(Canvas canvas)
    {
        if (m_drawTrack.isEmpty())
        {
            return false;
        }

        if (m_drawTrack.size() == 1)
        {
            // Only tapped? Draw a dot.
            Coordinate<Float> first = m_drawTrack.get(0);
            m_paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(first.x, first.y, 5, m_paint);
            canvas.drawCircle(first.x - 2, first.y - 1, 5, m_paint);
            m_paint.setStyle(Paint.Style.STROKE);
            return true;
        }

        m_stroke.reset();

        double ang = 2.1;
        int r = 4;

        RectF bounds = null;

        // Drawing the pen stroke as an area, then filling it in has the
        // problem that if the stroke crosses itself, the overlapped part
        // is cancelled out. So instead, draw several strokes next to each
        // other to create a solid area.
        for (int i = r; i <= 2 * r; i += r)
        {
            float dx = (i - r / 2f) * (float)Math.cos(ang);
            float dy = (i - r / 2f) * (float)Math.sin(ang);

            Coordinate<Float> coord = m_drawTrack.get(0);
            Coordinate<Float> prev;

            // One side of the pen stroke.
            m_stroke.moveTo(coord.x + dx, coord.y + dy);
            for (int j = 1; j < m_drawTrack.size(); ++j)
            {
                prev = coord;
                coord = m_drawTrack.get(j);
                float anchX = (prev.x + coord.x) / 2;
                float anchY = (prev.y + coord.y) / 2;
                m_stroke.quadTo(prev.x + dx, prev.y + dy, anchX + dx, anchY + dy);
            }
            m_stroke.lineTo(coord.x + dx, coord.y + dy);

            // The other side of the pen stroke.
            m_stroke.lineTo(coord.x - dx, coord.y - dy);
            for (int j = m_drawTrack.size() - 2; j >= 0; --j)
            {
                prev = coord;
                coord = m_drawTrack.get(j);
                float anchX = (prev.x + coord.x) / 2;
                float anchY = (prev.y + coord.y) / 2;
                m_stroke.quadTo(prev.x - dx, prev.y - dy, anchX - dx, anchY - dy);
            }
            m_stroke.lineTo(coord.x - dx, coord.y - dy);

            m_stroke.close();

            if (bounds == null)
            {
                bounds = new RectF();
                m_stroke.computeBounds(bounds, true);
                int buffer = Tile.TILE_SIZE / 4;
                if (!bounds.intersect(-buffer, -buffer, Tile.TILE_SIZE + buffer, Tile.TILE_SIZE + buffer))
                {
                    return false;
                }
            }

            canvas.drawPath(m_stroke, m_paint);
        }
        return true;
    }
}
