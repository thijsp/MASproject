package agents;

import cnet.Auction;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;
import java.util.Objects;

/**
 * Created by anthony on 5/11/17.
 */
public final class DroneParcel extends Parcel {

    private final DistributionCenter depot;
    // private Auction auction;
    private final List<Point> shortestPath;
    private final List<Point> shortestEscape;

    public DroneParcel(Point destination, DistributionCenter depot, List<Point> shortestPath, List<Point> shortestEscape) {
        super(Parcel.builder(depot.getPosition().get(), destination).buildDTO());
        this.depot = depot;
        this.shortestPath = shortestPath;
        this.shortestEscape = shortestEscape;
    }

//    public void setAuction(Auction auction) {
//        this.auction = auction;
//    }
//
//    public Auction getAuction() {
//        return this.auction;
//    }

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

    @Override
    public int hashCode() {
        return this.getDeliveryLocation().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DroneParcel))
            return false;
        return this.getDeliveryLocation().equals(((DroneParcel) other).getDeliveryLocation());
    }
}
