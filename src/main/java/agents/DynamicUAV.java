package agents;

import cnet.Auction;
import cnet.Bid;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import communication.BidMessage;
import communication.TypedMessage;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Drone with a dynamic contract net implementation.
 */
public final class DynamicUAV extends UAV {

    // private Optional<Bid> assignedParcel = Optional.absent();
    public DynamicUAV(int id, Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(id, startPos, speed, batteryCapacity, motorPower, maxSpeed);
    }

    @Override
    protected void move(TimeLapse time) {
        switch (this.state) {
            case DELIVERING:
                DroneParcel parcel = this.parcel.get();
                this.fly(parcel.getDeliveryLocation(), time);
                break;
            case CHARGING:
                this.chargeBattery(time);
                break;
            case OUT_OF_SERVICE:
                this.goToNearestDepot(time);
            case PICKING:
                if (this.parcel.isPresent())
                    this.pickParcel(time);
                else
                    this.goToNearestDepot(time);
        }
    }
    @Override
    protected void onNewAuction(Auction auction) {
        switch (this.state) {
            case DELIVERING:
            case OUT_OF_SERVICE:
                // ignore
                break;
            // TODO below: check participation in existing auctions
            case PICKING:
            case CHARGING:
                if (this.wantsToBid(auction.getParcel())) {
                    List<Point> fullPath = fullDeliveryPath(auction.getParcel()).get();
                    Bid bid = new Bid(this, this.getRoadModel().getDistanceOfPath(fullPath).getValue() / this.getSpeed(), auction);
                    TypedMessage offer = BidMessage.createNewBid(bid);
                    this.sendDirect(offer, auction.getModerator());
                }
        }
    }

    @Override
    protected void onAuctionWon(Bid bid) {
        checkArgument(this.equals(bid.getBidder()), "Won an auction with another UAV's bid");

        switch (this.state) {
            case DELIVERING:
            case OUT_OF_SERVICE:
                // Immediately refuse.
                this.sendDirect(BidMessage.createRefusal(bid), bid.getAuction().getModerator());
                break;
            case PICKING:
            case CHARGING:
                // Won auctions might be accepted by the UAV in these states.
                // First, check if we can still fulfill the bid
                double promisedTime = bid.getDeliveryTime();
                List<Point> fullPath = this.fullDeliveryPath(bid.getParcel()).get();
                double distance = this.getRoadModel().getDistanceOfPath(fullPath).getValue();
                double actualTime = distance / this.getSpeed();
                if (actualTime <= 1.1 * promisedTime && this.getMotor().canFlyTime(actualTime, this.getSpeed())) {
                    // We can still deliver the parcel in (approximately) the promised time frame
                    if (this.parcel.isPresent()) {
                        // We were already moving towards our favorite parcel
                        if (this.state == DroneState.CHARGING)
                            throw new IllegalStateException("Drone was charging when it should have been moving to pick up a parcel");
                        // Check which offer is the best
                        DroneParcel prev = this.parcel.get();
                        // Bid prevBid = this.assignedParcel.get();
                        double prevDist = this.getRoadModel()
                                .getDistanceOfPath(this.fullDeliveryPath(prev).get())
                                .getValue();
                        if (distance < prevDist) {
                            // Switch to (better) new bid
                            this.parcel = Optional.of(bid.getParcel());
                            Bid prevBid = new Bid(this, 0.0 /* not used */, new Auction(prev, prev.getDepot()));
                            this.sendDirect(BidMessage.createRefusal(prevBid), prevBid.getAuction().getModerator());
                        } else {
                            // Previous offer was better, don't switch
                            this.sendDirect(BidMessage.createRefusal(bid), bid.getAuction().getModerator());
                        }
                    } else {
                        // We did not have a favorite parcel yet, accept this parcel!
                        this.parcel = Optional.of(bid.getParcel());
                        this.state = DroneState.PICKING;
                        this.closestDepot = Optional.absent();
                    }
                } else {
                    // It is now impossible for us to deliver the parcel, refuse
                    this.sendDirect(BidMessage.createRefusal(bid), bid.getAuction().getModerator());
                }
                break;
        }
    }

    @Override
    protected void onAuctionLost(Auction auction) {
        switch (this.state) {
            case DELIVERING:
            case OUT_OF_SERVICE:
            case CHARGING:
                // ignore
                break;
            case PICKING:
                if (this.parcel.isPresent() && auction.getParcel().equals(this.parcel.get())) {
                    // We were denied our favorite parcel, go back to roaming
                    this.parcel = Optional.absent();
                }
        }
    }

    @Override
    protected void onAuctionDone(Auction auction) {
        // Dynamic drone does not keep track of auction participations.
        // Nothing to do here.
    }

    @Override
    protected void onPackageDelivered(DroneParcel parcel) {
        // We've moved to the PICKING state at this point
        // Nothing left to do here.
    }

    @Override
    protected void onFullyCharged() {
        // Stay in the CHARGING state, nothing to do here.
    }
}
