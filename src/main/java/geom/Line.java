package geom;

import com.github.rinde.rinsim.geom.Point;

/**
 * Created by anthony on 5/20/17.
 */
public class Line {
    public final Point start, end;
    public Line(Point start, Point end) {
        this.start = start;
        this.end   = end;
    }
    public Line(double x1, double y1, double x2, double y2) {
        this(new Point(x1, y1), new Point(x2, y2));
    }

    public double distance() {
        return Point.distance(this.start, this.end);
    }

    @Override
    public String toString() {
        return "[Line from " + start + " to " + end + "]";
    }

    static boolean intersects(Line a, Line b) {
        Point dp = Point.diff(a.end, a.start);
        Point dq = Point.diff(b.end, b.start);

        double discr = cross(dp, dq);
        if (discr == 0.0D)
            // Lines are collinear (overlapping or parallel)
            return false;

        Point d = Point.diff(b.start, a.start);
        double tp = cross(d, dq) / discr;
        double tq = cross(d, dp) / discr;

        return 0.0 < tp && tp < 1.0 && 0.0 < tq && tq < 1.0;
    }

    private static double cross(Point a, Point b) {
        return a.x * b.y - a.y * b.x;
    }

    public static void main(String[] args) {
        Line a = new Line(new Point(0, 0), new Point(1, 1));
        Line b = new Line(new Point(1, 0), new Point(0, 1));
        System.out.println(a);
        System.out.println(b);
        System.out.println(Line.intersects(a, b));
    }
}
