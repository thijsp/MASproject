package communication;

import cnet.Auction;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class BidMessage extends AuctionMessage {

    private double deliveryTime;

    public BidMessage(Auction auction, Double deliveryTime) {
        super(auction);
        this.setType(MessageType.BID);
        this.deliveryTime = deliveryTime;
    }

    public Double getDeliveryTime() {
        return this.deliveryTime;
    }
}
