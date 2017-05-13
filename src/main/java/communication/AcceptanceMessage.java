package communication;

import cnet.Auction;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AcceptanceMessage extends AuctionMessage {

    private boolean accepted;

    public AcceptanceMessage(Auction auction, boolean accepted) {
        super(auction);
        this.accepted = accepted;
        this.setType(MessageType.PARCEL_ACCEPTANCE);
    }

    public boolean isAccepted() {
        return accepted;
    }
}
