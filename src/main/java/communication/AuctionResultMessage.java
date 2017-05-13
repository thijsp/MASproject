package communication;

import cnet.Auction;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AuctionResultMessage extends AuctionMessage {

    private boolean accepted;

    public AuctionResultMessage(Auction auction, Boolean accepted) {
        super(auction);
        this.setType(MessageType.AUCTION_RESULT);
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return this.accepted;
    }
}
