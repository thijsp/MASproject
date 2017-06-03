package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import agents.accessories.Battery;
import agents.accessories.Motor;
import cnet.*;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import communication.*;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import java.util.*;

public abstract class UAV extends Vehicle implements CommUser {
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private final double maxSpeed;

    private Optional<DroneParcel> parcel = Optional.absent();
    private Optional<CommDevice> commDevice = Optional.absent();
    protected DroneState state = DroneState.PICKING;
    // private DynContractNet cnet;
    private Motor motor;
    // private List<Auction> auctions;
    private LinkedList<Point> path = new LinkedList<>();
    protected Optional<Point> closestDepot = Optional.absent();

    public UAV(Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(speed).startPosition(startPos).build());
        // this.cnet = new DynContractNet();
        this.maxSpeed = maxSpeed;
        this.motor = new Motor(this, new Battery(batteryCapacity, this), motorPower);
    }

    public double getMaxSpeed() {
        return this.maxSpeed;
    }

//    private void setState(DroneState state) {
//        if (!this.satisfiesPreconditions(state)) {
//            throw new IllegalStateException("Cannot change to this state: " + state);
//        }
//        this.state = state;
//    }

//    private boolean satisfiesPreconditions(DroneState state) {
//        if (state.equals(DroneState.PICKING)) {
//            return (this.parcel.isPresent());
//        }
//        if (state.equals(DroneState.DELIVERING)) {
//            return this.parcel.isPresent();
//        }
//        if (state.equals(DroneState.IDLE)) {
//            return (!this.parcel.isPresent());
//        }
//        if (state.equals(DroneState.IN_AUCTION)) {
//            return (!this.parcel.isPresent());
//        }
//        if (state.equals(DroneState.NO_SERVICE)) {
//            return (!this.parcel.isPresent());
//        }
//        return true;
//    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) { }

    /**
     * Executes a single tick on this drone.
     * First handles all received messages, then acts according to its internal state.
     *
     * @param time The time lapse available this tick
     */
    protected void tickImpl(TimeLapse time) {
        if (!this.commDevice.isPresent())
            throw new IllegalStateException("No commdevice in UAV");

        this.handleMessages();

        switch (this.state) { // TODO
        case DELIVERING:
            this.deliverParcel(time);
            break;
        case PICKING:
            this.move(time);
            break;
        case OUT_OF_SERVICE:
            this.goToNearestDepot(time);
            break;
        case CHARGING:
            this.chargeBattery(time);
            break;
        }
    }

    private void handleMessages() {
        this.commDevice.get()
                .getUnreadMessages()
                .stream()
                .map(Message::getContents)
                .map(TypedMessage.class::cast)
                .forEach(this::onMessage);
    }

    private void onMessage(TypedMessage msg) {
        switch (msg.type) {
            case NEW_AUCTION:
                this.onNewAuction(((AuctionMessage) msg).getAuction());
                break;
            case AUCTION_WON:
                this.onAuctionWon(((BidMessage) msg).getBid());
                break;
            case AUCTION_LOST:
                this.onAuctionLost(((AuctionMessage) msg).getAuction());
                break;
            case AUCTION_DONE:
                this.onAuctionDone(((AuctionMessage) msg).getAuction());
                break;
            default:
                // ingnored
                System.err.println(String.format("Unknown message of type %s received: %s", msg.type, msg));
        }
    }

    protected abstract void move(TimeLapse time);
    protected abstract void onNewAuction(Auction auction);
    protected abstract void onAuctionWon(Bid bid);
    protected abstract void onAuctionLost(Auction auction);
    protected abstract void onAuctionDone(Auction auction);
    protected abstract void onPackageDelivered(DroneParcel parcel); // TODO?

    private void deliverParcel(TimeLapse time) {
        DroneParcel parcel = this.parcel.get();
        final Point destination = parcel.getDeliveryLocation();

        // Fly towards the delivery location
        this.fly(destination, time);

        // If the UAV has arrived at its destination: finalize delivery
        if (this.getRoadModel().getPosition(this).equals(destination)) {
            this.getPDPModel().deliver(this, parcel, time);
            this.parcel = Optional.absent();
            this.state = DroneState.PICKING;
            this.onPackageDelivered(parcel); // TODO?
        }
    }


    protected void pickParcel(TimeLapse time) {
        // TODO: participate in auctions, move around
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();
        Point pos = rm.getPosition(this);
        Point depotPos = this.parcel.get().getPickupLocation();
        if (pos.equals(depotPos)) {
            DistributionCenter parcelDepot = this.parcel.get().getDepot();
            //DroneParcel parcel = parcelDepot.getParcel(this.parcel.get());
            DroneParcel parcel = this.parcel.get();
            this.sendDirect(AuctionMessage.createAcceptance(new Auction(parcel, parcelDepot)), parcelDepot);
            pm.pickup(this, parcel, time);
            assert rm.containsObject(parcel);
            this.parcel = Optional.of(parcel);
            this.state = DroneState.DELIVERING;
        }
        else {
            this.fly(depotPos, time);
        }
    }

    private void fly(Point destLoc, TimeLapse time) {
        RoadModel rm = this.getRoadModel();

        double flyTime = time.getTickLength()/ (1000); // in sec
        if (!this.motor.canFlyTime(flyTime, this.getSpeed())) {
            // Not enough power - crash!
            System.err.println("Drone " + this + " has crashed!");
            this.state = DroneState.OUT_OF_SERVICE;
            return;
        }

        // Update the cached path if necessary
        if (path.isEmpty() || !path.peekLast().equals(destLoc)) {
            Optional<LinkedList<Point>> maybePath = this.computeShortestPath(destLoc);
            if (!maybePath.isPresent())
                return;
            this.path = maybePath.get();
        }

        // followPath also updates this.path
        MoveProgress progress = rm.followPath(this, this.path, time);
        // Drain the battery
        this.getMotor().drainPower(progress.time().doubleValue(SI.SECOND), this.getSpeed());
    }

