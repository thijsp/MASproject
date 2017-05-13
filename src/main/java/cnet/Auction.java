package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.UAV;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class Auction {

    private DroneParcel parcel;
    private List<Bid> bids;
    private DistributionCenter moderator;

    public Auction(DroneParcel parcel, DistributionCenter moderator) {
        this.parcel = parcel;
        this.moderator = moderator;
        this.bids = new ArrayList<>();
    }

    public DroneParcel getParcel() {
        return this.parcel;
    }

    public DistributionCenter getModerator() {
        return this.moderator;
    }

    public List<UAV> getParticipants() {
        ArrayList<UAV> participants = new ArrayList<>();
        for (int i = 0; i < bids.size(); i++) {
            UAV participant = bids.get(i).getBidder();
            participants.add(participant);
        }
        return participants;
    }

    public void addBid(Bid bid) {
        this.bids.add(bid);
        bid.addAuction(this);
    }

    public Bid getBestBid() {
        Bid smallestBid = bids.get(0);
        for (int i = 1; i < this.bids.size(); i++) {
            if (this.bids.get(i).getBid() < smallestBid.getBid()) {
                smallestBid = this.bids.get(i);
            }
        }
        return smallestBid;

    }


}
