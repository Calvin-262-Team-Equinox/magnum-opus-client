package edu.calvin.equinox.magnumopus;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A section of the canvas.
 */

public class Tile
{
    Bitmap m_composite;
    Canvas m_compositeCanvas;

    public Tile(int size)
    {
        m_composite = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        m_compositeCanvas = new Canvas(m_composite);

        Paint brush = new Paint();
        brush.setStyle(Paint.Style.FILL);
        brush.setColor(Color.BLUE);
        m_compositeCanvas.drawCircle(150, 150, 100, brush);
    }

    public Bitmap getComposite()
    {
        return m_composite;
    }
}
