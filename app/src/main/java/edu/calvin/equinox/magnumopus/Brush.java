package edu.calvin.equinox.magnumopus;

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
    public abstract void onTouchMove(float x, float y);

    /**
     * Event handler for draw end.
     */
    public abstract void onTouchRelease();
}
