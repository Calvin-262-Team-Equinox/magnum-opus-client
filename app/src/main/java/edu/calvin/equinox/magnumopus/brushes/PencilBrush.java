package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;

import edu.calvin.equinox.magnumopus.Coordinate;

/**
 * Sketching pencil.
 *
 * This brush sketches in shading to the insides of corners while drawing. The
 * faster the movement, the large (but sparser) the shading.
 */

public class PencilBrush extends Brush
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

    public PencilBrush(Canvas canvas)
    {
        m_canvas = canvas;

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(3);
        m_paint.setAntiAlias(true);
        m_paint.setDither(true);
        m_paint.setColor(Color.argb(100, 0, 0, 0));

        m_stroke = new Path();

        m_drawTrack = new ArrayList<>(64);
    }

    /**
     * Event handler for draw movement.
     *
     * @param x
     *  The x coordinate of the current position.
     * @param y
     *  The y coordinate of the current position.
     */
    @Override
    public void onTouchMove(float x, float y)
    {
        Coordinate<Float> cur = new Coordinate<>(x, y);
        if (m_drawTrack.size() > 0)
        {
            Coordinate<Float> prev = m_drawTrack.get(m_drawTrack.size() - 1);
            float dist = Coordinate.dist(prev, cur);
            if (dist < 4)
            {
                // Very little movement.
                return;
            }

            if (dist > 64)
            {
                // Too much movement.
                onTouchMove((x + prev.x) / 2, (y + prev.y) / 2);
            }
        }

        // Record current position.
        m_drawTrack.add(cur);

        int len = m_drawTrack.size();
        // Draw lines from the last n recorded positions to the current position.
        for (int i = Math.max(0, len - 6); i < len - 1; i += 2)
        {
            Coordinate<Float> prev = m_drawTrack.get(i);
            // Compute an anchor as the average of the last n points so that
            // the shading lines are pulled closer to the actual draw path.
            float anchX = prev.x;
            float anchY = prev.y;
            for (int j = i + 1; j < len; ++j)
            {
                cur = m_drawTrack.get(j);
                anchX += cur.x;
                anchY += cur.y;
            }
            anchX /= len - i;
            anchY /= len - i;

            m_stroke.reset();
            m_stroke.moveTo(prev.x, prev.y);
            m_stroke.quadTo(anchX, anchY, x, y);
            m_canvas.drawPath(m_stroke, m_paint);
        }
    }

    /**
     * Event handler for draw end.
     */
    @Override
    public void onTouchRelease()
    {
        if (m_drawTrack.size() == 1)
        {
            // The canvas was just tapped, so draw a dot.
            Coordinate<Float> first = m_drawTrack.get(0);
            m_paint.setStyle(Paint.Style.FILL);
            m_canvas.drawCircle(first.x, first.y, 5, m_paint);
            m_canvas.drawCircle(first.x - 2, first.y - 1, 5, m_paint);
            m_paint.setStyle(Paint.Style.STROKE);
        }

        m_drawTrack.clear();
    }
}
