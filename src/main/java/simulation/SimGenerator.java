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
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.View.Builder;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

public final class SimGenerator {
    static final double VEHICLE_SPEED_KMH = 50.0D;
    static final Point MIN_POINT = new Point(-10.0D, -10.0D);
    static final Point MAX_POINT = new Point(10.0D, 10.0D);
    static final long TICK_LENGTH = 1000L;
    static final long RANDOM_SEED = 123L;
    static final int NUM_VEHICLES = 200;
    static final int TEST_SPEEDUP = 26;
    static final long TEST_STOP_TIME = 600000L;
    static final Point DEPOT_LOCATION = new Point(5.0D, 5.0D);

    private SimGenerator() {
    }

    public static void main(String[] args) {
        run(false);
    }

    public static void run(boolean testing) {
        Builder viewBuilder = View.builder().with(PlaneRoadModelRenderer.builder())
                .with(RoadUserRenderer.builder().withImageAssociation(DistributionCenter.class, "/graphics/perspective/tall-building-64.png"))
                .with(CommRenderer.builder().withMessageCount());
        if(testing) {
            viewBuilder = viewBuilder.withSpeedUp(16).withAutoClose().withAutoPlay().withSimulatorEndTime(600000L);
        }

        Simulator sim = Simulator.builder().setTickLength(1000L).setRandomSeed(RANDOM_SEED)
                .addModel(RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D))
                .addModel(DefaultPDPModel.builder())
                .addModel(CommModel.builder())
                .addModel(viewBuilder)
                .build();

        // create and register the depot
        DistributionCenter depot = new DistributionCenter(DEPOT_LOCATION, 1337.0D, sim.getRandomGenerator());
        sim.register(depot);

        // create and register the UAVs
        for(int i = 0; i < 20; ++i) {
            UAV uav = new UAV(sim.getRandomGenerator(), depot);
            sim.register(uav);
        }

        // create a ticklistner that generates parcels, registers them and add them to the depot
        ParcelGenerator parcelgen = new ParcelGenerator(depot, sim);
        sim.addTickListener(parcelgen);


//        for (int i = 0; i < 2000; ++i) {
//            Point destination = sim.getModelProvider().getModel(PlaneRoadModel.class).getRandomPosition(sim.getRandomGenerator());
//            DroneParcel parcel = new DroneParcel(DEPOT_LOCATION, destination);
//            sim.register(parcel);
//            depot.addParcel(parcel);
//        }

        System.out.println(sim.getModelProvider().getModel(PlaneRoadModel.class).getObjects().size());

        sim.start();
    }

}



