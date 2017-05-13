package simulation;

import agents.DistributionCenter;
import agents.DroneParcel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Created by thijspeirelinck on 12/05/2017.
 */
public class ParcelGenerator implements TickListener {

    private DistributionCenter depot;
    private final Simulator sim;
    private RandomGenerator rnd;
    private int parcels_generated = 0;
    private long stop_time;



    ParcelGenerator(DistributionCenter depot, Simulator sim, Long stop_time) {
        this.depot = depot;
        this.sim = sim;
        this.rnd = sim.getRandomGenerator();
        this.stop_time = stop_time;
    }

    @Override
    public void tick(TimeLapse time) {
        if(time.getTime() > this.stop_time) {
            this.sim.stop();
            System.out.println("simulation ended");
        } else if (this.rnd.nextDouble() < 0.2 & parcels_generated < 5) {
            if (this.depot.getPosition().isPresent()) {
                Point destination = this.sim.getModelProvider().getModel(PlaneRoadModel.class).getRandomPosition(this.sim.getRandomGenerator());
                DroneParcel parcel = new DroneParcel(this.depot.getPosition().get(), destination);
                this.sim.register(parcel);
                this.depot.addParcel(parcel);
                parcels_generated++;
            }
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
}