//    private void dealWithAuctions() {
//        List<TypedMessage> messages = this.readMessageContents();
//        List<Auction> availableAuctions = this.getAvailableAuctions(messages);
//        List<Auction> biddedAuctions = this.getCnet().bidOnAvailableAuction(availableAuctions, this);
//        this.addAuctions(biddedAuctions);
//        List<AuctionResultMessage> resultMessages = this.getAuctionResults(messages);
//        if (!resultMessages.isEmpty() ) {
//            AuctionResult allAuctions = this.getCnet().defineAuctionResult(resultMessages);
//            List<Auction> lostAuctions = allAuctions.getLostAuctions();
//            List<Auction> wonAuctions = allAuctions.getWonAuctions();
//            this.deleteAuctions(lostAuctions);
//            this.deleteAuctions(wonAuctions);
//            if (!wonAuctions.isEmpty()) {
//                Optional<Auction> auction = this.getBestAuction(wonAuctions);
//                if (auction.isPresent()) {
//                    wonAuctions.remove(auction.get());
//                    this.getCnet().refuseAuctions(wonAuctions, this);
//                    this.getCnet().acceptAuction(auction.get(), this);
//                    this.parcel = Optional.of(auction.get().getParcel());
//                    this.setState(DroneState.PICKING);
//                } else {
//                    this.getCnet().refuseAuctions(wonAuctions, this);
//                    this.setState(DroneState.NO_SERVICE);
//                }
//            }
//        }
//        if (this.state.equals(DroneState.IN_AUCTION) & this.auctions.isEmpty() ) {
//            this.setState(DroneState.IDLE);
//        }
//    }

    private Optional<LinkedList<Point>> computeShortestPath(Point dest) {
        final RoadModel rm = this.getRoadModel();
        final Point pos = rm.getPosition(this);
        try {
            return Optional.of(new LinkedList<>(rm.getShortestPathTo(pos, dest)));
        } catch (PathNotFoundException exc) {
            System.err.println(String.format("No path found between %s and %s", pos, dest));
            return Optional.absent();
        }
    }

    protected void goToNearestDepot(TimeLapse time) {
        // Make sure we have a path to the closest depot
        if (!this.closestDepot.isPresent()) {
            this.closestDepot = Optional.of(this.getNearestDepot());
        }

        // Move toward the depot
        this.fly(this.closestDepot.get(), time);

        // If the UAV has arrived, start charging
        if (this.getPosition().equals(this.closestDepot)) {
            this.closestDepot = Optional.absent();
            this.state = DroneState.CHARGING;
            this.chargeBattery(time);
        }

    }

    private void chargeBattery(TimeLapse time) {
        // TODO: use time lapse in battery charging, participate in auctions (?)
        Battery battery = this.getMotor().getPowerSource();
        battery.charge();
        if (battery.isFull()) {
            this.state = DroneState.PICKING;
        }
    }

    public double calculateDeliveryTime(Point loc) {
        RoadModel rm = this.getRoadModel();
        List<Point> shortestPathTo = rm.getShortestPathTo(this, loc);
        Measure<Double, Length> distanceOfPath = rm.getDistanceOfPath(shortestPathTo); // length in km, speed in km/h
        double pathLength = distanceOfPath.getValue();
        double speed = this.getSpeed();
        return pathLength / speed;
    }

