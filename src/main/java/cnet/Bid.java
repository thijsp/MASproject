package cnet;

import agents.UAV;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class Bid {

    private double deliveryTime;
    private UAV bidder;
    private Auction auction;

    public Bid(UAV bidder, double deliveryTime) {
        this.bidder = bidder;
        this.deliveryTime = deliveryTime;
    }

    public double getBid() {
        return deliveryTime;
    }

    public UAV getBidder() {
        return bidder;
    }

    public Auction getAuction() {
        return auction;
    }

    public void addAuction(Auction auction) {
        this.auction = auction;
    }
}
