package communication;

import agents.DroneParcel;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class AcceptParcelMessage extends AuctionMessage {

    private double deliveryTime;

    public AcceptParcelMessage(DroneParcel parcel, Double deliveryTime) {
        super(parcel);
        this.setType(MessageType.ACCEPTED);
        this.deliveryTime = deliveryTime;
    }

    public Double getDeliveryTime() {
        return this.deliveryTime;
    }
}
