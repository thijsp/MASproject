package agents;

import cnet.Auction;
import cnet.Bid;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import communication.BidMessage;

import javax.swing.text.html.Option;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

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
    protected void onPackageDelivered() {

    }

    private static class AuctionState {
        Optional<DistributionCenter> moderator = Optional.absent();
        Optional<Bid> myBid = Optional.absent();

        boolean hasModerator() { return this.moderator.isPresent(); }

        Optional<Bid> myBid() {return this.myBid; }

        void addBid(Bid bid) {this.myBid = Optional.of(bid);}
    }
}
