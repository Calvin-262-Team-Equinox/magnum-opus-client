package edu.calvin.equinox.magnumopus;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * Render canvas tiles to the view port. Dispatch paint commands to the
 * relevant tiles.
 */

public class TilingCanvasView extends View
{
    /**
     * Storage of currently loaded tiles.
     */
    private LongSparseArray<Tile> m_tiles;

    public TilingCanvasView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_tiles = new LongSparseArray<>(4);
        m_tiles.put(hashCoord(0, 0), new Tile());
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawBitmap(
                m_tiles.get(hashCoord(0, 0)).getComposite(),
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
                m_tiles.get(hashCoord(0, 0)).onTouchMove(event.getX(), event.getY());
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                m_tiles.get(hashCoord(0, 0)).onTouchRelease();
                invalidate();
                break;
        }
        return true;
    }

    /**
     * Transform an (x, y) coordinate into a single hash value.
     *
     * @param x
     *  X parameter of the coordinate.
     * @param y
     *  Y parameter of the coordinate.
     *
     * @return
     *  Hash identifier of the coordinate.
     */
    private long hashCoord(int x, int y)
    {
        long hash = 2166136261L;

        hash = hash ^ x;
        hash *= 16777619;

        hash = hash ^ y;
        hash *= 16777619;

        return hash;
    }
}
