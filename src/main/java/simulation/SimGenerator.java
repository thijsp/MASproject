package simulation;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.UAV;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.View.Builder;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;

public final class SimGenerator {
    static final double MIN_SPEED = 5000.0D;
    static final double MAX_SPEED = 8000.0D;

    static final Point MIN_POINT = new Point(-10.0D, -10.0D);
    static final Point MAX_POINT = new Point(10.0D, 10.0D);
    static final long TICK_LENGTH = 1000L;
    static final long RANDOM_SEED = 123L;
    static final int NUM_VEHICLES = 200;
    static final int TEST_SPEEDUP = 26;
    static final long TEST_STOP_TIME = 60000000L;

    static final int DEPOTS = 1;
    static final int UAVS = 4;
    static final int MAX_PARCELS = 100;

    static final boolean TESTING = true;

    private SimGenerator() {
    }

    public static void main(String[] args) {
        run(TESTING);
    }

    public static void run(boolean testing) {
        Builder viewBuilder = View.builder().with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder().withImageAssociation(DistributionCenter.class, "/graphics/perspective/tall-building-64.png"))
                .with(CommRenderer.builder().withMessageCount());
        if(testing) {
            viewBuilder = viewBuilder.withSpeedUp(TEST_SPEEDUP).withAutoClose().withAutoPlay().withSimulatorEndTime(TEST_STOP_TIME);
        }

        Simulator sim = Simulator.builder().setTickLength(1000L).setRandomSeed(RANDOM_SEED)
                .addModel(RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder)
                .build();

        RoadModel rm = sim.getModelProvider().tryGetModel(RoadModel.class);

        ArrayList<DistributionCenter> depots = new ArrayList<>();
        // create and register the depots
        for (int i = 0; i < DEPOTS; i++) {
            Point depotLocation = rm.getRandomPosition(sim.getRandomGenerator());
            DistributionCenter depot = new DistributionCenter(depotLocation, 1337.0D, sim.getRandomGenerator());
            sim.register(depot);
            depots.add(depot);
        }

        // create and register the UAVs
        for(int i = 0; i < UAVS; ++i) {
            double speed = getRandomspeed(sim.getRandomGenerator(), MAX_SPEED, MIN_SPEED);
            UAV uav = new UAV(sim.getRandomGenerator(), speed);
            sim.register(uav);
        }

        // create a ticklistner that generates parcels, registers them and add them to a depot
        ParcelGenerator parcelgen = new ParcelGenerator(depots, sim, TEST_STOP_TIME, MAX_PARCELS);
        sim.addTickListener(parcelgen);


        System.out.println(sim.getModelProvider().getModel(PlaneRoadModel.class).getObjects().size());

        sim.start();
    }

    public static double getRandomspeed(RandomGenerator rnd, double minSpeed, double maxSpeed) {
        double random = rnd.nextDouble();
        double speed = minSpeed + (maxSpeed - minSpeed) * random;
        System.out.println(speed);
        return speed;
    }

}



