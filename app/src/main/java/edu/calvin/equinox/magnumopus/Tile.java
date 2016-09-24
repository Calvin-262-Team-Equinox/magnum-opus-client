package edu.calvin.equinox.magnumopus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;

/**
 * A section of the canvas.
 */

public class Tile
{
    /**
     * Pixel dimension of each tile.
     */
    public static final int TILE_SIZE = 512;

    /**
     * Bitmap of active user drawings.
     */
    private Bitmap m_drawLayer;
    /**
     * Canvas for active drawing.
     */
    private Canvas m_drawLayerCanvas;

    /**
     * Verified synchronized tile bitmap from the server.
     */
    private Bitmap m_syncedLayer;
    /**
     * Version code of m_syncedLayer.
     */
    private int m_syncVersion;

    /**
     * Composited bitmap for display.
     */
    private Bitmap m_composite;
    /**
     * Canvas for drawing the composite.
     */
    private Canvas m_compositeCanvas;

    public Tile()
    {
        m_drawLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_drawLayerCanvas = new Canvas(m_drawLayer);

        m_syncedLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);

        m_composite = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_compositeCanvas = new Canvas(m_composite);

        // Sample painting.
        Paint brush = new Paint();
        brush.setStyle(Paint.Style.FILL);
        brush.setColor(Color.BLUE);
        m_drawLayerCanvas.drawCircle(150, 150, 100, brush);
    }

    /**
     * Composite all layers of this tile for display.
     *
     * @return
     *  The composite bitmap.
     */
    public Bitmap getComposite()
    {
        m_compositeCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        m_compositeCanvas.drawBitmap(m_syncedLayer, 0, 0, null);
        m_compositeCanvas.drawBitmap(m_drawLayer, 0, 0, null);

        return m_composite;
    }
}
