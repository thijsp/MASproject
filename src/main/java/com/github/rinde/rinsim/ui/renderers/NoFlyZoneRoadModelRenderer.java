package com.github.rinde.rinsim.ui.renderers;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.ModelBuilder;
import com.github.rinde.rinsim.core.model.road.NoFlyZoneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import geom.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

/**
 * Created by anthony on 5/18/17.
 */
public final class NoFlyZoneRoadModelRenderer extends CanvasRenderer.AbstractCanvasRenderer {
    private final double xMargin;
    private final double yMargin;
    private final ImmutableList<Point> bounds;
    private final ImmutableList<Rectangle> forbiddenZones;

    NoFlyZoneRoadModelRenderer(RoadModel rm, double margin) {
        this.bounds = rm.getBounds();
        final double width = bounds.get(1).x - bounds.get(0).x;
        final double height = bounds.get(1).y - bounds.get(0).y;
        this.xMargin = width * margin;
        this.yMargin = height * margin;
        this.forbiddenZones = ((NoFlyZoneRoadModel) rm).getForbiddenZones();
    }

    @Override
    public void renderStatic(GC gc, ViewPort vp) {
        int xMin = vp.toCoordX(bounds.get(0).x);
        int yMin = vp.toCoordY(bounds.get(0).y);
        int xMax = vp.toCoordX(bounds.get(1).x);
        int yMax = vp.toCoordY(bounds.get(1).y);

        final int outerXmin = vp.toCoordX(vp.rect.min.x);
        final int outerYmin = vp.toCoordY(vp.rect.min.y);
        final int outerXmax = vp.toCoordX(vp.rect.max.x);
        final int outerYmax = vp.toCoordY(vp.rect.max.y);

        gc.setBackground(
                gc.getDevice().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gc.fillRectangle(outerXmin, outerYmin, outerXmax, outerYmax);

        gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
        gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
        gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);

        gc.setBackground((gc.getDevice().getSystemColor(SWT.COLOR_DARK_RED)));
        for (Rectangle zone : this.forbiddenZones) {
            xMin = vp.toCoordX(zone.xMin);
            yMin = vp.toCoordY(zone.yMin);
            xMax = vp.toCoordX(zone.xMax);
            yMax = vp.toCoordY(zone.yMax);
            gc.fillRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
            gc.drawRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
        }
    }

    @Override
    public void renderDynamic(GC gc, ViewPort vp, long time) {}

    @Override
    public Optional<ViewRect> getViewRect() {
        return Optional.of(new ViewRect(
                new Point(bounds.get(0).x - xMargin, bounds.get(0).y - yMargin),
                new Point(bounds.get(1).x + xMargin, bounds.get(1).y + yMargin)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends ModelBuilder.AbstractModelBuilder<NoFlyZoneRoadModelRenderer, Void> {
        public static final double DEFAULT_MARGIN = 0.02;

        private double margin;

        Builder() {
            this.margin = DEFAULT_MARGIN;
            setDependencies(RoadModel.class); // PlaneRMB is generic over RoadModel, can't request NoFlyZoneRoadModel here.
        }

        public NoFlyZoneRoadModelRenderer.Builder withMargin(double margin) {
            this.margin = margin; // meh
            return this;
        }

        @Override
        public NoFlyZoneRoadModelRenderer build(DependencyProvider dependencyProvider) {
            return new NoFlyZoneRoadModelRenderer(dependencyProvider.get(RoadModel.class), this.margin);
        }
    }
}
