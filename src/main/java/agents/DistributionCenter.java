package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
import cnet.Bid;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import communication.AuctionMessage;
import communication.BidMessage;
import communication.TypedMessage;

//import java.util.*;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;


public final class DistributionCenter extends Depot implements CommUser, TickListener {

    private final int id;
//    private List<DroneParcel> availableParcels = new ArrayList<>();
    private Optional<CommDevice> commDevice = Optional.absent();
    private static final double RANGE = 2000.0D;
    private static final double RELIABILITY = 1.0D;
    private final HashMap<DroneParcel, AuctionState> auctions = new HashMap<>();
    private final HashMap<DroneParcel, ArrayList<Bid>> receivedBids = new HashMap();
    private final double updateFreq = 10.0D;
    private double lastUpdated;

    public DistributionCenter(int id, Point position, double capacity) {
        super(position);
        this.id = id;
        this.setCapacity(capacity);
        this.lastUpdated = 0.0;
    }

    @Override
    public void tick(TimeLapse _timeLapse) {
        // if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in depot"); }
        this.handleMessages();
        this.activateAuctions();
    }

    public int getRemainingParcels() {
        return this.auctions.size();
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) { }

    public void addParcel(DroneParcel parcel) {
        checkArgument(!this.auctions.containsKey(parcel), "Auction for parcel %s already exists in this DistributionCenter", parcel);

//        this.availableParcels.add(parcel); // TODO remove

        this.auctions.put(parcel, new AuctionState());
        this.onNewAuction(new Auction(parcel, this));
    }


    private void handleMessages() {
        this.getCommDevice()
                .getUnreadMessages()
                .stream()
                .map(Message::getContents)
                .map(TypedMessage.class::cast)
                .forEach(this::onMessage);
        this.onMessagesHandled();
    }

    private void onMessage(TypedMessage msg) {
        switch (msg.type) {
            case NEW_BID:
                this.onNewBid(((BidMessage) msg).getBid());
                break;
            case PARCEL_REFUSED:
                this.onParcelRefused(((BidMessage) msg).getBid());
                break;
            case PARCEL_ACCEPTED:
                this.onParcelAccepted(((BidMessage) msg).getBid());
                break;
            default:
                // Unknown message received
        }
    }

    private void onMessagesHandled() {
        for (DroneParcel parcel : this.receivedBids.keySet()) {
            AuctionState state = this.auctions.get(parcel);
            List<Bid> newBids = this.receivedBids.get(parcel);

            state.addAll(newBids);

            Bid best = state.bestBid().get();
            if (newBids.contains(best)) {
                // New optimal bid
                if (state.assignee.isPresent()) {
                    // Inform the previous winner of the disaster
                    TypedMessage lostMsg = AuctionMessage.createAuctionLost(best.getAuction());
                    sendDirect(lostMsg, state.assignee.get());
                }

                // Inform the new winner of their victory
                UAV winner = best.getBidder();
                state.assignee = Optional.of(winner);
                TypedMessage wonMsg = BidMessage.createAuctionWon(best);
                sendDirect(wonMsg, winner);
            }
        }

        this.receivedBids.clear();
    }

    private void onNewAuction(Auction auction) {
        this.broadcast(AuctionMessage.createNewAuction(auction));
    }

    // TODO: this.auctions.get(parcel) might fail - implement checks

    /**
     * A NEW_BID message was received.
     * Registers the bid, and if it is superior to the previous best, update the auction winner.
     * @param bid the Bid included in the message.
     */
    private void onNewBid(Bid bid) {
        UAV bidder = bid.getBidder();
        DroneParcel parcel = bid.getParcel();
        AuctionState state = this.auctions.get(parcel);

        if (state.assigneeEquals(bidder)) {
            System.err.println("New bid arrived from the winner of the auction - dropping previous assignment " + bid.getParcel());
            state.assignee = Optional.absent();
        }

        // Remove all previous bids
        state.removeBidsFrom(bidder);

        receivedBids.putIfAbsent(parcel, new ArrayList<>());
        receivedBids.get(parcel).add(bid);
    }

    /**
     * A PARCEL_REFUSED message was received.
     * Will re-assign a new drone or restart the auction if necessary.
     * @param bid the refused Bid, included in the message.
     */
    private void onParcelRefused(Bid bid) {
        DroneParcel parcel = bid.getParcel();
        UAV drone = bid.getBidder();
        AuctionState state = this.auctions.get(parcel);

        if (!state.assigneeEquals(drone)) {
            System.err.println(String.format("Drone %s is refusing parcel %s, which it did not win", drone, parcel));
            return; // Auction was not won by this drone, ignore
        }

        // Remove the drone from the auction
        state.removeBidsFrom(drone);
        state.assignee = Optional.absent();

        // Try next best offer
        Optional<Bid> best = state.bestBid();
        if (best.isPresent()) {
            // Re-assign to next best drone
            UAV winner = best.get().getBidder();
            state.assignee = Optional.of(winner);
            this.sendDirect(BidMessage.createAuctionWon(best.get()), winner);
        }
    }

