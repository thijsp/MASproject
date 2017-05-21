package com.github.rinde.rinsim.core.model.road;

import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableList;
import geom.Line;
import geom.Rectangle;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

/**
 * Created by anthony on 5/17/17.
 */
public class NoFlyZoneRoadModel extends PlaneRoadModel {

    private final ImmutableList<Rectangle> forbiddenZones;
    private final Graph<LengthData> graph;

    NoFlyZoneRoadModel(NoFlyZoneRMB source) {
        super(source);
        this.forbiddenZones = ImmutableList.copyOf(source.getForbiddenZones());
        this.graph = computeGraph(source.getForbiddenZones());
        System.out.println("NoFlyZoneRoadModel instantiated");
        System.out.println(this.graph.getNodes());
        System.out.println(this.graph.getConnections());
    }

    public ImmutableList<Rectangle> getForbiddenZones() {
        return this.forbiddenZones;
    }

    @Override
    public Point getRandomPosition(RandomGenerator rnd) {
        Point r = super.getRandomPosition(rnd);
        while (anyContains(this.forbiddenZones, r)) {
            r = super.getRandomPosition(rnd);
        }
        return r;
    }

    private boolean isPointForbidden(Point p) {
        return anyContains(this.forbiddenZones, p);
    }

    private boolean isLineForbidden(Point start, Point end) {
        return intersectsAny(this.forbiddenZones, new Line(start, end));
    }

    @Override
    public void addObjectAt(RoadUser obj, Point pos) {
        checkArgument(!isPointForbidden(pos),
                "objects may not be added inside of a forbidden zone (%s is in such a zone)",
                pos);
        super.addObjectAt(obj, pos);
    }

    @Override
    public List<Point> getShortestPathTo(Point from, Point to) {
        System.out.println("Computing shortest path from " + from + " to " + to);
        checkArgument(
                !isPointForbidden(from),
                "from may not be in a forbidden zone (%s is in a zone)", from);
        checkArgument(
                !isPointForbidden(to),
                "to may not be in a forbidden zone (%s is in a zone)", to);

        if (!isLineForbidden(from, to)) {
            System.out.println("Direct connection from " + from + " to " + to);
            return asList(from, to);
        }

        Graph<LengthData> g = new TableGraph<>();
        for (Connection<LengthData> c : this.graph.getConnections())
            g.addConnection(c);

        g.getNodes().stream()
                .filter(p -> !isLineForbidden(from, p))
                .forEach(p -> g.addConnection(from, p, LengthData.create(Point.distance(from, p))));
        g.getNodes().stream()
                .filter(p -> !isLineForbidden(p, to))
                .forEach(p -> g.addConnection(p, to, LengthData.create(Point.distance(p, to))));

        System.out.println("Shortest path computed: " + Graphs.shortestPathEuclideanDistance(g, from, to));
        return Graphs.shortestPathEuclideanDistance(g, from, to);
    }


    // Graph computation utilities
    private static boolean intersectsAny(List<Rectangle> rects, Line line) {
        return rects.stream()
                .anyMatch(rect -> rect.intersectsLine(line));
    }

    private static boolean anyContains(List<Rectangle> rects, Point pt) {
        return rects.stream()
                .anyMatch(rect -> rect.contains(pt));
    }

    private static List<Point> unobstructedEndpoints(Rectangle zone, List<Rectangle> zones) {
        return Arrays.stream(zone.getEndpoints())
                .filter(p -> !anyContains(zones, p))
                .collect(Collectors.toList());
    }

    private static Graph<LengthData> computeGraph(List<Rectangle> zones) {
        Graph<LengthData> graph = new TableGraph<>();

        // Add connections alongside the borders of no-fly zones
        zones.stream()
                .flatMap(zone -> Arrays.stream(zone.getSides()))
                .filter(side -> !intersectsAny(zones, side))
                .forEach(side -> {
                    System.out.println("Adding side " + side);
                    LengthData dist = LengthData.create(side.distance());
                    graph.addConnection(side.start, side.end,   dist);
                    graph.addConnection(side.end,   side.start, dist);
                });

        // Add connections between endpoints of no-fly zones
        List<List<Point>> pts = zones.stream().map(zone -> unobstructedEndpoints(zone, zones)).collect(Collectors.toList());
        System.out.println(pts);

        for (int i = 1; i < pts.size(); ++i) {
            List<Point> ptsA = pts.get(i);
            for (int j = 0; j < i; ++j) {
                List<Point> ptsB = pts.get(j);
                for (Point p : ptsA) {
                    for (Point q : ptsB) {
                        Line conn = new Line(p, q);
                        if (intersectsAny(zones, conn))
                            // No direct connection between those endpoints.
                            continue;
                        LengthData dist = LengthData.create(conn.distance());
                        graph.addConnection(p, q, dist);
                        graph.addConnection(q, p, dist);
                    }
                }
            }
        }

        return graph;
    }
}
