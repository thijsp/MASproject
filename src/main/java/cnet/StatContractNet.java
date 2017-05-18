package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.DroneState;
import agents.UAV;
import communication.*;
import com.google.common.base.Optional;

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

    public boolean placeBid(Auction auction, UAV bidder) {
        DroneParcel parcel = auction.getParcel();
        double delTime = bidder.calculateDeliveryTime(parcel.getDeliveryLocation());
        Bid bid = new Bid(bidder, delTime, auction);
        bidder.sendDirectMessage(new BidMessage(bid), auction.getModerator());
        return true;
    }

    public void moderateAuction(Auction auction, DistributionCenter moderator) {
        Optional<Bid> winning_bid = auction.getBestBid();
        assert winning_bid.isPresent();

        UAV winner = winning_bid.get().getBidder();
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
    private Optional<DroneParcel> selectAuction(List<Auction> auctions, UAV bidder) {
        assert (auctions.size() == 1); // static contract net, UAV can only be involved in one auction
        for (Auction auction : auctions) {
            assert (auction.isOpen()); // in the case of static contract net, this should always be true
            bidder.sendDirectMessage(new AcceptanceMessage(auction, true),auction.getModerator());
            return Optional.of(auction.getParcel());
        }
        return Optional.absent();
    }

    public void handleUnactiveAuctions(List<Auction> unactiveAuctions) {
        for (Auction auction : unactiveAuctions) {
            DistributionCenter moderator = auction.getModerator();
            moderator.sendBroadcastMessage(new NewParcelMessage(auction));
        }
    }

    public DroneState bidOnAvailableAuction(DroneState state, List<Auction> auctions, UAV bidder) {
        if (state.equals(DroneState.IDLE)) {
            DroneState proposedState = DroneState.IDLE;
            boolean placedBid = false;
            int i = 0;
            List<Auction> handledAuctions = new ArrayList<>();
            while (!placedBid & i < auctions.size()) {
                Auction auction = auctions.get(i);
                if (!handledAuctions.contains(auction)) {
                    if (auction.isOpen()) {
                        if (bidder.wantsToBid(auction.getParcel())) {
                            proposedState = DroneState.IN_AUCTION;
                            placedBid = this.placeBid(auction, bidder);
                        } else {
                            proposedState = DroneState.NO_SERVICE;
                        }
                    }
                    handledAuctions.add(auction);
                }
                i++;
            }
            return proposedState;
        }
        assert state.equals(DroneState.IN_AUCTION);
        return state;
    }

    public Optional<DroneParcel> defineAuctionResult(DroneState state, List<AuctionResultMessage> results, UAV bidder) {
        List<Auction> auctions = new ArrayList<>();
        if (state.equals(DroneState.IDLE) || results.isEmpty())
            return Optional.absent();

        if (state.equals(DroneState.IN_AUCTION)) {
            for (AuctionResultMessage message : results) {
                boolean lost = (!message.isAccepted());
                if (!lost) {
                    auctions.add(message.getAuction());
                }
                else {
                    return Optional.absent();
                }
            }
        }
        return this.selectAuction(auctions, bidder); // if an auction was won, return the parcel of this auction
    }


}
