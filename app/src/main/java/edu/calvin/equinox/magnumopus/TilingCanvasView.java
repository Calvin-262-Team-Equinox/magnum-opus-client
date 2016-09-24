package edu.calvin.equinox.magnumopus;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.View;

/**
 * Render canvas tiles to the view port. Dispatch paint commands to the
 * relevant tiles.
 */

public class TilingCanvasView extends View
{
    private static final int TILE_SIZE = 512;

    private LongSparseArray<Tile> m_tiles;

    public TilingCanvasView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        m_tiles = new LongSparseArray<>(4);
        m_tiles.put(hashCoord(0, 0), new Tile(TILE_SIZE));
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
