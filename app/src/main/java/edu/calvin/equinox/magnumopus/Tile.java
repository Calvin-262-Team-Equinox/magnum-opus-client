package edu.calvin.equinox.magnumopus;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import edu.calvin.equinox.magnumopus.brushes.Brush;
import edu.calvin.equinox.magnumopus.brushes.Eraser;
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
    private Color m_color;

    public Tile(String brushType, byte[] cacheImg)
    {
        m_drawLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
        m_drawLayerCanvas = new Canvas(m_drawLayer);

        if (cacheImg != null)
        {
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inMutable = true;
            m_syncedLayer = BitmapFactory.decodeByteArray(cacheImg, 0, cacheImg.length, bitmapOptions);
        }
        if (m_syncedLayer == null)
        {
            m_syncedLayer = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
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
            case "Pencil Brush":
                m_brush = new PencilBrush(m_drawLayerCanvas);
                break;
            default:
                m_brush = new Eraser(m_drawLayerCanvas);
                break;


        }
    }

    /**
     * Set the color depending on the color type
     *
     * @param colorType
     * @return Color m_color
     */
   protected void setColor(int colorType)
    {
        m_brush.setColor(colorType);
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

    public Bitmap getSolidComposite()
    {
        getComposite();
        m_compositeCanvas.drawColor(Color.WHITE, PorterDuff.Mode.DST_OVER);

        return m_composite;
    }

    public int getVersion()
    {
        return m_syncVersion;
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
        if (m_brush.onTouchMove(x, y))
        {
            m_isDirty = true;
        }
    }

    /**
     * Event handler for draw end.
     */
    public void onTouchRelease()
    {
        if (m_brush.onTouchRelease())
        {
            m_isDirty = true;
        }
    }

    /**
     * Dispatch current edits to the server for synchronization.
     */
    public void beginSyncEdits(String syncURL, String updateURL, View view)
    {
        if (m_syncState.compareAndSet(NOT_SYNCING, SYNCING))
        {
            if (m_isDirty)
            {
                // Dispatch m_drawLayer to server.
                new PostTileUpdateTask(m_drawLayer, m_syncVersion, view).execute(updateURL);

                m_syncedLayerCanvas.drawBitmap(m_drawLayer, 0, 0, null);
                m_drawLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                m_isDirty = false;
            }
            else
            {
                // Dispatch m_syncVersion to server.
                new GetTileSyncTask(m_syncVersion, view).execute(syncURL);
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
    private void completeSyncEdits(Bitmap syncedImg, int version, View view)
    {
        if (m_syncState.compareAndSet(SYNCING, COMPLETING_SYNC))
        {
            if (syncedImg != null)
            {
                m_syncedLayerCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                m_syncedLayerCanvas.drawBitmap(syncedImg, 0, 0, null);
                syncedImg.recycle();
                m_syncVersion = version;

                if (view != null)
                {
                    view.invalidate();
                }
            }

            m_syncState.set(NOT_SYNCING);
        }
    }

    private class PostTileUpdateTask extends AsyncTask<String, Void, JSONObject>
    {
        Bitmap m_img;
        int m_version;
        WeakReference<View> m_view;
        String theUrl = "";

        public PostTileUpdateTask(Bitmap img, int version, View view)
        {
            m_img = Bitmap.createBitmap(img);
            m_version = version;
            m_view = new WeakReference<>(view);
        }

        @Override
        protected JSONObject doInBackground(String... params)
        {
            theUrl = params[0];

            URL apiURL;
            try
            {
                apiURL = new URL(theUrl);
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
                return null;
            }

            String postData = null;
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            m_img.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    new Base64OutputStream(data, Base64.NO_WRAP)
            );
            m_img.recycle();
            try
            {
                JSONObject obj = new JSONObject();
                obj.put("version", m_version);
                obj.put("data", data);
                postData = obj.toString();
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
            if (postData == null)
            {
                return null;
            }

            JSONObject output = null;

            try
            {
                HttpURLConnection conn = (HttpURLConnection)apiURL.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000); // 10 sec
                conn.setReadTimeout(10000);    // 10 sec
                conn.setRequestProperty("Content-Type", "application/json; charset=utf8");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8")
                );
                writer.write(postData);
                writer.flush();
                writer.close();
                os.close();

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);
                    }

                    try
                    {
                        output = new JSONObject(result.toString());
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    Log.e("PostTileUpdateTask", "HTTP error " + conn.getResponseCode());
                }
                conn.disconnect();

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return output;
        }

        @Override
        protected void onPostExecute(JSONObject tileData)
        {
            int version = 0;
            Bitmap data = null;
            if (tileData != null)
            {
                try
                {
                    version = tileData.getInt("version");
                    byte[] rawData = Base64.decode(tileData.getString("data"), Base64.DEFAULT);
                    data = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            completeSyncEdits(data, version, m_view.get());
        }
    }

    private class GetTileSyncTask extends AsyncTask<String, Void, JSONObject>
    {
        int m_version;
        WeakReference<View> m_view;
        String theUrl = "";

        public GetTileSyncTask(int version, View view)
        {
            m_version = version;
            m_view = new WeakReference<>(view);
        }

        @Override
        protected JSONObject doInBackground(String... params)
        {
            theUrl = params[0] + "/" + m_version;

            URL apiURL;
            try
            {
                apiURL = new URL(theUrl);
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
                return null;
            }

            JSONObject output = null;

            try
            {
                HttpURLConnection conn = (HttpURLConnection)apiURL.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null)
                    {
                        result.append(line);
                    }

                    try
                    {
                        output = new JSONObject(result.toString());
                    } catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    Log.e("GetTileSyncTask", "HTTP error " + conn.getResponseCode());
                }
                conn.disconnect();

            } catch (IOException e)
            {
                e.printStackTrace();
            }

            return output;
        }

        @Override
        protected void onPostExecute(JSONObject tileData)
        {
            int version = 0;
            Bitmap data = null;
            if (tileData != null)
            {
                try
                {
                    version = tileData.getInt("version");
                    if (version > m_version)
                    {
                        byte[] rawData = Base64.decode(tileData.getString("data"), Base64.DEFAULT);
                        data = BitmapFactory.decodeByteArray(rawData, 0, rawData.length);
                    }
                } catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            completeSyncEdits(data, version, m_view.get());
        }
    }
}
