package cnet;

import agents.DroneParcel;
import agents.UAV;

/**
 * A Bid represents a non-binding claim by a UAV that it can deliver a parcel in the given time frame.
 */
public final class Bid {

    private final double deliveryTime;
    private final UAV bidder;
    private final Auction auction;

    public Bid(UAV bidder, double deliveryTime, Auction auction) {
        this.bidder = bidder;
        this.deliveryTime = deliveryTime;
        this.auction = auction;
        // auction.addBid(this);
    }

    public final double getDeliveryTime() {
        return this.deliveryTime;
    }

    public final UAV getBidder() {
        return this.bidder;
    }

    public final Auction getAuction() {
        return this.auction;
    }

    public final DroneParcel getParcel() {
        return this.auction.getParcel();
    }

    @Override
    public String toString() {
        return String.format("<Bid by %s for %s [time %.03f]>", this.bidder, this.auction, this.deliveryTime);
    }
}
