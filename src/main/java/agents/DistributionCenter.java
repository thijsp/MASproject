package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
import cnet.Bid;
import cnet.ContractNet;
import cnet.StatContractNet;
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
import com.google.common.collect.ImmutableList;
import communication.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;


public class DistributionCenter extends Depot implements CommUser, TickListener {

    private List<DroneParcel> availableParcels = new ArrayList<>();
    private Optional<CommDevice> commDevice = Optional.absent();
    private static final double RANGE = 2000.0D;
    private static final double RELIABILITY = 1.0D;
    private final HashMap<DroneParcel, AuctionState> auctions = new HashMap<>();
    private ContractNet cnet;
    private final double updateFreq = 10.0D;
    private double lastUpdated;

    public DistributionCenter(Point position, double capacity) {
        super(position);
        this.setCapacity(capacity);
        this.cnet = new StatContractNet();
        this.lastUpdated = 0.0;
    }

    @Override
    public void tick(TimeLapse _timeLapse) {
        // if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in depot"); }
        // this.checkMessages();
        this.handleMessages();

        // TODO: removeBidsFrom/replace this
        if (this.lastUpdated > this.updateFreq) {
            this.getCnet().handleUnactiveAuctions(this.getUnactiveAuctions());
            this.lastUpdated = 0.0;
        }
        this.lastUpdated += 1;
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) { }

    public void addParcel(DroneParcel parcel) {
        checkArgument(!this.auctions.containsKey(parcel), "Auction for parcel %s already exists in this DistributionCenter", parcel);

        this.availableParcels.add(parcel); // TODO removeBidsFrom

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
                this.onParcelAccepted(((AuctionMessage) msg).getAuction());
                break;
            default:
                // Unknown message received
                System.err.println(String.format("Unknown message of type %s received: %s", msg.type, msg));
        }
    }

    private void onNewAuction(Auction auction) {
        System.out.println(String.format("New auction at %s for %s", auction.getModerator(), auction.getParcel()));
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
        AuctionState state = this.auctions.get(bid.getParcel());
        if (state.assigneeEquals(bidder)) {
            System.err.println("New bid arrived from the winner of the auction - dropping previous assignment");
            state.assignee = Optional.absent();
        }

        // Remove all previous bids
        state.removeBidsFrom(bidder);

        // Add the new bid, and check if it wins the auction
        if (state.add(bid)) {
            // If there was a previous winner, inform them of their loss
            if (state.assignee.isPresent()) {
                TypedMessage lostMsg = AuctionMessage.createAuctionLost(bid.getAuction());
                sendDirect(lostMsg, state.assignee.get());
            }
            // Register the new winner and inform them
            state.assignee = Optional.of(bidder);
            TypedMessage wonMsg = BidMessage.createAuctionWon(bid);
            sendDirect(wonMsg, bidder);
        }
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
            System.err.println(String.format("Drone %s is lying to depot!", drone));
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
        } else {
            // No more bids available: restart auction
            this.onNewAuction(new Auction(bid.getParcel(), this));
        }
    }

    /**
     * A PARCEL_ACCEPTED message was received.
     * The parcel has been picked up, so the corresponding auction may now be destroyed.
     * @param auction The auction corresponding to the parcel that was picked up
     */
    private void onParcelAccepted(Auction auction) {
        // Message informing recipient that the auction is finished.
        final TypedMessage doneMsg = AuctionMessage.createAuctionDone(auction);
        // TODO: broadcast to all instead of direct messages?
        auctions.get(auction.getParcel()).bids.stream()
                .map(Bid::getBidder)
                .forEach(drone -> sendDirect(doneMsg, drone));

        this.auctions.remove(auction.getParcel());
    }

    private static class AuctionState {
        final PriorityQueue<Bid> bids = new PriorityQueue<>(Comparator.comparingDouble(Bid::getDeliveryTime));
        Optional<UAV> assignee = Optional.absent();

        boolean assigneeEquals(UAV drone) {
            return this.assignee.isPresent() && this.assignee.get().equals(drone);
        }

        void removeBidsFrom(UAV drone) {
            bids.removeIf(bid -> bid.getBidder().equals(drone));
        }

        /**
         * @param bid
         * @return whether this bid is the new optimal bid
         */
        boolean add(Bid bid) {
            bids.add(bid);
            return bids.peek().equals(bid);
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

    public DroneParcel getParcel(DroneParcel requestedParcel) {
        // TODO removeBidsFrom method
        for (DroneParcel parcel : this.availableParcels) {
            if (parcel.equals(requestedParcel)) {
                availableParcels.remove(parcel);
                return parcel;
            }
        }
        throw new IllegalArgumentException("no such parcel available");
    }

    @Override
    public Optional<Point> getPosition() {
        return this.getRoadModel().containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(DistributionCenter.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(this.RELIABILITY).build());
    }

    private CommDevice getCommDevice() {
        if (!this.commDevice.isPresent())
            throw new IllegalStateException("CommDevice not initialized in DistributionCenter");
        return this.commDevice.get();
    }

//    private List<Auction> getUnactiveAuctions() {
//        return this.auctions.stream().filter(auction -> !auction.hasBids()).collect(Collectors.toList());
//    }

//    private void checkMessages() {
//        List<TypedMessage> messages = this.readMessages();
//        List<Auction> auctions = new ArrayList<>();
//        for (int i = 0; i < messages.size(); i++) {
//            TypedMessage content = messages.get(i);
//            if (content.getType().equals(MessageType.NEW_BID)) {
//                BidMessage message = (BidMessage) content;
//                auctions.add(message.getAuction());
//            }
//            if (content.getType().equals(MessageType.PARCEL_ACCEPTANCE)) {
//                if (((AcceptanceMessage) content).isAccepted()) {
//                    Auction auction = ((AcceptanceMessage) content).getAuction();
//                    auction.close();
//                }
//                else {
//                    Auction auction = ((AcceptanceMessage) content).getAuction();
//                    Bid refusedBid = ((AcceptanceMessage) content).getBid();
//                    System.out.println(refusedBid);
//                    auction.deleteBid(refusedBid);
//                }
//            }
//        }
//        this.moderateAuction(auctions);
//    }

//    private void moderateAuction(List<Auction> auctions) {
//        List<Auction> handledAuctions = new ArrayList<>();
//        for (Auction auction : auctions) {
//            if (!handledAuctions.contains(auction)) {
//                this.getCnet().moderateAuction(auction, this);
//                handledAuctions.add(auction);
//            }
//        }
//    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

//    private List<TypedMessage> readMessages() {
//        CommDevice device = this.commDevice.get();
//        List<TypedMessage> contents = new ArrayList<>();
//        if (device.getUnreadCount() != 0) {
//            ImmutableList<Message> messages = device.getUnreadMessages();
//            contents = this.getCnet().getMessageContent(messages);
//        }
//        return contents;
//    }

//    public ContractNet getCnet() {
//        return this.cnet;
//    }

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
        return ""; // FIXME overlapping RoadUser tags
    }

}
