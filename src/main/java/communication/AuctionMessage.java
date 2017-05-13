package communication;

import agents.DroneParcel;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AuctionMessage extends MessageContent {

    private DroneParcel parcel;

    AuctionMessage(DroneParcel parcel) {
        this.parcel = parcel;
    }

    public DroneParcel parcel() {
        return this.parcel;
    }
}
