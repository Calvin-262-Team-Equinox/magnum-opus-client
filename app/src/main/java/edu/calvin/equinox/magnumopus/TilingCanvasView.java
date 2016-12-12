package edu.calvin.equinox.magnumopus;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

/**
 * Render canvas tiles to the view port. Dispatch paint commands to the
 * relevant tiles.
 */

public class TilingCanvasView extends View implements GestureDetector.OnGestureListener
{
    /**
     * Storage of currently loaded tiles.
     */
    private TreeMap<Coordinate<Integer>, Tile> m_tiles;

    /**
     * The current type of brush being used.
     */
    private String m_brushType = "Paint Brush";

    /**
     * The current color being used.
     */
    private int m_colorType = 0;

    /**
     * Offset from the origin that the canvas is panned to.
     */
    private Coordinate<Float> m_curPos;

    /**
     * Touch gesture processing.
     */
    private GestureDetectorCompat m_detector;

    /**
     * Is canvas in navigation mode.
     */
    private boolean m_isNavigating;

    private boolean m_isErasing;

    private Random m_rand;

    public TilingCanvasView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        Cache.INSTANCE.init(getContext().getCacheDir());

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

        m_curPos = new Coordinate<>(0f, 0f);
        m_detector = new GestureDetectorCompat(getContext(), this);
        m_isNavigating = false;
        m_isErasing = false;

        m_rand = new Random(System.nanoTime());

        postDelayed(new TimedUpdater(this), 1000);
    }

    static final class TimedUpdater implements Runnable
    {
        private WeakReference<TilingCanvasView> m_view;

        private TimedUpdater(TilingCanvasView view)
        {
            m_view = new WeakReference<>(view);
        }

        @Override
        public void run()
        {
            TilingCanvasView view = m_view.get();
            if (view != null)
            {
                view.syncTiles();
                view.postDelayed(this, 5000);
            }
        }
    }

    /**
     * Check if canvas is in navigation mode.
     *
     * @return
     *  True if canvas is in navigation mode.
     */
    public boolean isNavigating()
    {
        return m_isNavigating;
    }

    /**
     * Switch between navigation and painting modes.
     *
     * @return
     *  True if canvas is now in navigation mode.
     */
    public boolean toggleNavigating()
    {
        m_isNavigating = !m_isNavigating;
        return m_isNavigating;
    }

    /**
     * Check if canvas is in erasing mode.
     *
     * @return
     *  True if canvas is in erasing mode.
     */
    public boolean isErasing()
    {
        return m_isErasing;
    }

    /**
     * Switch between erasing and painting modes.
     *
     * @return
     *  True if canvas is now in erasing mode.
     */
    public boolean toggleErasing()
    {
        m_isErasing = !m_isErasing;
        return m_isErasing;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        loadTiles();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
        {
            Coordinate<Integer> coord = entry.getKey();
            Tile tile = entry.getValue();
            canvas.drawBitmap(
                    tile.getComposite(),
                    coord.x - m_curPos.x, coord.y - m_curPos.y,
                    null
            );
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (m_isNavigating)
        {
            m_detector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                // Potentially ending a pan operation. Check if tiles
                // need to be loaded or unloaded.
                loadTiles();
            }
            return true;
        }

        // TODO: Can we optimize this by sending events only to relevant tiles?
        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
                {
                    Coordinate<Integer> coord = entry.getKey();
                    entry.getValue().onTouchMove(
                            event.getX() - coord.x + m_curPos.x,
                            event.getY() - coord.y + m_curPos.y
                    );
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
                {
                    entry.getValue().onTouchRelease();
                }
                invalidate();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    /****************************** Gestures ******************************/
    @Override
    public boolean onDown(MotionEvent motionEvent)
    {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent)
    {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent)
    {
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
    {
        m_curPos.x += distanceX;
        m_curPos.y += distanceY;
        invalidate();
        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent)
    {
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1)
    {
        return true;
    }
    /**********************************************************************/

    /**
     * Check which tiles are currently in memory; unload tiles that are no
     * longer visible, and load that have become visible.
     */
    private void loadTiles()
    {
        // Keep a one-tile buffer around the screen.
        // TODO: Does this allocate too much RAM? Should bounds be tighter?
        int curX = Math.round(m_curPos.x) - Tile.TILE_SIZE;
        int curY = Math.round(m_curPos.y) - Tile.TILE_SIZE;
        int xMax = getWidth() + curX + 2 * Tile.TILE_SIZE;
        int yMax = getHeight() + curY + 2 * Tile.TILE_SIZE;

        int canvasID = 1;

        // Remove unneeded tiles.
        Iterator<TreeMap.Entry<Coordinate<Integer>, Tile>> it = m_tiles.entrySet().iterator();
        while (it.hasNext())
        {
            TreeMap.Entry<Coordinate<Integer>, Tile> entry = it.next();
            Coordinate<Integer> coord = entry.getKey();
            if (   coord.x > xMax || coord.x + Tile.TILE_SIZE < curX
                || coord.y > yMax || coord.y + Tile.TILE_SIZE < curY )
            {
                // TODO: Save tile to disk/server.
                Tile tile = entry.getValue();
                if (tile.getVersion() > 0)
                {
                    Bitmap img = entry.getValue().getSolidComposite();
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    img.compress(
                            Bitmap.CompressFormat.JPEG,
                            5,
                            data
                    );
                    Cache.INSTANCE.put(canvasID + "-" + coord.x + "-" + coord.y, data.toByteArray());
                }

                it.remove();
            }
        }

        // Load new tiles.
        for (int x = curX; x < xMax; x += Tile.TILE_SIZE)
        {
            for (int y = curY; y < yMax; y += Tile.TILE_SIZE)
            {
                Coordinate<Integer> coord = makeCoord(x, y);
                Tile tile = m_tiles.get(coord);
                if (tile == null)
                {
                    // TODO: Load tile from disk/server.
                    try
                    {
                        tile = new Tile(
                                m_brushType,
                                Cache.INSTANCE.get(canvasID + "-" + coord.x + "-" + coord.y)
                        );
                        m_tiles.put(coord, tile);
                    }
                    catch (OutOfMemoryError e)
                    {
                        // Not enough memory currently?
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void syncTiles()
    {
        int canvasID = 1;

        for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
        {
            Tile tile = entry.getValue();
            Coordinate<Integer> coord = entry.getKey();
            tile.beginSyncEdits(
                    "http://153.106.116.72:8085/equinox/tile/" + canvasID + "/" + coord.x + "/" + coord.y,
                    "http://153.106.116.72:8085/equinox/update/tile/" + canvasID + "/" + coord.x + "/" + coord.y,
                    this
            );
            if (tile.getVersion() > 0 && m_rand.nextDouble() < 0.1)
            {
                Bitmap img = entry.getValue().getSolidComposite();
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                img.compress(
                        Bitmap.CompressFormat.JPEG,
                        5,
                        data
                );
                Cache.INSTANCE.put(canvasID + "-" + coord.x + "-" + coord.y, data.toByteArray());
            }
        }
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
     * @param brushType
     */
    protected void setBrush(String brushType)
    {
        m_brushType = brushType;
        for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
        {
            entry.getValue().setBrush(brushType);
        }
    }

    /**
     * Set the color for each canvas tile
     * @param colorType
     */
    protected void setColor(int colorType)
    {
        m_colorType = colorType;
        for (TreeMap.Entry<Coordinate<Integer>, Tile> entry : m_tiles.entrySet())
        {
            entry.getValue().setColor(colorType);
        }
    }

}
