package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
import cnet.ContractNet;
import cnet.StatContractNet;
import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import communication.*;
import org.apache.commons.math3.random.RandomGenerator;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import java.util.ArrayList;
import java.util.List;

public class UAV extends Vehicle implements CommUser {
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private RandomGenerator rnd;

    private Optional<DroneParcel> parcel;
    private Optional<CommDevice> commDevice;
    private DroneState state = DroneState.IDLE;
    private ContractNet cnet;

    public UAV(RandomGenerator rnd, Double speed) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(speed).build());
        this.rnd = rnd;
        this.commDevice = Optional.absent();
        this.cnet = new StatContractNet();
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
        return true;
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.parcel = Optional.absent();
    }

    protected void tickImpl(TimeLapse time) {
        this.doNextMove(time);
    }

    private void doNextMove(TimeLapse time) {
        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in UAV"); }

        else if(this.state.equals(DroneState.DELIVERING)) {
            this.deliverParcel(time);
        }
        else if (this.state.equals(DroneState.PICKING)) {
            this.pickupParcel(time);
        }
        else if (this.state.equals(DroneState.IN_AUCTION)) {
            this.dealWithAuctions();
        }
        else if (this.state.equals(DroneState.IDLE)) {
            this.dealWithAuctions();
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
            rm.moveTo(this, this.parcel.get().getDeliveryLocation(), time);
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
            rm.moveTo(this, depotPos, time);
        }
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

    public void sendDirectMessage(TypedMessage content, CommUser recipiant) {
        CommDevice device = this.commDevice.get();
        device.send(content, recipiant);
    }

    public Double calculateDeliveryTime(Point loc) {
        RoadModel rm = this.getRoadModel();
        List<Point> shortestPathTo = rm.getShortestPathTo(this, loc);
        Measure<Double, Length> distanceOfPath = rm.getDistanceOfPath(shortestPathTo); // length in km, speed in km/h
        double pathLength = distanceOfPath.getValue();
        double speed = this.getSpeed();
        return pathLength / speed;
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

    public List<Auction> getAvailableAuctions(List<TypedMessage> messages) {
        List<Auction> availablaAuctions = new ArrayList<>();
        for (TypedMessage content : messages) {
            if (content.getType().equals(MessageType.NEW_PARCEL)) {
                Auction auction = ((AuctionMessage) content).getAuction();
                availablaAuctions.add(auction);
            }
        }
        return availablaAuctions;
    }

    public List<AuctionResultMessage> getAuctionResults(List<TypedMessage> messages) {
        List<AuctionResultMessage> auctionResultMessages = new ArrayList<>();
        for (TypedMessage content : messages) {
            if (content.getType().equals(MessageType.AUCTION_RESULT)) {
                auctionResultMessages.add((AuctionResultMessage) content);
            }
        }
        return auctionResultMessages;
    }

    @Override
    public Optional<Point> getPosition() {
        return ((RoadModel)this.getRoadModel()).containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(this.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(this.RELIABILITY).build());
    }

    public ContractNet getCnet() {
        return this.cnet;
    }

}


