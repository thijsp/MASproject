package cnet;

import agents.DroneParcel;
import agents.DroneState;
import agents.UAV;
import com.google.common.base.Optional;
import communication.AcceptanceMessage;
import communication.AuctionResultMessage;
import communication.BidMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krispeirelinck on 26/05/17.
 */
public class DynContractNet extends ContractNet {

    public DynContractNet() {
        super();
    }

    public boolean placeBid(Auction auction, UAV bidder) {
        DroneParcel parcel = auction.getParcel();
        double delTime = bidder.calculateDeliveryTime(parcel.getDeliveryLocation());
        Bid bid = new Bid(bidder, delTime, auction);
        bidder.sendDirectMessage(new BidMessage(bid), auction.getModerator());
        return true;
    }

    public List<Auction> bidOnAvailableAuction(List<Auction> auctions, UAV bidder) {
        List<Auction> biddedAuctions = new ArrayList<>();
        List<Auction> handledAuctions = new ArrayList<>();
        int i = 0;
        while (i < auctions.size()) {
            Auction auction = auctions.get(i);
            if (!handledAuctions.contains(auction)) {
                if (auction.isOpen()) {
                    if (bidder.wantsToBid(auction.getParcel())) {
                        biddedAuctions.add(auction);
                        this.placeBid(auction, bidder);
                    }
                }
                handledAuctions.add(auction);
            }
            i++;
        }
        return biddedAuctions;
    }

    public AuctionResult defineAuctionResult(List<AuctionResultMessage> results) {
        AuctionResult auctionResult = new AuctionResult();
        for (AuctionResultMessage message : results) {
            boolean lost = (!message.isAccepted());
            if (!lost) {
                auctionResult.addWonAuction(message.getAuction());
            }
            else {
                auctionResult.addLostAuction(message.getAuction());
            }
        }
        return auctionResult;
    }

    public void acceptAuction(Auction auction, UAV bidder) {
        bidder.sendDirectMessage(new AcceptanceMessage(true, auction.getMyBid(bidder)),auction.getModerator());
    }


    public void refuseAuctions(List<Auction> refusedAuctions, UAV bidder) {
        for (Auction auction : refusedAuctions) {
            bidder.sendDirectMessage(new AcceptanceMessage(false, auction.getMyBid(bidder)), auction.getModerator() );
        }
    }
}
