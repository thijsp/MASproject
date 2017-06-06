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

    private final int id;
    private final DistributionCenter depot;
    // private Auction auction;
    private final List<Point> shortestPath;
    private final List<Point> shortestEscape;
    private long pickUpTime;
    private long deliveredTime;
    private long creationTime;

    public DroneParcel(Point destination, DistributionCenter depot, List<Point> shortestPath, List<Point> shortestEscape, int id) {
        super(Parcel.builder(depot.getPosition().get(), destination).buildDTO());
        this.id = id;
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
        return 0xcafebabe ^ this.getDeliveryLocation().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DroneParcel))
            return false;
        return this.getDeliveryLocation().equals(((DroneParcel) other).getDeliveryLocation());
    }

    @Override
    public String toString() {
        return String.format("<Parcel %d>", this.id);
    }

    public void setPickupTime(long time) {
        this.pickUpTime = time;
    }

    public void setDeliveredTime(long time) {
        this.deliveredTime = time;
    }

    public void setCreationTime(long time) { this.creationTime = time; }

    public long getDeliveryTime() {
        assert this.deliveredTime != 0 && this.pickUpTime != 0;
        return this.deliveredTime - this.pickUpTime;
    }

    public long getExistanceTime() {
        assert this.deliveredTime != 0;
        return this.deliveredTime - this.creationTime;
    }

}
