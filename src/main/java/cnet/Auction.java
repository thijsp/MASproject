package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.UAV;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Comparator;
import java.util.List;
import com.google.common.base.Optional;
import java.util.stream.Collectors;

/**
 * Contains the data required to hold an auction:
 * - the parcel that is being auctioned,
 * - the DistributionCenter that moderates the auction.
 *
 * Because this class is used as an identifier and is sent around in messages, it does not contain any stateful
 * information on the auction (e.g. registered bids)
 */
public final class Auction {

    private final DroneParcel parcel;
    // private List<Bid> bids;
    private final DistributionCenter moderator;
    // private boolean open;

    public Auction(DroneParcel parcel, DistributionCenter moderator) {
        this.parcel = parcel;
        this.moderator = moderator;
        // this.bids = new ArrayList<>();
        // this.open = true;
        // parcel.setAuction(this);
    }

    public DroneParcel getParcel() {
        return this.parcel;
    }

    public DistributionCenter getModerator() {
        return this.moderator;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Auction))
            return false;
        Auction other = (Auction) obj;
        return this.parcel.equals(other.getParcel()) && this.moderator.equals(other.getModerator());
    }

    public String toString() {
        return String.format("<Auction for %s at %s>", this.parcel, this.moderator);
    }
}
