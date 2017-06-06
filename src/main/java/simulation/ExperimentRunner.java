package simulation;

import agents.DistributionCenter;
import agents.UAV;
import agents.StaticUAV;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.NoFlyZoneRMB;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.NoFlyZoneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import geom.Rectangle;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.RGB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentRunner {
    static final double MIN_SPEED = 10.0D;
    static final double MAX_SPEED = 30.0D;
    static final double BAT_CAPACITY = 3600 * 300.0D;
    static final double MOT_POWER = 130.0;

    static final Point MIN_POINT = new Point(0.0D, 0.0D);
    static final Point MAX_POINT = new Point(10.0D, 10.0D);
    static final long TICK_LENGTH = 1000L;
    static final long RANDOM_SEED = 124L;
    static final int TEST_SPEEDUP = 500;
    static final long TEST_STOP_TIME = 6000000000L;

    static final int MAX_PARCELS = 400;

    static final boolean TESTING = true;
    static final boolean randomDepots = true;
    static final boolean WITH_GUI = false;

    private ExperimentRunner() {
    }

    public static void main(String[] args) {
        int steps = 2;
        ExperimentInput input = new ExperimentInput(steps, 1, 10);
        ExperimentResult result = new ExperimentResult(steps, 1);
        runParcelSizeExperiment(TESTING, input, result);
        System.out.println(result.getDeliveredAverages());
        System.out.println(result.getExistanceAverages());
        result.saveResult();
    }

    public static void runParcelSizeExperiment(boolean testing, ExperimentInput experimentInput, ExperimentResult res) {

        List<Integer> parcelSize = experimentInput.getParcelList();
        System.out.println(parcelSize);
        int DEPOTS = experimentInput.DEPOTS;
        int UAVS = experimentInput.UAVS;

        for (int exp = 0; exp < experimentInput.STEPS; exp++) {
            View.Builder viewBuilder = View.builder()
                    //.with(PlaneRoadModelRenderer.builder())
                    .with(NoFlyZoneRoadModelRenderer.builder())
                    .with(RoadUserRenderer.builder()
                            .withImageAssociation(DistributionCenter.class, "/graphics/flat/factory-32.png")
                            .withToStringLabel()
                            .withColorAssociation(UAV.class, new RGB(0, 255, 0)));
            //.with(CommRenderer.builder().withMessageCount());
            if(testing) {
                viewBuilder = viewBuilder.withSpeedUp(TEST_SPEEDUP).withAutoClose().withAutoPlay().withSimulatorEndTime(TEST_STOP_TIME);
            }

            List<Rectangle> forbiddenZones = Maze.getSmallMaze().getWalls(MIN_POINT, MAX_POINT).collect(Collectors.toList());

            Simulator sim;
            if (WITH_GUI) {
                sim = Simulator.builder().setTickLength(1000L).setRandomSeed(RANDOM_SEED)
                        //.addModel(RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D))
                        .addModel(NoFlyZoneRMB.create().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D).withForbiddenZones(forbiddenZones))
                        .addModel(DefaultPDPModel.builder())
                        .addModel(CommModel.builder())
                        .addModel(viewBuilder)
                        .build();
            }
            else {
                sim = Simulator.builder().setTickLength(1000L).setRandomSeed(RANDOM_SEED)
                        //.addModel(RoadModelBuilders.plane().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D))
                        .addModel(NoFlyZoneRMB.create().withMinPoint(MIN_POINT).withMaxPoint(MAX_POINT).withMaxSpeed(50.0D).withForbiddenZones(forbiddenZones))
                        .addModel(DefaultPDPModel.builder())
                        .addModel(CommModel.builder())
                        .build();
            }

            RoadModel rm = sim.getModelProvider().tryGetModel(RoadModel.class);
            RandomGenerator rnd = sim.getRandomGenerator();

            ArrayList<DistributionCenter> depots = new ArrayList<>();
            JDKRandomGenerator gen = new JDKRandomGenerator();
            gen.setSeed(124L);
            // create and register the depots
            for (int i = 0; i < DEPOTS; i++) {
                Point depotLocation;
                if (randomDepots) {
                    depotLocation = rm.getRandomPosition(gen);
                } else {
                    depotLocation = rm.getRandomPosition(rnd);
                }
                DistributionCenter depot = new DistributionCenter(i, depotLocation, 1337.0D);
                sim.register(depot);
                depots.add(depot);
            }

            // create and register the UAVs
            for (int i = 0; i < UAVS; ++i) {
                double speed = getRandomSpeed(rnd, MAX_SPEED, MIN_SPEED);
                Point startPos = rm.getRandomPosition(rnd);
                UAV uav = new StaticUAV(i, startPos, speed, BAT_CAPACITY, MOT_POWER, MAX_SPEED);
                ((StaticUAV)uav).setMaxAssignments(parcelSize.get(exp));
                sim.register(uav);
            }

            // create a ticklistner that generates parcels, registers them and add them to a depot
            ParcelGenerator parcelgen = new ParcelGenerator(depots, sim, TEST_STOP_TIME, MAX_PARCELS);
            ExperimentListner result = new ExperimentListner(sim, parcelgen);
            sim.addTickListener(parcelgen);
            sim.addTickListener(result);

            sim.start();

            // report the results of this run
            res.report(result.getDeliveredParcels());
            res.report(result.getDeliveredParcels());
            res.reportDeliveredAverage(result.getAverageDeliveryTime());
            res.reportExistanceAverage(result.getAverageExistanceTime());
        }

    }


    public static double getRandomSpeed(RandomGenerator rnd, double minSpeed, double maxSpeed) {
        double random = rnd.nextDouble();
        return minSpeed + (maxSpeed - minSpeed) * random;
    }

}

