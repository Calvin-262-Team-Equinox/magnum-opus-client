package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;

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

    /**
     * Pixel dimension of the brush for painting previews.
     */
    private int m_brushSize;

    /**
     * Rendered brush for fast previews.
     */
    private Bitmap m_brushBitmap;

    /**
     * Index of 1-past the last coordinate drawn for the preview.
     */
    private int m_drawnUntil;

    public PaintBrush(Canvas canvas)
    {
        m_canvas = canvas;

        m_previewLayer = Bitmap.createBitmap(
                Tile.TILE_SIZE, Tile.TILE_SIZE,
                Bitmap.Config.ARGB_8888
        );
        m_previewLayerCanvas = new Canvas(m_previewLayer);

        m_paint = new Paint();
        m_paint.setStyle(Paint.Style.STROKE);
        m_paint.setStrokeWidth(10);
        m_paint.setStrokeCap(Paint.Cap.ROUND);
        m_paint.setPathEffect(new CornerPathEffect(8));
        m_paint.setMaskFilter(new BlurMaskFilter(3, BlurMaskFilter.Blur.NORMAL));
        m_paint.setAntiAlias(true);
        m_paint.setDither(true);

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

        setColor(Color.BLUE);

        m_drawnUntil = 0;
    }

    @Override
    public void setColor(int color)
    {
        m_paint.setColor(Color.argb(
                Math.min(100, Color.alpha(color)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        ));

        // Render the preview brush.
        m_brushSize = 64;
        m_brushBitmap = Bitmap.createBitmap(
                m_brushSize, m_brushSize,
                Bitmap.Config.ARGB_8888
        );
        Canvas brushCanvas = new Canvas(m_brushBitmap);
        int brushCenter = m_brushSize / 2;
        for (Coordinate<Float> bristle : m_bristles)
        {
            m_stroke.reset();
            m_stroke.moveTo(brushCenter + bristle.x, brushCenter + bristle.y);
            m_stroke.lineTo(brushCenter + bristle.x + 0.01f, brushCenter + bristle.y + 0.01f);
            brushCanvas.drawPath(m_stroke, m_paint);
        }
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
        return false;
    }

    @Override
    public boolean onTouchRelease()
    {
        // Quality render the stroke.
        RectF bounds = null;
        for (Coordinate<Float> bristle : m_bristles)
        {
            m_stroke.reset();

            Coordinate<Float> coord = m_drawTrack.get(0);
            Coordinate<Float> prev;

            // To smooth the stroke, take 3 consecutive points. Use the
            // midpoint of the first and second as the start of the curve. Use
            // the second point as the quadratic anchor. Use the midpoint of
            // the second and third points as the end of the curve.
            m_stroke.moveTo(coord.x + bristle.x, coord.y + bristle.y);
            for (int j = 1; j < m_drawTrack.size(); ++j)
            {
                prev = coord;
                coord = m_drawTrack.get(j);
                float anchX = (prev.x + coord.x) / 2;
                float anchY = (prev.y + coord.y) / 2;
                m_stroke.quadTo(
                        prev.x + bristle.x, prev.y + bristle.y,
                        anchX + bristle.x, anchY + bristle.y
                );
            }
            m_stroke.lineTo(coord.x + bristle.x + 0.01f, coord.y + bristle.y + 0.01f);

            if (bounds == null)
            {
                bounds = new RectF();
                m_stroke.computeBounds(bounds, true);
                int buffer = 23;
                if (!bounds.intersect(-buffer, -buffer, Tile.TILE_SIZE + buffer, Tile.TILE_SIZE + buffer))
                {
                    // BlurMaskFilter makes this draw operation very expensive, so
                    // avoid it if possible.
                    m_drawTrack.clear();
                    m_drawnUntil = 0;
                    return false;
                }
            }

            m_canvas.drawPath(m_stroke, m_paint);
        }

        // Clear the cached preview data.
        m_previewLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        m_drawTrack.clear();
        m_drawnUntil = 0;
        return true;
    }

    private boolean inBounds(Coordinate<Float> coord)
    {
        float lower = -m_brushSize / 2;
        float upper = Tile.TILE_SIZE + m_brushSize / 2;
        return coord.x > lower && coord.x < upper
                && coord.y > lower && coord.y < upper;
    }

    @Override
    public void drawPreview(Canvas previewCanvas)
    {
        if (m_drawTrack.isEmpty())
        {
            return;
        }

        Coordinate<Float> prevPrev;
        Coordinate<Float> prev;
        Coordinate<Float> coord;
        float x0, y0,
              x1, y1,
              x2, y2;
        for (; m_drawnUntil < m_drawTrack.size(); ++m_drawnUntil)
        {
            coord = m_drawTrack.get(m_drawnUntil);
            switch (m_drawnUntil)
            {
                case 0:
                    // Nothing drawn yet, put a dot at current location.
                    m_previewLayerCanvas.drawBitmap(
                            m_brushBitmap,
                            coord.x - (float)m_brushSize / 2,
                            coord.y - (float)m_brushSize / 2,
                            null
                    );
                    continue;

                case 1:
                    // Insufficient data for quadratic interpolation, draw
                    // a line instead.
                    prev = m_drawTrack.get(0);
                    x0 = prev.x;
                    y0 = prev.y;
                    x2 = (x0 + coord.x) / 2;
                    y2 = (y0 + coord.y) / 2;
                    x1 = (x0 + x2) / 2;
                    y1 = (y0 + y2) / 2;
                    break;

                default:
                    // Prepare interpolation anchors.
                    prev = m_drawTrack.get(m_drawnUntil - 1);
                    prevPrev = m_drawTrack.get(m_drawnUntil - 2);
                    x0 = (prevPrev.x + prev.x) / 2;
                    y0 = (prevPrev.y + prev.y) / 2;
                    x1 = prev.x;
                    y1 = prev.y;
                    x2 = (prev.x + coord.x) / 2;
                    y2 = (prev.y + coord.y) / 2;
            }

            coord = new Coordinate<>(x0, y0);
            prev = new Coordinate<>(x0, y0);
            // Iterate along the curve.
            for (float t = 0; t <= 1; t += 0.01f)
            {
                Coordinate.quadPoint(
                        x0, y0,
                        x1, y1,
                        x2, y2,
                        t,
                        coord
                );

                if (inBounds(coord) && Coordinate.dist(prev, coord) > 6)
                {
                    // Draw point if it is a sufficient distance from the
                    // previous drawn point.
                    m_previewLayerCanvas.drawBitmap(
                            m_brushBitmap,
                            coord.x - (float)m_brushSize / 2,
                            coord.y - (float)m_brushSize / 2,
                            null
                    );

                    prev.x = coord.x;
                    prev.y = coord.y;
                }
            }
        }

        previewCanvas.drawBitmap(m_previewLayer, 0, 0, null);

        if (m_drawTrack.size() > 1)
        {
            coord = m_drawTrack.get(m_drawTrack.size() - 1);
            prev = m_drawTrack.get(m_drawTrack.size() - 2);
            float totalDist = Coordinate.dist(prev, coord);
            if (totalDist <= 6)
            {
                return;
            }

            x0 = (prev.x + coord.x) / 2;
            y0 = (prev.y + coord.y) / 2;
            x1 = coord.x;
            y1 = coord.y;
            float scale = 128 / totalDist;
            x2 = scale * (x1 - x0) + x1;
            y2 = scale * (y1 - y0) + y1;
            x1 = (x0 + x2) / 2;
            y1 = (y0 + y2) / 2;

            coord = new Coordinate<>(x0, y0);
            prev = new Coordinate<>(x0, y0);
            // Iterate along the curve.
            for (float t = 0; t <= 1; t += 0.03f)
            {
                Coordinate.quadPoint(
                        x0, y0,
                        x1, y1,
                        x2, y2,
                        t,
                        coord
                );

                if (inBounds(coord) && Coordinate.dist(prev, coord) > 6)
                {
                    // Draw point if it is a sufficient distance from the
                    // previous drawn point.
                    previewCanvas.drawBitmap(
                            m_brushBitmap,
                            coord.x - (float)m_brushSize / 2,
                            coord.y - (float)m_brushSize / 2,
                            null
                    );

                    prev.x = coord.x;
                    prev.y = coord.y;
                }
            }
        }
    }
}
