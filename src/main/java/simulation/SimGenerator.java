package simulation;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import agents.DistributionCenter;
import agents.UAV;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.NoFlyZoneRMB;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.View.Builder;
import com.github.rinde.rinsim.ui.renderers.NoFlyZoneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import geom.Rectangle;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.RGB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class SimGenerator {
    static final double MIN_SPEED = 10.0D;
    static final double MAX_SPEED = 30.0D;
    static final double BAT_CAPACITY = 3600 * 300.0D;
    static final double MOT_POWER = 130.0;

    static final Point MIN_POINT = new Point(0.0D, 0.0D);
    static final Point MAX_POINT = new Point(10.0D, 10.0D);
    static final long TICK_LENGTH = 1000L;
    static final long RANDOM_SEED = 123L;
    static final int TEST_SPEEDUP = 30;
    static final long TEST_STOP_TIME = 60000000L;

    static final int DEPOTS = 3;
    static final int UAVS = 4;
    static final int MAX_PARCELS = 100;

    static final boolean TESTING = true;

    private SimGenerator() {
    }

    public static void main(String[] args) {
        run(TESTING);
    }

    public static void run(boolean testing) {
        Builder viewBuilder = View.builder()
                //.with(PlaneRoadModelRenderer.builder())
                .with(NoFlyZoneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder()
                        .withImageAssociation(DistributionCenter.class, "/graphics/perspective/tall-building-64.png")
                        .withToStringLabel()
                        .withColorAssociation(UAV.class, new RGB(0, 255, 0)));
                //.with(CommRenderer.builder().withMessageCount());
        if(testing) {
            viewBuilder = viewBuilder.withSpeedUp(TEST_SPEEDUP).withAutoClose().withAutoPlay().withSimulatorEndTime(TEST_STOP_TIME);
        }

        List<Rectangle> forbiddenZones = Maze.getSmallMaze().getWalls(MIN_POINT, MAX_POINT).collect(Collectors.toList());

        Simulator sim = Simulator.builder().setTickLength(1000L).setRandomSeed(RANDOM_SEED)
                //.addModel(RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D))
                .addModel(NoFlyZoneRMB.create().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D).withForbiddenZones(forbiddenZones))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder)
                .build();

        RoadModel rm = sim.getModelProvider().tryGetModel(RoadModel.class);
        RandomGenerator rnd = sim.getRandomGenerator();

        ArrayList<DistributionCenter> depots = new ArrayList<>();
        // create and register the depots
        for (int i = 0; i < DEPOTS; i++) {
            Point depotLocation = rm.getRandomPosition(rnd);
            DistributionCenter depot = new DistributionCenter(depotLocation, 1337.0D, rnd);
            sim.register(depot);
            depots.add(depot);
        }

        // create and register the UAVs
        for(int i = 0; i < UAVS; ++i) {
            double speed = getRandomSpeed(rnd, MAX_SPEED, MIN_SPEED);
            Point startPos = rm.getRandomPosition(rnd);
            UAV uav = new UAV(startPos, speed, BAT_CAPACITY, MOT_POWER, MAX_SPEED);
            sim.register(uav);
        }

        // create a ticklistner that generates parcels, registers them and add them to a depot
        ParcelGenerator parcelgen = new ParcelGenerator(depots, sim, TEST_STOP_TIME, MAX_PARCELS);
        sim.addTickListener(parcelgen);


        // System.out.println(sim.getModelProvider().getModel(PlaneRoadModel.class).getObjects().size());

        sim.start();
    }

    public static double getRandomSpeed(RandomGenerator rnd, double minSpeed, double maxSpeed) {
        double random = rnd.nextDouble();
        return minSpeed + (maxSpeed - minSpeed) * random;
    }

}



