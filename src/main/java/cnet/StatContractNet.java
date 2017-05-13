package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.UAV;
import com.google.common.base.Optional;
import communication.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class StatContractNet extends ContractNet {


    public StatContractNet() {
        super();
    }

    public void addAuction(DroneParcel parcel, DistributionCenter moderator) {
        Auction auction = super.createAuction(parcel, moderator);
        super.sendAuctionMessage(moderator, auction);
    }

    public boolean placeBid(AuctionMessage content, UAV bidder) {
        Auction auction = content.getAuction();
        if (auction.isOpen()) {
            DroneParcel parcel = auction.getParcel();
            double delTime = bidder.calculateDeliveryTime(parcel.getDeliveryLocation());
            Bid bid = new Bid(bidder, delTime, auction);
            bidder.sendDirectMessage(new BidMessage(bid), auction.getModerator());
            return true;
        }
        return false;
    }

    public void moderateAuction(Auction auction, DistributionCenter moderator) {
        UAV winner = auction.getBestBid().getBidder();
        List<UAV> participants = auction.getParticipants();
        for (UAV participant : participants) {
            AuctionResultMessage message = new AuctionResultMessage(auction, participant.equals(winner) );
            moderator.sendDirectMessage(message, participant);
        }
    }

    /**
     *
     * @param auctions a list of all auctions that were accepted (this tick)
     * @return an optional including the auction that was chosen for the UAV
     *          or absent if no auction was chosen
     */
    public Optional<DroneParcel> selectAuction(List<Auction> auctions, UAV bidder) {
        for (Auction auction : auctions) {
            if (auction.isOpen()) {
                bidder.sendDirectMessage(new AcceptanceMessage(auction, true),auction.getModerator());
                return Optional.of(auction.getParcel());
            }
        }
        return Optional.absent();
    }

    public void handleUnactiveAuctions(List<Auction> unactiveAuctions) {
        for (Auction auction : unactiveAuctions) {
            DistributionCenter moderator = auction.getModerator();
            moderator.sendBroadcastMessage(new NewParcelMessage(auction));
        }
    }


}
