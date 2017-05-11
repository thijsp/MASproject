package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;


public class DistributionCenter extends Depot{

    DistributionCenter(Point position, double capacity) {
        super(position);
        this.setCapacity(capacity);
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    }
}
