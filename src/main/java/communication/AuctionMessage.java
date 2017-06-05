package communication;

import cnet.Auction;
import cnet.Bid;
import com.github.rinde.rinsim.core.model.comm.Message;

/**
 * Common superclass for messages that are related to an auction.
 */
public final class AuctionMessage extends TypedMessage {

    private final Auction auction;

    private AuctionMessage(MessageType type, Auction auction) {
        super(type);
        this.auction = auction;
    }

    public static AuctionMessage createNewAuction(Auction auction) {
        return new AuctionMessage(MessageType.NEW_AUCTION, auction);
    }

    public static AuctionMessage createAuctionLost(Auction auction) {
        return new AuctionMessage(MessageType.AUCTION_LOST, auction);
    }

    public static AuctionMessage createAcceptance(Auction auction) {
        return new AuctionMessage(MessageType.PARCEL_ACCEPTED, auction);
    }

    public static AuctionMessage createAuctionDone(Auction auction) {
        return new AuctionMessage(MessageType.AUCTION_DONE, auction);
    }

    public final Auction getAuction() {
        return this.auction;
    }

    @Override
    String getDescription() {
        return "" + this.auction;
    }
}
