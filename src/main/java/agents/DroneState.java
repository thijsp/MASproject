package agents;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public enum DroneState {

    /**
     * UAV is carrying a package to its destination.
     */
    DELIVERING,
    /**
     * UAV is not currently carrying a package, and is interested in picking up a package.
     * It may concurrently take part in multiple auctions in this state.
     */
    PICKING,
    /**
     * UAV is unable to deliver any packages at this time because it needs to recharge.
     */
    OUT_OF_SERVICE,
    /**
     * UAV is at a depot, recharging its battery.
     * It may concurrently take part in multiple auctions, however, it needs to take into account the remaining
     * charging time before it can deliver a package
     */
    CHARGING,

    //IDLE, DELIVERING, PICKING, IN_AUCTION, CHARGING, NO_SERVICE
}
