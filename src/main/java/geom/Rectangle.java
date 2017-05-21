package geom;

import com.github.rinde.rinsim.geom.Point;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by anthony on 5/20/17.
 */
public class Rectangle {
    private final double MARGIN = 0.001D;
    public final double xMin, xMax, yMin, yMax;

    public Rectangle(double xMin, double xMax, double yMin, double yMax) {
        checkArgument(xMin < xMax && yMin < yMax,
                "Invalid rectangle boundaries (%s - %s, %s - %s)", xMin, xMax, yMin, yMax);
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public Rectangle(Point min, Point max) {
        this(min.x, max.x, min.y, max.y);
    }

    public double width() {
        return xMax - xMin;
    }

    public double height() {
        return yMax - yMin;
    }

    public Point[] getEndpoints() {
        return new Point[] {
                new Point(xMin, yMin),
                new Point(xMin, yMax),
                new Point(xMax, yMax),
                new Point(xMax, yMin),
        };
    }

    public Line[] getSides() {
        Point[] endpoints = this.getEndpoints();
        Line[] sides = new Line[4];
        for (int i = 0; i < 4; ++i)
            sides[i] = new Line(endpoints[i], endpoints[(i+1)&3]);
        return sides;
    }

    private Line diag1() {
        return new Line(xMin + MARGIN, yMin + MARGIN, xMax - MARGIN, yMax - MARGIN);
    }
    private Line diag2() {
        return new Line(xMin + MARGIN, yMax - MARGIN, xMax - MARGIN, yMin + MARGIN);
    }

    public boolean contains(Point p) {
        return xMin < p.x && p.x < xMax && yMin < p.y && p.y < yMax;
    }

    public boolean intersectsLine(Line l) {
        return  this.contains(l.start) ||
                this.contains(l.end) ||
                Line.intersects(l, this.diag1()) ||
                Line.intersects(l, this.diag2());
        // Arrays.stream(this.getSides()).anyMatch(side -> Line.intersects(side, l));
    }

    @Override
    public String toString() {
        return "[Rectangle " + xMin + "," + xMax + " " + yMin + "," + yMax + "]";
    }
}
