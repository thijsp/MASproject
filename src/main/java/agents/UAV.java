package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
import cnet.Bid;
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

import javax.swing.text.html.Option;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UAV extends Vehicle implements CommUser {
    private static final double SPEED = 3000.0D;
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private RandomGenerator rnd;

    private Optional<DroneParcel> parcel;
    //private DistributionCenter depot;
    private Optional<CommDevice> commDevice;
    private State state = State.IDLE;
    private ContractNet cnet;

    public UAV(RandomGenerator rnd) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(SPEED).build());
        this.rnd = rnd;
        //this.depot = depot;
        this.commDevice = Optional.absent();
        this.cnet = new StatContractNet();

    }

//    public DistributionCenter getDepot(){
//        return this.depot;
//    }

    private void setState(State state) {
        if (!this.satisfiesPreconditions(state)) {
            throw new IllegalStateException("Cannot change to this state: " + state);
        }
        this.state = state;
    }

    private boolean satisfiesPreconditions(State state) {
        if (state.equals(State.PICKING)) {
            return (this.parcel.isPresent());
        }
        if (state.equals(State.DELIVERING)) {
            return this.parcel.isPresent();
        }
        if (state.equals(State.IDLE)) {
            return (!this.parcel.isPresent());
        }
        if (state.equals(State.IN_AUCTION)) {
            return (!this.parcel.isPresent());
        }
        return true;
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.parcel = Optional.absent();
    }

    protected void tickImpl(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();

        // define the next move and take action
        this.doNextMove(rm, pm, time);
    }

    private void doNextMove(RoadModel rm, PDPModel pm, TimeLapse time) {
        Point pos = rm.getPosition(this);
        //Point depotPos = rm.getPosition(this.getDepot());

        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in UAV"); }

        else if(this.state.equals(State.DELIVERING)) {
            this.deliverParcel(rm, pm, time);
        }
        else if (this.state.equals(State.PICKING)) {
            this.pickupParcel(rm, pm, time);
        }
        else if (this.state.equals(State.IN_AUCTION)) {
            this.checkAuctionResult(rm, time);
        }
        else if (this.state.equals(State.IDLE)) {
            this.checkAvailableAuctions(rm, pm, time);
        }
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

    private void deliverParcel(RoadModel rm, PDPModel pm, TimeLapse time) {
        if (rm.getPosition(this).equals(this.parcel.get().getDeliveryLocation())) {
            pm.deliver(this, this.parcel.get(), time);
            this.parcel = Optional.absent();
            this.setState(State.IDLE);
        } else {
            rm.moveTo(this, this.parcel.get().getDeliveryLocation(), time);
        }
    }

    private void pickupParcel(RoadModel rm, PDPModel pm, TimeLapse time) {
        Point pos = rm.getPosition(this);
        //Point depotPos = rm.getPosition(this.getDepot());
        Point depotPos = this.parcel.get().getPickupLocation();
        if (pos.equals(depotPos)) {
            DistributionCenter parcelDepot = this.parcel.get().getDepot();
            DroneParcel parcel = parcelDepot.getParcel(this.parcel.get());
            pm.pickup(this, parcel, time);
            assert rm.containsObject(parcel);
            this.parcel = Optional.of(parcel);
            this.setState(State.DELIVERING);
        }
        else {
            rm.moveTo(this, depotPos, time);
        }
    }

    private void checkAvailableAuctions(RoadModel rm, PDPModel pm, TimeLapse time) {
        List<MessageContent> messages = this.readMessageContents();
        for (MessageContent content : messages) {
            if (content.getType().equals(MessageType.NEW_PARCEL)) {
                boolean bidPlaced = this.getCnet().placeBid(messages, this);
                if (bidPlaced) {
                    this.setState(State.IN_AUCTION);
                }
            }
        }
    }

    public void sendDirectMessage(MessageContent content, CommUser recipiant) {
        CommDevice device = this.commDevice.get();
        device.send(content, recipiant);
    }

    private void checkAuctionResult(RoadModel rm, TimeLapse time) {
        List<MessageContent> messages = this.readMessageContents();
        List<Auction> auctions = new ArrayList<>();
        for (MessageContent message : messages) {
            if (message.getType().equals(MessageType.AUCTION_RESULT)) {
                AuctionResultMessage content = (AuctionResultMessage) message;
                if (content.isAccepted()) {
                    auctions.add(content.getAuction());
                }
            }
        }
        Optional<DroneParcel> selectedParcel = this.getCnet().selectAuction(auctions, this);
        if(selectedParcel.isPresent()) {
            this.parcel = selectedParcel;
            this.setState(State.PICKING);
            rm.moveTo(this, selectedParcel.get().getPickupLocation(), time );
        }
        else if (!messages.isEmpty()) {
            this.setState(State.IDLE);
        }
    }

    public Double calculateDeliveryTime(Point loc) {
        //test: random delivery time
        return this.rnd.nextDouble();
    }

    private List<MessageContent> readMessageContents() {
        CommDevice device = this.commDevice.get();
        List<MessageContent> contents = new ArrayList<>();
        if (device.getUnreadCount() != 0) {
            ImmutableList<Message> messages = device.getUnreadMessages();
            contents = this.getCnet().getMessageContent(messages);
        }
        return contents;
    }

    public ContractNet getCnet() {
        return this.cnet;
    }
}

 enum State {

    IDLE, DELIVERING, PICKING, IN_AUCTION
}
