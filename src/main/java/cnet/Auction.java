package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.UAV;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Comparator;
import java.util.List;
import com.google.common.base.Optional;
import java.util.stream.Collectors;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class Auction {

    private DroneParcel parcel;
    private List<Bid> bids;
    private DistributionCenter moderator;
    private boolean open;

    public Auction(DroneParcel parcel, DistributionCenter moderator) {
        this.parcel = parcel;
        this.moderator = moderator;
        this.bids = new ArrayList<>();
        this.open = true;
        parcel.setAuction(this);
    }

    public DroneParcel getParcel() {
        return this.parcel;
    }

    public DistributionCenter getModerator() {
        return this.moderator;
    }

    public boolean isOpen() {
        return open;
    }

    public List<UAV> getParticipants() {
        return this.bids.stream().map(Bid::getBidder).collect(Collectors.toList());
    }

    public void addBid(Bid bid) {
        Optional<Bid> doubleBid = Optional.absent();
        for (Bid alreadyBid : this.bids) {
            if (alreadyBid.getBidder().equals(bid.getBidder())) {
                doubleBid = Optional.of(alreadyBid);
            }
        }
        if (doubleBid.isPresent()) {
            this.bids.remove(doubleBid);
            this.bids.add(bid);
            bid.addAuction(this);

        } else {
            this.bids.add(bid);
            bid.addAuction(this);
        }
    }

    public Optional<Bid> getBestBid() {
        java.util.Optional<Bid> bestBid = bids.stream().min(Comparator.comparingDouble(Bid::getBid));
        return bestBid.isPresent() ? Optional.of(bestBid.get()) : Optional.absent();
    }

    public void close() {
        if (!this.open) {throw new IllegalStateException("auction is closed");}
        this.open = false;
    }

    public boolean hasBids() {
        return !this.bids.isEmpty();
    }

    public Bid getMyBid(UAV bidder) {
        if (!this.hasBids()) {throw new IllegalArgumentException("no bidder like this");}
        for (Bid bid : this.bids) {
            if (bid.getBidder().equals(bidder)) {
                return bid;
            }
        }
        return null;
    }


    public void deleteBid(Bid refusedBid) {
        if (!this.bids.contains(refusedBid)) {throw new IllegalArgumentException("no bid like this");}
        this.bids.remove(refusedBid);
    }
}
