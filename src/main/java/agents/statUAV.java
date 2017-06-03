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

    protected HashMap<Auction, AuctionState> auctions = new HashMap<>();

    public statUAV(Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(startPos, speed, batteryCapacity, motorPower, maxSpeed);
    }

    @Override
    protected void move(TimeLapse time) {
        Optional<DroneParcel> parcel = this.getParcel();
        if (parcel.isPresent()) {
            this.pickParcel(time);
        }
        else {
            this.goToNearestDepot(time);
        }
    }

    @Override
    protected void onNewAuction(Auction auction) {
        //TODO: difference between states
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
        if(this.getParcel().isPresent()) {
            AuctionState state = new AuctionState();
            state.addBid(bid);
            this.auctions.put(auction, state);
        }
        else {
            this.assignParcel(bid.getParcel());
        }
    }

    @Override
    protected void onAuctionLost(Auction auction) {
        if (this.auctions.containsKey(auction)){
            this.auctions.remove(auction);
        }
        Optional<DroneParcel> parcel = this.getParcel();
        if (parcel.isPresent()) {
            if (parcel.get().equals(auction.getParcel())) {
                this.removeParcel();
                this.state = DroneState.PICKING;
                if (!this.auctions.keySet().isEmpty()) {
                    Optional<DroneParcel> nextParcel = this.getNextParcel();
                    if (nextParcel.isPresent()) {
                        this.assignParcel(this.getNextParcel().get());
                    }
                }
            }
        }
    }

    @Override
    protected void onAuctionDone(Auction auction) {
        if(this.auctions.containsKey(auction)) {
            this.auctions.remove(auction);
        }
    }

    @Override
    protected void onPackageDelivered(DroneParcel parcel) {
        System.out.println(this.toString() + " delivered " + parcel.toString());
        Auction auction = new Auction(parcel, parcel.getDepot());
        System.out.println(this.auctions.containsKey(auction));
        this.auctions.remove(auction);
        System.out.println(this.auctions.size());
        Set<Auction> myAuctions = this.auctions.keySet();
        if (!myAuctions.isEmpty()) {
            Optional<DroneParcel> nextParcel = this.getNextParcel();
            if(nextParcel.isPresent()) {
                this.assignParcel(nextParcel.get());
                System.out.println(this.toString() + " " + parcel.toString() + " " + this.state);
                System.out.println(nextParcel.toString());
                this.state = DroneState.PICKING;

            }
            else {
                this.state = DroneState.OUT_OF_SERVICE;
            }
        }
        else {
            this.state = DroneState.PICKING;
        }
    }

    public Optional<DroneParcel> getNextParcel() {
        Set<Auction> myAuctions = this.auctions.keySet();
        Iterator<Auction> it = myAuctions.iterator();
        Auction nextAuction = it.next();
        double delTime = this.auctions.get(nextAuction).myBid().get().getDeliveryTime();
        while (it.hasNext()) {
            Auction auction = it.next();
            System.out.println(auction.getParcel());
            double thisDel = this.auctions.get(auction).myBid().get().getDeliveryTime();
            if(thisDel < delTime) {
                delTime = thisDel;
                nextAuction = auction;
            }
        }
        if (this.wantsToBid(nextAuction.getParcel())) {
            return Optional.of(nextAuction.getParcel());
        }
        else {
            return Optional.absent();
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
