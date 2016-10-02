package edu.calvin.equinox.magnumopus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;

import java.util.concurrent.atomic.AtomicInteger;

import edu.calvin.equinox.magnumopus.brushes.Brush;
import edu.calvin.equinox.magnumopus.brushes.PencilBrush;

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
     * Bitmap of edits that are in progress of synchronizing with the server.
     */
    private Bitmap m_syncingLayer;

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

    /**
     * True if there are active edits on m_drawLayer.
     */
    private boolean m_isDirty;

    // Valid states for m_syncState.
    private static final int NOT_SYNCING     = 0;
    private static final int SYNCING         = 1;
    private static final int COMPLETING_SYNC = 2;

    /**
     * State lock to avoid multiple syncs occurring at the same time.
     */
    private AtomicInteger m_syncState;

    /**
     * Current brush this tile is painting with.
     */
    private Brush m_brush;

    public Tile()
    {
        m_drawLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_drawLayerCanvas = new Canvas(m_drawLayer);

        m_syncingLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);

        m_syncedLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);

        m_composite = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_compositeCanvas = new Canvas(m_composite);

        m_isDirty = false;
        m_syncState = new AtomicInteger(NOT_SYNCING);

        m_brush = new PencilBrush(m_drawLayerCanvas);
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
        m_compositeCanvas.drawBitmap(m_syncingLayer, 0, 0, null);
        m_compositeCanvas.drawBitmap(m_drawLayer, 0, 0, null);

        Bitmap preview = m_brush.getPreview();
        if (preview != null)
        {
            m_compositeCanvas.drawBitmap(preview, 0, 0, null);
        }

        return m_composite;
    }

    /**
     * Event handler for draw movement.
     *
     * @param x
     *  The x coordinate of the current position.
     * @param y
     *  The y coordinate of the current position.
     */
    public void onTouchMove(float x, float y)
    {
        m_brush.onTouchMove(x, y);
        // TODO: Only set dirty bit if this tile actually changed.
        m_isDirty = true;
    }

    /**
     * Event handler for draw end.
     */
    public void onTouchRelease()
    {
        m_brush.onTouchRelease();
    }

    /**
     * Dispatch current edits to the server for synchronization.
     */
    private void beginSyncEdits()
    {
        if (m_syncState.compareAndSet(NOT_SYNCING, SYNCING))
        {
            if (m_isDirty)
            {
                m_syncingLayer = Bitmap.createBitmap(m_drawLayer);
                m_drawLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                m_isDirty = false;

                // Dispatch m_syncingLayer to server.
            }
            else
            {
                // Dispatch m_syncVersion to server.
            }
        }
    }

    /**
     * Apply a synchronized image from the server to this tile.
     *
     * @param syncedImg
     *  The updated image.
     * @param version
     *  Version code of this image.
     */
    private void completeSyncEdits(Bitmap syncedImg, int version)
    {
        if (m_syncState.compareAndSet(SYNCING, COMPLETING_SYNC))
        {
            m_syncedLayer = syncedImg;
            m_syncVersion = version;
            m_syncingLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);

            m_syncState.set(NOT_SYNCING);
        }
    }
}
