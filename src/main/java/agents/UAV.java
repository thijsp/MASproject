package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
import cnet.Bid;
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

    public UAV(RandomGenerator rnd) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(SPEED).build());
        this.rnd = rnd;
        //this.depot = depot;
        this.commDevice = Optional.absent();

    }

//    public DistributionCenter getDepot(){
//        return this.depot;
//    }

    private void setState(State state) {
        if (!this.satisfiesPreconditions(state)) {
            throw new IllegalStateException("Cannot change to this state");
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
            this.checkMessages(rm, pm, time);
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

    private void checkMessages(RoadModel rm, PDPModel pm, TimeLapse time) {
        CommDevice device = this.commDevice.get();
        List<Message> messages = this.readMessagesOfType(MessageType.NEW_PARCEL);
        //for now: just read the first message (has to become a loop over messages)
        if(!messages.isEmpty()) {
            Message message = messages.get(0);
            MessageContent content = (MessageContent) message.getContents();
            NewParcelMessage parcelMessage = (NewParcelMessage) content;
            Auction auction = parcelMessage.getAuction();
            if (auction.isOpen()) {
                this.handleNewParcel(rm, pm, time, auction);
            }
        }
    }

    private void handleNewParcel(RoadModel rm, PDPModel pm, TimeLapse time, Auction auction) {
        CommDevice device = this.commDevice.get();
        DroneParcel parcel = auction.getParcel();
        Double deliveryTime = this.calculateDeliveryTime(parcel);
        Bid bid = new Bid(this, deliveryTime);
        auction.addBid(bid);
        device.send(new BidMessage(auction, deliveryTime), parcel.getDepot() );
        this.setState(State.IN_AUCTION);
    }

    private void checkAuctionResult(RoadModel rm, TimeLapse time) {
        CommDevice device = this.commDevice.get();
        List<Message> messages = this.readMessagesOfType(MessageType.AUCTION_RESULT);
        if (!messages.isEmpty()) {
            // temporary: this can only be one message
            assert messages.size() == 1;
            AuctionResultMessage content = (AuctionResultMessage) messages.get(0).getContents();
            Boolean result = content.isAccepted();
            if (result) {
                DistributionCenter depot = content.getAuction().getModerator();
                Point depotPos = rm.getPosition(depot);
                this.parcel = Optional.of(content.getAuction().getParcel());
                this.setState(State.PICKING);
                content.getAuction().close();
                rm.moveTo(this, depotPos, time);
            } else {
                this.setState(State.IDLE);
            }
        }
    }

    private Double calculateDeliveryTime(DroneParcel parcel) {
        //test: random delivery time
        return this.rnd.nextDouble();
    }

    private List<Message> readMessagesOfType(MessageType type) {
        CommDevice device = this.commDevice.get();
        ArrayList<Message> messagesOfType = new ArrayList<>();
        if (device.getUnreadCount() != 0) {
            ImmutableList<Message> messages = device.getUnreadMessages();
            Iterator<Message> mIt = messages.iterator();
            while (mIt.hasNext()) {
                Message message = mIt.next();
                MessageContent content = (MessageContent)message.getContents();
                if (content.getType().equals(type)) {
                    messagesOfType.add(message);
                }
            }
        }
        return messagesOfType;
    }
}

 enum State {

    IDLE, DELIVERING, PICKING, IN_AUCTION
}
