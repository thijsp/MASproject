package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
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
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import communication.*;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;


public class DistributionCenter extends Depot implements CommUser, TickListener {

    private List<DroneParcel> availableParcels = new ArrayList<>();
    private Optional<CommDevice> commDevice;
    private RandomGenerator rnd;
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private List<Auction> auctions;
    private ContractNet cnet;

    public DistributionCenter(Point position, double capacity, RandomGenerator rnd) {
        super(position);
        this.setCapacity(capacity);
        this.commDevice = Optional.absent();
        this.rnd = rnd;
        this.auctions = new ArrayList<>();
        this.cnet = new StatContractNet();
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    }

    public void addParcel(DroneParcel p) {
        this.availableParcels.add(p);
        this.getCnet().addAuction(p, this);
    }

    public void addAuction(Auction auction) {
        this.auctions.add(auction);
    }

    public void sendBroadcastMessage(MessageContent content) {
        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in the depot");}
        CommDevice device = this.commDevice.get();
        device.broadcast(content);
    }
    
    public void sendDirectMessage(MessageContent content, CommUser recipient) {
        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in the depot");}
        CommDevice device = this.commDevice.get();
        device.send(content, recipient);
    }

    public DroneParcel getParcel(DroneParcel requestedParcel) {
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
        return ((RoadModel)this.getRoadModel()).containsObject(this)? Optional.of(this.getRoadModel().getPosition(this)) : Optional.<Point>absent();
    }

    @Override
    public void setCommDevice(CommDeviceBuilder commDeviceBuilder) {
        commDeviceBuilder.setMaxRange(this.RANGE);
        this.commDevice = Optional.of(commDeviceBuilder.setReliability(this.RELIABILITY).build());
    }

    @Override
    public void tick(TimeLapse timeLapse) {
        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in depot"); }
        this.checkMessages();
    }

    private void checkMessages() {
        List<MessageContent> messages = this.readMessages();
        List<Auction> auctions = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageContent content = messages.get(i);
            if (content.getType().equals(MessageType.BID)) {
                BidMessage message = (BidMessage) messages.get(i);
                Auction auction = message.getAuction();
                auctions.add(message.getAuction());
            }
            if (content.getType().equals(MessageType.PARCEL_ACCEPTANCE)) {
            }
        }
        this.moderateAuction(auctions);
    }

    private void moderateAuction(List<Auction> auctions) {
        List<Auction> handledAuctions = new ArrayList<>();
        for (Auction auction : auctions) {
            if (!handledAuctions.contains(auction)) {
                this.getCnet().moderateAuction(auction, this);
                handledAuctions.add(auction);
            }
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}

    private List<MessageContent> readMessages() {
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
