package communication;

import cnet.Auction;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

public class NewParcelMessage extends AuctionMessage {


    public NewParcelMessage(Auction auction) {
        super(auction);
        this.setType(MessageType.NEW_PARCEL);
    }


}
