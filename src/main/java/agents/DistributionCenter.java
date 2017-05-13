package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import cnet.Auction;
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

    private Deque<DroneParcel> availableParcels = new LinkedList<>();
    private Optional<CommDevice> commDevice;
    private RandomGenerator rnd;
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private List<Auction> auctions;

    public DistributionCenter(Point position, double capacity, RandomGenerator rnd) {
        super(position);
        this.setCapacity(capacity);
        this.commDevice = Optional.absent();
        this.rnd = rnd;
        this.auctions = new ArrayList<>();
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    }

    public void addParcel(DroneParcel p) {
        if (this.commDevice.isPresent()) {
            this.availableParcels.add(p);
            CommDevice device = this.commDevice.get();
            Auction parcelAuction = new Auction(p, this);
            this.auctions.add(parcelAuction);
            device.broadcast(new NewParcelMessage(parcelAuction));
        }
        else {
            throw new IllegalStateException("No commDevice configured in the depot");
        }
    }

    public DroneParcel getRandomParcel() {
        if (!availableParcels.isEmpty()) {
            return availableParcels.pop();
        }
        else {
            throw new IllegalStateException("UAV asked for non-existing parcel");
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

    @Override
    public void tick(TimeLapse timeLapse) {
        if (!this.commDevice.isPresent()) {throw new IllegalStateException("No commdevice in depot"); }
        this.checkMessages();
    }

    private void checkMessages() {
        CommDevice device = this.commDevice.get();
        if (device.getUnreadCount() != 0) {
            ImmutableList<Message> messages = device.getUnreadMessages();
            // always take first received message first (??)
            Message message = messages.get(0);
            MessageContent content = (MessageContent)message.getContents();

            if (content.getType().equals(MessageType.BID)) {
                Auction auction = ((BidMessage)content).getAuction();
                this.moderateAuction(auction);
            }
        }
    }

    private void moderateAuction(Auction auction) {
        CommDevice device = this.commDevice.get();
        UAV winner = auction.getBestBid().getBidder();
        List<UAV> participants = auction.getParticipants();
        for (UAV participant : participants) {
            device.send(new AuctionResultMessage(auction, participant.equals(winner)), participant);
        }
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {}
}
