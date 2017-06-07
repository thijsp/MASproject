package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import agents.accessories.Battery;
import agents.accessories.Motor;
import cnet.Auction;
import cnet.Bid;
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

import javax.measure.unit.SI;

import java.util.*;

public abstract class UAV extends Vehicle implements CommUser {
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private final double maxSpeed;

    private final int id;
    Optional<DroneParcel> parcel = Optional.absent();
    private Optional<CommDevice> commDevice = Optional.absent();
    protected DroneState state = DroneState.PICKING;
    private Motor motor;
    private LinkedList<Point> path = new LinkedList<>();
    protected Optional<Point> closestDepot = Optional.absent();

    UAV(int id, Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(speed).startPosition(startPos).build());
        this.id = id;
        this.maxSpeed = maxSpeed;
        this.motor = new Motor(this, new Battery(batteryCapacity, this), motorPower);
    }

    public double getMaxSpeed() {
        return this.maxSpeed;
    }

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
    protected abstract void onFullyCharged();

    private void deliverParcel(TimeLapse time) {
        DroneParcel parcel = this.parcel.get();
        final Point destination = parcel.getDeliveryLocation();

        // Fly towards the delivery location
        this.fly(destination, time);

        // If the UAV has arrived at its destination: finalize delivery
        if (this.getRoadModel().getPosition(this).equals(destination)) {
            this.getPDPModel().deliver(this, parcel, time);
            parcel.setDeliveredTime(time.getTime());
            this.parcel = Optional.absent();
            this.state = DroneState.PICKING;
            this.closestDepot = Optional.absent();  // force re-initialization of closest depot calculation
            this.onPackageDelivered(parcel); // TODO?
        }
    }

    protected void pickParcel(TimeLapse time) {
        // TODO: participate in auctions, move around
        RoadModel rm = this.getRoadModel();
        Point pos = rm.getPosition(this);
        Point depotPos = this.parcel.get().getPickupLocation();
        this.fly(depotPos, time);
        if (pos.equals(depotPos)) {
            DistributionCenter parcelDepot = this.parcel.get().getDepot();
            DroneParcel currentParcel = this.parcel.get();
            Bid dummyBid = new Bid(this, 0.0, new Auction(currentParcel, parcelDepot));
            this.sendDirect(BidMessage.createBidRetrieval(dummyBid), parcelDepot);
            this.getPDPModel().pickup(this, currentParcel, time);
            currentParcel.setPickupTime(time.getTime());
            assert rm.containsObject(currentParcel);
            this.state = DroneState.DELIVERING;
            this.closestDepot = Optional.absent();
        }
    }

    void fly(Point destLoc, TimeLapse time) {
        RoadModel rm = this.getRoadModel();

        double flyTime = time.getTickLength() / 1000; // in sec
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
        if (!this.closestDepot.isPresent())
            this.closestDepot = Optional.of(this.getNearestDepot());

        // Move toward the depot
        this.fly(this.closestDepot.get(), time);

        // If the UAV has arrived, start charging
        if (this.getPosition().equals(this.closestDepot)) {
            this.closestDepot = Optional.absent();
            this.state = DroneState.CHARGING;
            this.chargeBattery(time);
        }

    }

    void chargeBattery(TimeLapse time) {
        // TODO: use time lapse in battery charging, participate in auctions (?)
        Battery battery = this.getMotor().getPowerSource();
        battery.chargeFor(time);
        if (battery.isFull())
            this.onFullyCharged();
    }

    public double calculateDeliveryTime(DroneParcel parcel) {
        Point pickupLoc = parcel.getPickupLocation();
        LinkedList<Point> pathToPickup = this.computeShortestPath(pickupLoc).get();
        List<Point> deliveryPath = Lists.newArrayList(Iterables.concat(pathToPickup, parcel.getShortestPath()));
        double pathLength = this.getRoadModel().getDistanceOfPath(deliveryPath).getValue();
        return pathLength / this.getSpeed();
    }

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
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(UAV.RELIABILITY).build());
    }

//    public DynContractNet getCnet() {
//        return this.cnet;
//    }

    protected Motor getMotor() { return this.motor; }

    Optional<List<Point>> fullDeliveryPath(DroneParcel parcel) {
        Point pickupLoc = parcel.getPickupLocation();
        Optional<LinkedList<Point>> pathToPickup = this.computeShortestPath(pickupLoc);
        if (!pathToPickup.isPresent())
            return Optional.absent();
        return Optional.of(Lists.newArrayList(Iterables.concat(pathToPickup.get(), parcel.getShortestPath(), parcel.getShortestEscape())));
    }

    public boolean wantsToBid(DroneParcel parcel) {
        Optional<List<Point>> fullPath = fullDeliveryPath(parcel);
        if (!fullPath.isPresent())
            return false;
        double distance = this.getRoadModel().getDistanceOfPath(fullPath.get()).getValue();
        return this.getMotor().canFlyDistance(distance, this.getSpeed());
    }

    protected Point getNearestDepot() {
        List<Point> shortestPath = DistributionCenter.getPathToClosest(this.getRoadModel(), this.getPosition().get());
        return shortestPath.get(shortestPath.size() - 1);
    }

    @Override
    public String toString() {
        Point pos = this.getPosition().get();
        return String.format("<UAV %2d at (%.2f,%.2f) [Bat%3d%%]>", this.id, pos.x, pos.y, (int) (100*this.motor.getPowerSource().getBatteryLevel()));
    }

    protected Optional<DroneParcel> getParcel() {
        return this.parcel;
    }

    protected void assignParcel(DroneParcel parcel) {
        this.closestDepot = Optional.absent();
        this.parcel = Optional.of(parcel);
    }

    protected void removeParcel() {
        this.parcel = Optional.absent();
    }

}


