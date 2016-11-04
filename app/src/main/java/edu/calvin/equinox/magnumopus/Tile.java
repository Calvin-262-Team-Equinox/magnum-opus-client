package edu.calvin.equinox.magnumopus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import edu.calvin.equinox.magnumopus.brushes.Brush;
import edu.calvin.equinox.magnumopus.brushes.PaintBrush;
import edu.calvin.equinox.magnumopus.brushes.PenBrush;
import edu.calvin.equinox.magnumopus.brushes.PencilBrush;

/**
 * A section of the canvas.
 */

public class Tile
{
    /**
     * Pixel dimension of each tile.
     */
    public static final int TILE_SIZE = 256;

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
     * Canvas for updating synchronized layer.
     */
    private Canvas m_syncedLayerCanvas;
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

    public Tile(String brushType, File file)
    {
        m_drawLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_drawLayerCanvas = new Canvas(m_drawLayer);

        if (file == null){
            m_syncedLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        }
        else{
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inMutable = true;
            m_syncedLayer = BitmapFactory.decodeFile(file.getAbsolutePath(), bitmapOptions);
        }
        m_syncedLayerCanvas = new Canvas(m_syncedLayer);

        m_composite = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_compositeCanvas = new Canvas(m_composite);

        m_isDirty = false;
        m_syncState = new AtomicInteger(NOT_SYNCING);

        setBrush(brushType);
    }

    /**
     * Set the brush depending on the brush type
     *
     * @param brushType
     * @return Brush m_brush
     */
    protected void setBrush(String brushType)
    {
        switch (brushType)
        {
            case "Paint Brush":
                m_brush = new PaintBrush(m_drawLayerCanvas);
                break;
            case "Pen Brush":
                m_brush = new PenBrush(m_drawLayerCanvas);
                break;
            default:
                m_brush = new PencilBrush(m_drawLayerCanvas);
                break;
        }
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

        Bitmap preview = m_brush.getPreview();
        if (preview != null)
        {
            m_compositeCanvas.drawBitmap(preview, 0, 0, null);
        }

        return m_composite;
    }

    /**
     * Saves composite of tile to a file in a hardcoded folder.
     * @param x
     *   The x coordinate of a tile.
     * @param y
     *   The y coordinate of a tile.
     */
    public void saveComposite(int x, int y){
        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        String filename = "Image-"+ x + "-" + y +".png";
        File file = new File (myDir, filename);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            m_composite.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                // Dispatch m_drawLayer to server.

                m_syncedLayerCanvas.drawBitmap(m_drawLayer, 0, 0, null);
                m_drawLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                m_isDirty = false;
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
            m_syncedLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            m_syncedLayerCanvas.drawBitmap(syncedImg, 0, 0, null);
            m_syncVersion = version;

            m_syncState.set(NOT_SYNCING);
        }
    }
}
