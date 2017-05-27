package agents;

import cnet.Auction;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * Created by anthony on 5/11/17.
 */
public class DroneParcel extends Parcel {

    private DistributionCenter depot;
    private Auction auction;

    public DroneParcel(Point destination, DistributionCenter depot) {
        super(Parcel.builder(depot.getPosition().get(), destination).buildDTO());
        this.depot = depot;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return this.auction;
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        super.initRoadPDP(pRoadModel, pPdpModel);
    }

    public DistributionCenter getDepot() {
        return depot;
    }
}
