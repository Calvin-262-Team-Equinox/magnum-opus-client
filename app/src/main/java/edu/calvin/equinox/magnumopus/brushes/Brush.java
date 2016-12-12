package edu.calvin.equinox.magnumopus.brushes;

import android.graphics.Canvas;

/**
 * Basic brush interface.
 */

public abstract class Brush
{
    /**
     * Event handler for draw movement.
     *
     * @param x
     *  The x coordinate of the current position.
     * @param y
     *  The y coordinate of the current position.
     */
    public abstract boolean onTouchMove(float x, float y);

    /**
     * Event handler for draw end.
     */
    public abstract boolean onTouchRelease();

    /**
     * Render a preview of what this brush is drawing, but has not committed yet.
     */
    public void drawPreview(Canvas previewCanvas)
    {
    }

    public abstract void setColor(int color);

}
