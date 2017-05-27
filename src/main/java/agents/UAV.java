package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import agents.accessories.Battery;
import agents.accessories.Motor;
import cnet.Auction;
import cnet.ContractNet;
import cnet.StatContractNet;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.PathNotFoundException;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.ImmutableList;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import communication.*;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.SI;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.*;

public class UAV extends Vehicle implements CommUser {
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private final double maxSpeed;

    private Optional<DroneParcel> parcel;
    private Optional<CommDevice> commDevice = Optional.absent();
    private DroneState state = DroneState.IDLE;
    private ContractNet cnet;
    private Motor motor;
    private LinkedList<Point> path = new LinkedList<>();
    private Optional<Point> closestDepot = Optional.absent();

    public UAV(Point startPos, double speed, double batteryCapacity, double motorPower, double maxSpeed) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(speed).startPosition(startPos).build());
        this.cnet = new StatContractNet();
        this.maxSpeed = maxSpeed;
        this.motor = new Motor(this, new Battery(batteryCapacity), motorPower);

    }

    public double getMaxSpeed() {
        return this.maxSpeed;
    }

    private void setState(DroneState state) {
        if (!this.satisfiesPreconditions(state)) {
            throw new IllegalStateException("Cannot change to this state: " + state);
        }
        this.state = state;
    }

    private boolean satisfiesPreconditions(DroneState state) {
        if (state.equals(DroneState.PICKING)) {
            return (this.parcel.isPresent());
        }
        if (state.equals(DroneState.DELIVERING)) {
            return this.parcel.isPresent();
        }
        if (state.equals(DroneState.IDLE)) {
            return (!this.parcel.isPresent());
        }
        if (state.equals(DroneState.IN_AUCTION)) {
            return (!this.parcel.isPresent());
        }
        if (state.equals(DroneState.NO_SERVICE)) {
            return (!this.parcel.isPresent());
        }
        return true;
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.parcel = Optional.absent();
    }

    protected void tickImpl(TimeLapse time) {
        this.doNextMove(time);
    }

    private void doNextMove(TimeLapse time) {
        if (!this.commDevice.isPresent())
            throw new IllegalStateException("No commdevice in UAV");

        switch (this.state) {
        case DELIVERING:
            this.deliverParcel(time);
            break;
        case PICKING:
            this.pickupParcel(time);
            break;
        case IN_AUCTION:
        case IDLE:
            this.dealWithAuctions();
            break;
        case NO_SERVICE:
            this.goCharge(time);
            break;
        case CHARGING:
            this.charge();
            break;
        }
    }

    private void deliverParcel(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();
        if (rm.getPosition(this).equals(this.parcel.get().getDeliveryLocation())) {
            pm.deliver(this, this.parcel.get(), time);
            this.parcel = Optional.absent();
            this.setState(DroneState.IDLE);
        } else {
            Point destLoc = this.parcel.get().getDeliveryLocation();
            this.fly(destLoc, time);
        }
    }

    private void pickupParcel(TimeLapse time) {
        assert (this.state.equals(DroneState.PICKING));
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();
        Point pos = rm.getPosition(this);
        Point depotPos = this.parcel.get().getPickupLocation();
        if (pos.equals(depotPos)) {
            DistributionCenter parcelDepot = this.parcel.get().getDepot();
            DroneParcel parcel = parcelDepot.getParcel(this.parcel.get());
            pm.pickup(this, parcel, time);
            assert rm.containsObject(parcel);
            this.parcel = Optional.of(parcel);
            this.setState(DroneState.DELIVERING);
        }
        else {
            this.fly(depotPos, time);
        }
    }

    private void fly(Point destLoc, TimeLapse time) {
        RoadModel rm = this.getRoadModel();

        // Update the cached path if necessary
        if (path.isEmpty() || !path.peekLast().equals(destLoc)) {
            Optional<LinkedList<Point>> maybePath = this.computeShortestPath(destLoc);
            if (!maybePath.isPresent())
                return;
            this.path = maybePath.get();
        }

        // followPath also updates this.path
        MoveProgress progress = rm.followPath(this, this.path, time);
        this.getMotor().fly(progress.time().doubleValue(SI.SECOND), this.getSpeed());
    }

    private void dealWithAuctions() {
        List<TypedMessage> messages = this.readMessageContents();
        List<Auction> availableAuctions = this.getAvailableAuctions(messages);
        DroneState newState = this.getCnet().bidOnAvailableAuction(this.state, availableAuctions, this);
        List<AuctionResultMessage> resultMessages = this.getAuctionResults(messages);
        if (this.state.equals(DroneState.IN_AUCTION) & (!resultMessages.isEmpty()) ) {
            Optional<DroneParcel> newParcel = this.getCnet().defineAuctionResult(this.state, resultMessages, this);
            if (newParcel.isPresent()) {
                this.parcel = newParcel;
                this.setState(DroneState.PICKING);
            } else {
                this.setState(DroneState.IDLE);
            }
        }
        else {
            this.setState(newState);
        }
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

    private void goCharge(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        boolean atDepot = rm.getObjectsOfType(DistributionCenter.class).stream()
                .anyMatch(depot -> rm.equalPosition(depot, this));

        if (atDepot) {
            this.setState(DroneState.CHARGING);
            this.closestDepot = Optional.absent();
        } else {
            if (!this.closestDepot.isPresent())
                this.closestDepot = Optional.of(this.getNearestChargeStation());
            this.fly(this.closestDepot.get(), time);
        }
    }

    private void charge() {
        Battery battery = this.getMotor().getPowerSource();
        battery.charge();
        if (battery.isFull())
            this.setState(DroneState.IDLE);
    }

    public double calculateDeliveryTime(Point loc) {
        RoadModel rm = this.getRoadModel();
        List<Point> shortestPathTo = rm.getShortestPathTo(this, loc);
        Measure<Double, Length> distanceOfPath = rm.getDistanceOfPath(shortestPathTo); // length in km, speed in km/h
        double pathLength = distanceOfPath.getValue();
        double speed = this.getSpeed();
        return pathLength / speed;
    }

    public List<Auction> getAvailableAuctions(List<TypedMessage> messages) {
        return messages.stream()
                .filter(msg -> msg.getType() == MessageType.NEW_PARCEL)
                .map(msg -> ((AuctionMessage) msg).getAuction())
                .collect(Collectors.toList());
    }

    public List<AuctionResultMessage> getAuctionResults(List<TypedMessage> messages) {
        return messages.stream()
                .filter(msg -> msg.getType() == MessageType.AUCTION_RESULT)
                .map(msg -> (AuctionResultMessage) msg)
                .collect(Collectors.toList());
    }

    public List<TypedMessage> readMessageContents() {
        CommDevice device = this.commDevice.get();
        List<TypedMessage> contents = new ArrayList<>();
        if (device.getUnreadCount() != 0) {
            ImmutableList<Message> messages = device.getUnreadMessages();
            contents = this.getCnet().getMessageContent(messages);
        }
        return contents;
    }


    public void sendDirectMessage(TypedMessage content, CommUser recipient) {
        CommDevice device = this.commDevice.get();
        device.send(content, recipient);
    }

    @Override
    public Optional<Point> getPosition() {
        return ((RoadModel)this.getRoadModel()).containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(UAV.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(this.RELIABILITY).build());
    }

    public ContractNet getCnet() {
        return this.cnet;
    }

    public Motor getMotor() { return this.motor; }

    public boolean wantsToBid(DroneParcel parcel) {
        RoadModel rm = this.getRoadModel();
        Point destLoc = parcel.getDeliveryLocation();
        Point pickupLoc = parcel.getPickupLocation();
        Optional<LinkedList<Point>> pathToPickup = this.computeShortestPath(pickupLoc);
        if (!pathToPickup.isPresent())
            return false;
        List<Point> fullPath = Lists.newArrayList(Iterables.concat(pathToPickup.get(), parcel.getShortestPath(), parcel.getShortestEscape()));
        double distance = rm.getDistanceOfPath(fullPath).getValue();
        return this.getMotor().possibleJourney(distance, this.getSpeed());
    }

    public Point getNearestChargeStation() {
        List<Point> shortestPath = DistributionCenter.getPathToClosest(this.getRoadModel(), this.getPosition().get());
        return shortestPath.get(shortestPath.size() - 1);
    }

    @Override
    public String toString() {
        Point pos = this.getPosition().get();
        return String.format("<UAV at (%.2f,%.2f) [Bat %.0f%%]>", pos.x, pos.y, 100*this.motor.getPowerSource().getChargePercentage());
    }
}


