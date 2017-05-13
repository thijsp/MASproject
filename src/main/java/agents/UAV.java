package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import com.github.rinde.rinsim.core.model.comm.*;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModels;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import communication.AcceptBidMessage;
import communication.MessageContent;
import communication.MessageType;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;

public class UAV extends Vehicle implements CommUser {
    private static final double SPEED = 3000.0D;
    private static final double RANGE = 3.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private RandomGenerator rnd;
    private Optional<DroneParcel> parcel;
    private DistributionCenter depot;
    private Optional<CommDevice> commDevice;
    private State state = State.IDLE;

    public UAV(RandomGenerator rnd, DistributionCenter depot) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(SPEED).build());
        this.rnd = rnd;
        this.depot = depot;
        this.commDevice = Optional.absent();

    }

    public DistributionCenter getDepot(){
        return this.depot;
    }

    private void setState(State state) {
        this.state = state;
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.parcel = Optional.absent();
    }

    protected void tickImpl(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();

        Point depotPosition = rm.getPosition(this.depot);
        Point pos = rm.getPosition(this);

        // if a parcel is present, go and deliver it
        if (this.state.equals(State.DELIVERING)) {
            this.deliverParcel(rm, pm, time);
        }

        // if no parcel is present, figure out what to do
        else {
            this.doNextMove(rm, pm, time);
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
        DroneParcel parcel = this.depot.getRandomParcel();
        pm.pickup(this, parcel, time);
        assert rm.containsObject(parcel);
        this.parcel = Optional.of(parcel);
        this.setState(State.DELIVERING);
    }

    private void doNextMove(RoadModel rm, PDPModel pm, TimeLapse time) {
        Point pos = rm.getPosition(this);
        Point depotPos = rm.getPosition(this.getDepot());

        if (pos.equals(depotPos)) {
            this.pickupParcel(rm, pm, time);
        }
        else if (this.state.equals(State.IDLE)) {
            this.checkMessages(rm, pm, time);
        }
        else if (this.state.equals(State.PICKING)) {
            rm.moveTo(this, depotPos, time);
        }

    }

    private void checkMessages(RoadModel rm, PDPModel pm, TimeLapse time) {
        CommDevice device = this.commDevice.get();
        if (device.getUnreadCount() != 0) {
            ImmutableList<Message> messages = device.getUnreadMessages();
            // always take first received message first (??)
            Message message = messages.get(0);
            MessageContent content = (MessageContent)message.getContents();
            if (content.getType().equals(MessageType.NEW_PARCEL)) {
                this.handleNewParcel(rm, pm, time);
            }

        }
    }

    private void handleNewParcel(RoadModel rm, PDPModel pm, TimeLapse time) {
        if (this.commDevice.isPresent()) {
            CommDevice device = this.commDevice.get();
            device.send(new AcceptBidMessage(), this.getDepot());
            this.setState(State.PICKING);
            rm.moveTo(this, rm.getPosition(this.getDepot()), time);
        }
        else {
            throw new IllegalStateException("No commDevice present when accepting bid");
        }
    }
}

 enum State {

    IDLE, DELIVERING, PICKING
}
