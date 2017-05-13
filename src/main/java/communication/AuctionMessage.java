package communication;

import cnet.Auction;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AuctionMessage extends MessageContent {

    private Auction auction;

    AuctionMessage(Auction auction) {
        this.auction = auction;
    }

    public Auction getAuction() {
        return this.auction;
    }

    public boolean AuctionStillActive() {
        return this.getAuction().isOpen();
    }
}
