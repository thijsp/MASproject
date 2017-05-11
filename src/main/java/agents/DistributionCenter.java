package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

import java.util.*;


public class DistributionCenter extends Depot {

    private Deque<DroneParcel> availableParcels = new LinkedList<>();
    DistributionCenter(Point position, double capacity) {
        super(position);
        this.setCapacity(capacity);
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    }

    public void addParcel(DroneParcel p) {
        this.availableParcels.add(p);
    }

    public DroneParcel getRandomParcel() {
        return availableParcels.pop();
    }
}
