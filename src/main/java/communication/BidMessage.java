package communication;

import cnet.Auction;
import cnet.Bid;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class BidMessage extends AuctionMessage {

    private Bid bid;

    public BidMessage(Bid bid) {
        super(bid.getAuction());
        this.setType(MessageType.BID);
        this.bid = bid;
    }

    public double getDeliveryTime() {
        return this.bid.getBid();
    }

    public Bid getBid() {
        return this.bid;
    }
}
