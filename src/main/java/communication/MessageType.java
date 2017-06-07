package communication;

/**
 * Tags for all different types of messages that can be sent, either from DistributionCenter to UAV or the other way
 * around.
 */

public enum MessageType {

    /**
     * DistributionCenter broadcasts the availability of a new parcel to all nearby UAVs.
     */
    NEW_AUCTION,
    /**
     * DistributionCenter informs a UAV that its bid won the auction.
     */
    AUCTION_WON,
    /**
     * DistributionCenter informs a UAV that its bid did not win the auction.
     * This message can be sent despite an earlier confirmation that the UAV won an auction, when a different UAV
     * sent a superior offer.
     */
    AUCTION_LOST,
    /**
     * DistributionCenter informs a UAV that an auction has been deleted (the package is being delivered).
     */
    AUCTION_DONE,

    /**
     * UAV bids on an auction. The bid contains the computed delivery time claimed by the UAV.
     */
    NEW_BID,
    /**
     * UAV informs the DistributionCenter that it unconditionally accepts the responsibility for delivering a parcel.
     * Note that this message may only be sent when the parcel is effectively being picked up.
     */
    PARCEL_ACCEPTED,
    /**
     * UAV informs the DistributionCenter that it no longer accepts the responsibility for delivering a parcel,
     * despite its earlier claims.
     * The DistributionCenter will then have to search for a fallback UAV to handle the parcel,
     * or restart the auction entirely.
     */
    PARCEL_REFUSED,

}
