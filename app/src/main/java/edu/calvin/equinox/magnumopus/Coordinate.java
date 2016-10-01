package edu.calvin.equinox.magnumopus;

/**
 * Helper class to hold (x, y) coordinates.
 */

public class Coordinate<E>
{
    public final E x;
    public final E y;

    public Coordinate(E x, E y)
    {
        this.x = x;
        this.y = y;
    }
}
