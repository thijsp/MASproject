package communication;

import agents.DroneParcel;
import com.github.rinde.rinsim.core.model.comm.MessageContents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

public class NewParcelMessage extends AuctionMessage {

    private DroneParcel parcel;

    public NewParcelMessage(DroneParcel parcel) {
        super(parcel);
        this.setType(MessageType.NEW_PARCEL);
    }


}
