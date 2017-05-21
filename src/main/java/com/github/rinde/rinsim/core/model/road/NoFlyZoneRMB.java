package com.github.rinde.rinsim .core.model.road;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.PlaneRMB;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders.AbstractRMB;
import geom.*;
import geom.Rectangle;

import javax.measure.quantity.Length;
import javax.measure.quantity.Velocity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by anthony on 5/17/17.
 */
public final class NoFlyZoneRMB extends PlaneRMB /*AbstractRMB<NoFlyZoneRoadModel, NoFlyZoneRMB>*/ {

    protected static final Unit<Length> DEFAULT_DISTANCE_UNIT = SI.KILOMETER;
    protected static final Unit<Velocity> DEFAULT_SPEED_UNIT = NonSI.KILOMETERS_PER_HOUR;
    static final double DEFAULT_MAX_SPEED = 50d;
    static final Point DEFAULT_MIN_POINT = new Point(0, 0);
    static final Point DEFAULT_MAX_POINT = new Point(10, 10);

    private final Unit<Length> distanceUnit;
    private final Unit<Velocity> speedUnit;
    private final Point min;
    private final Point max;
    private final double maxSpeed;
    private final ArrayList<Rectangle> forbiddenZones;

    public static NoFlyZoneRMB create() {
        return new NoFlyZoneRMB(DEFAULT_DISTANCE_UNIT, DEFAULT_SPEED_UNIT, DEFAULT_MIN_POINT, DEFAULT_MAX_POINT, DEFAULT_MAX_SPEED, new ArrayList<>());
    }

    NoFlyZoneRMB(Unit<Length> distanceUnit, Unit<Velocity> speedUnit, Point min, Point max, double maxSpeed, ArrayList<geom.Rectangle> forbiddenZones) {
        this.distanceUnit = distanceUnit;
        this.speedUnit = speedUnit;
        this.min = min;
        this.max = max;
        this.maxSpeed = maxSpeed;
        this.forbiddenZones = forbiddenZones;
    }

    public Unit<Length> getDistanceUnit() {
        return this.distanceUnit;
    }

    public Unit<Velocity> getSpeedUnit() {
        return this.speedUnit;
    }

    @Override
    public NoFlyZoneRMB withDistanceUnit(Unit<Length> unit) {
        return new NoFlyZoneRMB(unit, this.speedUnit, this.min, this.max, this.maxSpeed, this.forbiddenZones);
    }

    @Override
    public NoFlyZoneRMB withSpeedUnit(Unit<Velocity> unit) {
        return new NoFlyZoneRMB(this.distanceUnit, unit, this.min, this.max, this.maxSpeed, this.forbiddenZones);
    }

    public NoFlyZoneRMB withMinPoint(Point minPoint) {
        return new NoFlyZoneRMB(this.distanceUnit, this.speedUnit, minPoint, this.max, this.maxSpeed, this.forbiddenZones);
    }

    public NoFlyZoneRMB withMaxPoint(Point maxPoint) {
        return new NoFlyZoneRMB(this.distanceUnit, this.speedUnit, this.min, maxPoint, this.maxSpeed, this.forbiddenZones);
    }

    public NoFlyZoneRMB withMaxSpeed(double maxSpeed) {
        checkArgument(maxSpeed > 0d,
                "Max speed must be strictly positive but is %s.",
                maxSpeed);
        return new NoFlyZoneRMB(this.distanceUnit, this.speedUnit, this.min, this.max, maxSpeed, this.forbiddenZones);
    }

    public NoFlyZoneRMB withForbiddenZones(List<Rectangle> forbiddenZones) {
        return new NoFlyZoneRMB(this.distanceUnit, this.speedUnit, this.min, this.max, this.maxSpeed, new ArrayList<geom.Rectangle>(forbiddenZones));
    }

    Point getMin() {
        return this.min;
    }

    Point getMax() {
        return this.max;
    }

    double getMaxSpeed() {
        return this.maxSpeed;
    }

    @Override
    public NoFlyZoneRoadModel build(DependencyProvider dependencyProvider) {
        return new NoFlyZoneRoadModel(this);
    }

    public List<Rectangle> getForbiddenZones() {
        return this.forbiddenZones;
    }

}
