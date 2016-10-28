package edu.calvin.equinox.magnumopus;

/**
 * Helper class to hold (x, y) coordinates.
 */

public class Coordinate<E>
{
    public E x;
    public E y;

    public Coordinate(E x, E y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Compute the distance between two points.
     *
     * @param p0
     *  The start point.
     * @param p1
     *  The end point.
     * @return
     *  Distance between this and other.
     */
    public static float dist(Coordinate<Float> p0, Coordinate<Float> p1)
    {
        double dx = p1.x - p0.x;
        double dy = p1.y - p0.y;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Interpolate a point on a quadratic curve.
     *
     * @param x0, y0
     *  Start point.
     * @param x1, y1
     *  Anchor.
     * @param x2, y2
     *  End point.
     * @param t
     *  Percent along the line (0.0 <= t <= 1.0).
     * @param point
     *  Coordinate to fill with the interpolated point into.
     */
    public static void quadPoint(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2,
            float t,
            Coordinate<Float> point)
    {
        float coef0 = (1 - t) * (1 - t);
        float coef1 = 2 * (1 - t) * t;
        float coef2 = t * t;
        point.x = coef0 * x0 + coef1 * x1 + coef2 * x2;
        point.y = coef0 * y0 + coef1 * y1 + coef2 * y2;
    }
}