    /**
     * A PARCEL_ACCEPTED message was received.
     * The parcel has been picked up, so the corresponding auction may now be destroyed.
     * @param bid The bid corresponding to the parcel that was picked up and UAV that is picking it up
     */
    private void onParcelAccepted(Bid bid) {
        // Message informing recipient that the auction is finished.
        Auction auction = bid.getAuction();
        DroneParcel parcel = bid.getParcel();
        AuctionState state = this.auctions.get(parcel);
        if (!state.assigneeEquals(bid.getBidder())) {
            throw new IllegalStateException("Drone " + bid.getBidder() + " is lying to depot, he didn't win this auction, parcel: " + parcel);
        }
        final TypedMessage doneMsg = AuctionMessage.createAuctionDone(auction);
        auctions.get(auction.getParcel()).bids.stream()
                .map(Bid::getBidder)
                .forEach(drone -> sendDirect(doneMsg, drone));
        this.auctions.remove(auction.getParcel());
    }

    private static class AuctionState {
        final PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.comparingDouble(Bid::getDeliveryTime));
        Optional<UAV> assignee = Optional.absent();
        int unactive = 0;

        boolean assigneeEquals(UAV drone) {
            return this.assignee.isPresent() && this.assignee.get().equals(drone);
        }

        void removeBidsFrom(UAV drone) {
            bids.removeIf(bid -> bid.getBidder().equals(drone));
            this.unactive = 0;
        }

        void addAll(Collection<Bid> coll) {
            this.bids.addAll(coll);
        }

        boolean forgotten() {
            return !this.assignee.isPresent() && this.unactive > 10;
        }

        void timestep(){
            this.unactive++;
        }

        Optional<Bid> bestBid() {
            return Optional.fromNullable(this.bids.peek());
        }
    }


    public void broadcast(TypedMessage content) {
        this.getCommDevice().broadcast(content);
    }
    
    public void sendDirect(TypedMessage content, CommUser recipient) {
        this.getCommDevice().send(content, recipient);
    }

    @Override
    public Optional<Point> getPosition() {
        return this.getRoadModel().containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(DistributionCenter.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(DistributionCenter.RELIABILITY).build());
    }

    private CommDevice getCommDevice() {
        if (!this.commDevice.isPresent())
            throw new IllegalStateException("CommDevice not initialized in DistributionCenter");
        return this.commDevice.get();
    }

    public static List<Point> getPathToClosest(RoadModel rm, Point from) {
        return rm.getObjectsOfType(DistributionCenter.class).stream()
                .flatMap(depot -> {
                    try {
                        return Stream.of(rm.getShortestPathTo(from, depot.getPosition().get()));
                    } catch (PathNotFoundException exc) {
                        return Stream.<List<Point>>empty();
                    }
                })
                .min(Comparator.comparingDouble(path -> rm.getDistanceOfPath(path).getValue()))
                .get();
    }

    @Override
    public String toString() {
        // FIXME overlapping RoadUser tags
        // Point pos = this.getPosition().get();
        return String.format("<Depot %1d [%d]>", this.id, this.auctions.size()); // at (%.2f,%.2f) [%d active auctions]>", this.id, pos.x, pos.y, this.auctions.size());
    }

    private void activateAuctions() {
        Set<DroneParcel> parcels = this.auctions.keySet();
        List<DroneParcel> changedParcels = new ArrayList<>();
        for (DroneParcel parcel : parcels) {
            AuctionState state = this.auctions.get(parcel);
            if (state.forgotten()) {
                final TypedMessage doneMsg = AuctionMessage.createAuctionDone(new Auction(parcel, this));
                state.bids.stream()
                        .map(Bid::getBidder)
                        .forEach(drone -> sendDirect(doneMsg, drone));
                changedParcels.add(parcel);
                this.onNewAuction(new Auction(parcel, this));
            }
        }
        for (DroneParcel parcel : changedParcels) {
            this.auctions.replace(parcel, new AuctionState());
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        if (timeLapse.getStartTime() % 1000000 == 0) {
            this.auctions.values().forEach(AuctionState::timestep);
        }
    }

}
