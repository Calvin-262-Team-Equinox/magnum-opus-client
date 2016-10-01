package edu.calvin.equinox.magnumopus;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.Comparator;
import java.util.TreeMap;

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
        m_tiles.put(makeCoord(0, 0), new Tile());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawBitmap(
                m_tiles.get(makeCoord(0, 0)).getComposite(),
                0, 0, null
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                m_tiles.get(makeCoord(0, 0)).onTouchMove(event.getX(), event.getY());
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                m_tiles.get(makeCoord(0, 0)).onTouchRelease();
                invalidate();
                break;
        }
        return true;
    }

    /**
     * Helper function to make an integer coordinate.
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
        return new Coordinate<>(x, y);
    }
}
