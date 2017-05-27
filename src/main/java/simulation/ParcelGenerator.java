package simulation;

import agents.DistributionCenter;
import agents.DroneParcel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;

/**
 * Created by thijspeirelinck on 12/05/2017.
 */
public class ParcelGenerator implements TickListener {

    private static final double GEN_PROBABILITY = 0.02;
    private final Simulator sim;
    private RandomGenerator rnd;
    private int parcels_generated = 0;
    private long stop_time;
    private List<DistributionCenter> depots;
    private int maxParcels;



    ParcelGenerator(List<DistributionCenter> depots, Simulator sim, Long stop_time, int maxParcels) {
        this.sim = sim;
        this.rnd = sim.getRandomGenerator();
        this.stop_time = stop_time;
        this.depots = depots;
        this.maxParcels = maxParcels;
    }

    @Override
    public void tick(TimeLapse time) {
        if(time.getTime() > this.stop_time) {
            this.sim.stop();
            System.out.println("simulation ended");
            return;
        }

        if (this.rnd.nextDouble() < GEN_PROBABILITY & parcels_generated < this.maxParcels) {
            int nextDepot = this.rnd.nextInt(this.depots.size());
            DistributionCenter depot = this.depots.get(nextDepot);
            Point depotLoc = depot.getPosition().get();
            RoadModel rm = this.sim.getModelProvider().getModel(RoadModel.class);
            Point destination = rm.getRandomPosition(this.sim.getRandomGenerator());
            List<Point> shortestPath, shortestEscape;
            try {
                shortestPath = rm.getShortestPathTo(depot.getPosition().get(), destination);
            } catch (PathNotFoundException exc) {
                return;
            }
            shortestEscape = DistributionCenter.getPathToClosest(rm, destination);
            // System.out.println(String.format("New parcel added with destination (%.2f,%.2f)", destination.x, destination.y));
            DroneParcel parcel = new DroneParcel(destination, depot, shortestPath, shortestEscape);
            this.sim.register(parcel);
            depot.addParcel(parcel);
            parcels_generated++;
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
}
