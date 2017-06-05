package agents;

import cnet.Auction;
import cnet.Bid;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import communication.BidMessage;

import java.util.*;

/**
 * Created by thijspeirelinck on 2/06/17.
 */
public class statUAV extends UAV {

    protected HashMap<DroneParcel, AuctionState> parcels = new HashMap<>();
    private int maxParcels = 5;

    public statUAV(Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(startPos, speed, batteryCapacity, motorPower, maxSpeed);
    }

    public void setMaxAssignments(int maxAssignments) {
        this.maxParcels = maxAssignments;
    }

    @Override
    protected void move(TimeLapse time) {
        Optional<DroneParcel> parcel = this.getParcel();
        if (parcel.isPresent()) {
            // if this UAV was picking up a parcel, go along with this task
            this.pickParcel(time);
        }
        else if (this.parcels.size() > 0) {
            // if this UAV didn't have a parcel to pick up, check if it has remaining assigned tasks
            // go the closest parcel first
            Optional<DroneParcel> nextParcel = this.getNextParcel();
            if (nextParcel.isPresent()) {
                this.assignParcel(nextParcel.get());
                this.pickParcel(time);
            }
            else {
                // if this UAV has remaining tasks but refuses to do any, decide what to do next
                this.decideAction(time);
            }
        }
        else {
            // if no tasks left, just go charging in the nearest depot
            this.goToNearestDepot(time);
        }
    }

    private void decideAction(TimeLapse time) {
        if (this.getPosition().get().equals(this.getNearestDepot())) {
            // if the UAV is in a depot but it can still not deliver it's closest (and thus any) parcel refuse all
            this.refuseParcels();
        }
        else {
            // if the UAV isn't in a depot, just go to the closest depot
            this.goToNearestDepot(time);
        }
    }

    private void refuseParcels() {
        // in this rare case, also static UAVs can refuse parcels
        Set<DroneParcel> refusingParcels = this.parcels.keySet();
        refusingParcels.stream()
                .forEach(parcel ->
                        this.sendDirect(BidMessage.
                                createRefusal(this.parcels.get(parcel).myBid().get()), parcel.getDepot()));
        System.out.println("uav " + this + " refused its parcels" + refusingParcels);
        this.parcels = new HashMap<>();

    }

    @Override
    protected void onNewAuction(Auction auction) {
        switch (this.state) {
            case DELIVERING:
                //don't participate in new auctions
                break;
            case OUT_OF_SERVICE:
                //don't participate in new auctions
                break;
            case PICKING:
                // bid on auctions if not yet too many assignments
                if (this.parcels.size() < maxParcels) {
                    bidOnAuction(auction);
                }
                break;
            case CHARGING:
                // bid on auctions if not yet too many assignments and enough charged
                if (this.parcels.size() < maxParcels && this.getMotor().getPowerSource().getBatteryLevel() > 0.8) {
                    bidOnAuction(auction);
                }
                break;
        }
    }

    private void bidOnAuction(Auction auction) {
        DistributionCenter depot = auction.getModerator();
        DroneParcel parcel = auction.getParcel();
        if(this.wantsToBid(parcel)) {
            double delTime = this.calculateDeliveryTime(parcel.getDeliveryLocation());
            Bid bid = new Bid(this, delTime, auction);
            BidMessage answer = BidMessage.createNewBid(bid);
            this.sendDirect(answer, depot);
        }
    }

    @Override
    protected void onAuctionWon(Bid bid) {
        Auction auction = bid.getAuction();
        AuctionState state = new AuctionState();
        state.addBid(bid);
        this.parcels.put(auction.getParcel(), state);
        if (!this.getParcel().isPresent()) {
            this.takeNextParcel();
        }
    }

    @Override
    protected void onAuctionLost(Auction auction) {
        DroneParcel lostParcel = auction.getParcel();
        if (this.parcels.containsKey(lostParcel)) {
            this.parcels.remove(lostParcel);
        }
        Optional<DroneParcel> parcel = this.getParcel();
        if (parcel.isPresent()) {
            if (parcel.get().equals(auction.getParcel())) {
                this.removeParcel();
                this.takeNextParcel();
            }
        }
    }

    @Override
    protected void onAuctionDone(Auction auction) {
        DroneParcel parcel = auction.getParcel();
        if(this.parcels.containsKey(parcel)) {
            this.parcels.remove(parcel);
        }
        switch (this.state) {
            case DELIVERING:
                // done message is either about an other parcel, and this is already deleted
                // or message is about this drone, thus neglect
                break;
            case PICKING:
                // if this drone is picking the parcel of which the auction is over, stop
                Optional<DroneParcel> currentParcel = this.getParcel();
                if (currentParcel.isPresent()) {
                    if (currentParcel.get().equals(parcel)) {
                        //this.removeParcel();
                        //this.takeNextParcel();
                        System.out.println(this.toString() + " " + parcel);
                        throw new IllegalArgumentException();
                    }
                }
                break;
        }
        //onAuctionLost(auction);
    }

    @Override
    protected void onPackageDelivered(DroneParcel parcel) {
        //this.parcels.remove(parcel);
        this.takeNextParcel();
    }

//    @Override
//    protected void onFullyCharged() {
//        this.state = DroneState.PICKING;
//    }

    public Optional<DroneParcel> getNextParcel() {
        Set<DroneParcel> myParcels = this.parcels.keySet();
        Iterator<DroneParcel> it = myParcels.iterator();
        DroneParcel nextParcel = it.next();
        double delTime = this.parcels.get(nextParcel).myBid().get().getDeliveryTime();
        while (it.hasNext()) {
            DroneParcel parcel = it.next();
            double thisDel = this.parcels.get(parcel).myBid().get().getDeliveryTime();
            if(thisDel < delTime) {
                delTime = thisDel;
                nextParcel = parcel;
            }
        }
        if (this.wantsToBid(nextParcel)) {
            return Optional.of(nextParcel);
        }
        else {
            return Optional.absent();
        }
    }

    public void takeNextParcel() {
        //this.state = DroneState.PICKING;
        if (!this.parcels.keySet().isEmpty()) {
            Optional<DroneParcel> nextParcel = this.getNextParcel();
            if(nextParcel.isPresent()) {
                this.assignParcel(nextParcel.get());
                //this.closestDepot = Optional.absent();
                this.state = DroneState.PICKING;
            }
            else {
                this.state = DroneState.OUT_OF_SERVICE;
            }
        }
    }

    private static class AuctionState {
        Optional<DistributionCenter> moderator = Optional.absent();
        Optional<Bid> myBid = Optional.absent();

        boolean hasModerator() { return this.moderator.isPresent(); }

        Optional<Bid> myBid() {return this.myBid; }

        void addBid(Bid bid) {this.myBid = Optional.of(bid);}
    }

    @Override
    public String toString() {
        Point pos = this.getPosition().get();
        //return String.format("<UAV at (%.2f,%.2f) [Bat %.0f%%]>", pos.x, pos.y, 100*this.motor.getPowerSource().getBatteryLevel());
        return String.valueOf(this.hashCode());
    }
}