//    public List<Auction> getAvailableAuctions(List<TypedMessage> messages) {
//        return messages.stream()
//                .filter(msg -> msg.type == MessageType.NEW_AUCTION)
//                .map(msg -> ((AuctionMessage) msg).getAuction())
//                .filter(auction -> !this.auctions.contains(auction))
//                .collect(Collectors.toList());
//    }

//    public List<AuctionResultMessage> getAuctionResults(List<TypedMessage> messages) {
//        return messages.stream()
//                .filter(msg -> msg.getType() == MessageType.AUCTION_RESULT)
//                .map(AuctionResultMessage.class::cast)
//                .collect(Collectors.toList());
//    }

//    public List<TypedMessage> readMessageContents() {
//        CommDevice device = this.commDevice.get();
//        List<TypedMessage> contents = new ArrayList<>();
//        if (device.getUnreadCount() != 0) {
//            ImmutableList<Message> messages = device.getUnreadMessages();
//            contents = this.getCnet().getMessageContent(messages);
//        }
//        return contents;
//    }


    public void sendDirect(TypedMessage content, CommUser recipient) {
        CommDevice device = this.commDevice.get();
        device.send(content, recipient);
    }

    @Override
    public Optional<Point> getPosition() {
        return this.getRoadModel().containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(UAV.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(this.RELIABILITY).build());
    }

//    public DynContractNet getCnet() {
//        return this.cnet;
//    }

    protected Motor getMotor() { return this.motor; }

    public boolean wantsToBid(DroneParcel parcel) {
        RoadModel rm = this.getRoadModel();
        Point destLoc = parcel.getDeliveryLocation();
        Point pickupLoc = parcel.getPickupLocation();
        Optional<LinkedList<Point>> pathToPickup = this.computeShortestPath(pickupLoc);
        if (!pathToPickup.isPresent())
            return false;
        List<Point> fullPath = Lists.newArrayList(Iterables.concat(pathToPickup.get(), parcel.getShortestPath(), parcel.getShortestEscape()));
        double distance = rm.getDistanceOfPath(fullPath).getValue();
        return this.getMotor().canFlyDistance(distance, this.getSpeed());
    }

    protected Point getNearestDepot() {
        List<Point> shortestPath = DistributionCenter.getPathToClosest(this.getRoadModel(), this.getPosition().get());
        return shortestPath.get(shortestPath.size() - 1);
    }

    @Override
    public String toString() {
        Point pos = this.getPosition().get();
        return String.format("<UAV at (%.2f,%.2f) [Bat %.0f%%]>", pos.x, pos.y, 100*this.motor.getPowerSource().getBatteryLevel());
    }

//    private void addAuctions(List<Auction> auctions) {
//        this.auctions.addAll(auctions);
//    }

//    private void deleteAuctions(List<Auction> auctions) {
//        this.auctions.removeAll(auctions);
//    }

    /**
     * @pre wonAuctions is not empty
     * @param
     * @return
     */
//    private Optional<Auction> getBestAuction(List<Auction> wonAuctions) {
//        java.util.Optional<Auction> best = wonAuctions.stream()
//                .min(Comparator.comparingDouble(auction -> auction.getMyBid(this).getBid()))
//                .filter()
//        best.
//        // best.flatMap(auction -> this.wantsToBid(auction.getParcel()))
//        return (best.isPresent() && this.wantsToBid(best.get().getParcel()) ? Optional.of(best.get()) : Optional.absent();
//        Auction bestAuction = wonAuctions.get(0);
//        Bid myBestBid = bestAuction.getMyBid(this);
//        int i = 1;
//        while (i < wonAuctions.size()) {
//            Auction auction = wonAuctions.get(i);
//            Bid bid = auction.getMyBid(this);
//            if (bid.getDeliveryTime() < myBestBid.getDeliveryTime()) {
//                myBestBid = bid;
//                bestAuction = auction;
//            }
//            i++;
//        }
//        if (this.wantsToBid(bestAuction.getParcel())) {
//            return Optional.of(bestAuction);
//        } else {
//            return Optional.absent();
//        }
//    }

    public DroneState getState() {
        return state;
    }

    protected Optional<DroneParcel> getParcel() {return this.parcel; }

    protected void assignParcel(DroneParcel parcel) {
        this.parcel = Optional.of(parcel);
    }

    protected void removeParcel() {
        this.parcel = Optional.absent();
    }

}


