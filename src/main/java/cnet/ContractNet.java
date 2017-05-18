package cnet;

import agents.DistributionCenter;
import agents.DroneParcel;
import agents.DroneState;
import agents.UAV;
import com.github.rinde.rinsim.core.model.comm.Message;
import communication.AuctionMessage;
import communication.AuctionResultMessage;
import communication.TypedMessage;
import communication.NewParcelMessage;
import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by thijspeirelinck on 13/05/2017.
 */
public class ContractNet {

    private List<UAV> drones;
    private List<DistributionCenter> depots;
    private List<Auction> auctions;

    ContractNet() {
        this.drones = new ArrayList<>();
        this.depots = new ArrayList<>();
        this.auctions = new ArrayList<>();
    }

    public void addDepot(DistributionCenter depot) {
        this.depots.add(depot);
    }

    public void addDrone(UAV uav) {
        this.drones.add(uav);
    }

    public void addAuction(Auction auction) {
        this.auctions.add(auction);
    }

    public void addAuction(DroneParcel parcel, DistributionCenter moderator) {}

    private Optional<DroneParcel> selectAuction(List<Auction> auctions, UAV bidder) { return Optional.absent(); }

    public Optional<DroneParcel> defineAuctionResult(DroneState state, List<AuctionResultMessage> results, UAV bidder) {return Optional.absent();}

    public Auction createAuction(DroneParcel parcel, DistributionCenter moderator) {
        Auction auction = new Auction(parcel, moderator);
        moderator.addAuction(auction);
        return auction;
    }

    public void sendAuctionMessage(DistributionCenter moderator, Auction auction) {
        TypedMessage content = new NewParcelMessage(auction);
        moderator.sendBroadcastMessage(content);
    }

    public DroneState bidOnAvailableAuction(DroneState state, List<Auction> auctions, UAV bidder) {return state;}


    public List<DistributionCenter> getDepots() {
        return depots;
    }

    public List<UAV> getDrones() {
        return drones;
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public void moderateAuction(Auction auction, DistributionCenter moderator) {}

    public boolean placeBid(AuctionMessage content, UAV bidder) {
        return false;
    }

    public List<TypedMessage> getMessageContent(List<Message> messages) {
        Iterator<Message> mIt = messages.iterator();
        List<TypedMessage> contents = new ArrayList<>();
        while (mIt.hasNext()) {
            Message message = mIt.next();
            TypedMessage content = (TypedMessage)message.getContents();
            contents.add(content);
        }
        return contents;
    }

    public void handleUnactiveAuctions(List<Auction> unactiveAuctions) {}
}
