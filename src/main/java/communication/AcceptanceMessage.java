package communication;

import agents.UAV;
import cnet.Auction;
import cnet.Bid;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AcceptanceMessage extends BidMessage {

    private boolean accepted;

    public AcceptanceMessage(boolean accepted, Bid bid) {
        super(bid);
        this.accepted = accepted;
        this.setType(MessageType.PARCEL_ACCEPTANCE);
    }

    public boolean isAccepted() {
        return accepted;
    }
}
