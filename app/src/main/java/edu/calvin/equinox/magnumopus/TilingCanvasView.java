package edu.calvin.equinox.magnumopus;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Comparator;
import java.util.TreeMap;

import edu.calvin.equinox.magnumopus.brushes.PaintBrush;

/**
 * Render canvas tiles to the view port. Dispatch paint commands to the
 * relevant tiles.
 */

public class TilingCanvasView extends View
{
    /**
     * Storage of currently loaded tiles.
     */
    private TreeMap<Coordinate<Integer>, Tile> m_tiles;
    public String brushType = "Paint Brush";

    public TilingCanvasView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        m_tiles = new TreeMap<>(new Comparator<Coordinate<Integer>>()
        {
            @Override
            public int compare(Coordinate<Integer> c1, Coordinate<Integer> c2)
            {
                return c1.x < c2.x ? -1
                     : c1.x > c2.x ?  1
                     : c1.y < c2.y ? -1
                     : c1.y > c2.y ?  1
                     : 0;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // TODO: Convert this to a foreach m_tiles, and put tile loading/unloading elsewhere.
        for (int x = 0; x < width; x += Tile.TILE_SIZE)
        {
            for (int y = 0; y < height; y += Tile.TILE_SIZE)
            {
                Coordinate<Integer> coord = makeCoord(x, y);
                Tile tile = m_tiles.get(coord);
                if (tile == null)
                {
                    tile = new Tile( brushType );
                    m_tiles.put(coord, tile);
                }

                canvas.drawBitmap(
                        tile.getComposite(),
                        coord.x, coord.y, null
                );
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // TODO: Can we optimize this by sending events only to relevant tiles?
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
                {
                    Coordinate<Integer> coord = entry.getKey();
                    entry.getValue().onTouchMove(event.getX() - coord.x, event.getY() - coord.y);
                }
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
                {
                    entry.getValue().onTouchRelease();
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * Helper function to make an integer coordinate aligned to Tile.TILE_SIZE.
     *
     * @param x
     *  X parameter of the coordinate.
     * @param y
     *  Y parameter of the coordinate.
     *
     * @return
     *  The coordinate.
     */
    private static Coordinate<Integer> makeCoord(int x, int y)
    {
        return new Coordinate<>(align(x), align(y));
    }

    /**
     * Align a position to the nearest multiple of Tile.TILE_SIZE <= pos.
     *
     * @param pos
     *  Position to align.
     *
     * @return
     *  The aligned position.
     */
    private static int align(int pos)
    {
        return (pos < 0 ? pos + 1 - Tile.TILE_SIZE : pos) / Tile.TILE_SIZE * Tile.TILE_SIZE;
    }

    /**
     * Set the brush for each canvas tile
     * @param brushID
     */
    protected void setBrush(String brushID)
    {
        for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
        {
            entry.getValue().setBrush(brushID);
        }
    }

}
