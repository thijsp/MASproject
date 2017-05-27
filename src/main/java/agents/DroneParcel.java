package agents;

import cnet.Auction;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;

/**
 * Created by anthony on 5/11/17.
 */
public class DroneParcel extends Parcel {

    private DistributionCenter depot;
    private List<Point> shortestPath;
    private List<Point> shortestEscape;

    public DroneParcel(Point destination, DistributionCenter depot, List<Point> shortestPath, List<Point> shortestEscape) {
        super(Parcel.builder(depot.getPosition().get(), destination).buildDTO());
        this.depot = depot;
        this.shortestPath = shortestPath;
        this.shortestEscape = shortestEscape;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        super.initRoadPDP(pRoadModel, pPdpModel);
    }

    public DistributionCenter getDepot() {
        return depot;
    }

    public List<Point> getShortestPath() {
        return shortestPath;
    }
    public List<Point> getShortestEscape() {
        return shortestEscape;
    }
}
