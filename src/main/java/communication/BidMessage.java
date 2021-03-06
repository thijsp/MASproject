package communication;

import cnet.Auction;
import cnet.Bid;

/**
 * Common superclass for messages that are related to a bid (NewBid, Acceptance, Refusal)
 */
public final class BidMessage extends TypedMessage {

    public final Bid bid;

    private BidMessage(MessageType type, Bid bid) {
        super(type);
        this.bid = bid;
    }

    public static BidMessage createNewBid(Bid bid) {
        return new BidMessage(MessageType.NEW_BID, bid);
    }

    public static BidMessage createAuctionWon(Bid bid) {
        return new BidMessage(MessageType.AUCTION_WON, bid);
    }

    public static BidMessage createRefusal(Bid bid) {
        return new BidMessage(MessageType.PARCEL_REFUSED, bid);
    }

    public static BidMessage createBidRetrieval(Bid bid) {
        return new BidMessage(MessageType.PARCEL_ACCEPTED, bid);
    }

    public final Bid getBid() {
        return this.bid;
    }

    @Override
    String getDescription() {
        return "" + this.bid;
    }
//    public double getDeliveryTime() {
//        return this.bid.getDeliveryTime();
//    }
}
