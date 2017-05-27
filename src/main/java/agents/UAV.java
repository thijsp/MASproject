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
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class UAV extends Vehicle implements CommUser {
    private static final double RANGE = 20.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private RandomGenerator rnd;
    private final double maxSpeed;

    private Optional<DroneParcel> parcel;
    private Optional<CommDevice> commDevice;
    private DroneState state = DroneState.IDLE;
    private DynContractNet cnet;
    private Motor motor;
    private List<Auction> auctions;

    public UAV(RandomGenerator rnd, Double speed, Double batteryCapacity, Double motorPower, Double maxSpeed) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(speed).build());
        this.rnd = rnd;
        this.commDevice = Optional.absent();
        this.cnet = new DynContractNet();
        this.maxSpeed = maxSpeed;
        this.motor = new Motor(this, new Battery(batteryCapacity, this), motorPower);
        this.auctions = new ArrayList<>();

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
            this.goCharge(time);
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
        double flyTime = time.getTickLength()/ (1000); // in sec
        if (this.motor.canFly(flyTime, this.getSpeed())) {
            this.getMotor().fly(flyTime, this.getSpeed());
            rm.moveTo(this, destLoc, time);
        }
        else {
            this.setState(DroneState.NO_SERVICE);
        }
    }

    private void dealWithAuctions() {
        List<TypedMessage> messages = this.readMessageContents();
        List<Auction> availableAuctions = this.getAvailableAuctions(messages);
        List<Auction> biddedAuctions = this.getCnet().bidOnAvailableAuction(availableAuctions, this);
        this.addAuctions(biddedAuctions);
        List<AuctionResultMessage> resultMessages = this.getAuctionResults(messages);
        if (!resultMessages.isEmpty() ) {
            AuctionResult allAuctions = this.getCnet().defineAuctionResult(resultMessages);
            List<Auction> lostAuctions = allAuctions.getLostAuctions();
            List<Auction> wonAuctions = allAuctions.getWonAuctions();
            this.deleteAuctions(lostAuctions);
            this.deleteAuctions(wonAuctions);
            if (!wonAuctions.isEmpty()) {
                Optional<Auction> auction = this.getBestAuction(wonAuctions);
                if (auction.isPresent()) {
                    wonAuctions.remove(auction.get());
                    this.getCnet().refuseAuctions(wonAuctions, this);
                    this.getCnet().acceptAuction(auction.get(), this);
                    this.parcel = Optional.of(auction.get().getParcel());
                    this.setState(DroneState.PICKING);
                } else {
                    this.getCnet().refuseAuctions(wonAuctions, this);
                    this.setState(DroneState.NO_SERVICE);
                }
            }
        }
        if (this.state.equals(DroneState.IN_AUCTION) & this.auctions.isEmpty() ) {
            this.setState(DroneState.IDLE);
        }
    }


    private void goCharge(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        Iterator<DistributionCenter> it = rm.getObjectsOfType(DistributionCenter.class).iterator();
        boolean atChargingStation = false;
        while (it.hasNext() & !atChargingStation) {
            DistributionCenter chargingStation = it.next();
            if (chargingStation.getPosition().get().equals(this.getPosition().get())) {
                atChargingStation = true;
            }
        }
        if (atChargingStation) {
            this.setState(DroneState.CHARGING);
        }
        else {
            List<Point> chargePath = this.getPathToNearestChargeStation(this.getPosition().get());
            Point chargeLoc = chargePath.get(chargePath.size() - 1);
            this.fly(chargeLoc, time);
        }
    }

    private void charge() {
        Battery battery = this.getMotor().getPowerSource();
        if (battery.getChargePercentage() < 1.0) {
            battery.charge();
        }
        else {
            if (this.parcel.isPresent()) {
                this.setState(DroneState.PICKING);
            }
            else if (!this.auctions.isEmpty()) {
                this.setState(DroneState.IN_AUCTION);
            }
            else {
                this.setState(DroneState.IDLE);
            }
        }
    }

    public Double calculateDeliveryTime(Point loc) {
        RoadModel rm = this.getRoadModel();
        List<Point> shortestPathTo = rm.getShortestPathTo(this, loc);
        Measure<Double, Length> distanceOfPath = rm.getDistanceOfPath(shortestPathTo); // length in km, speed in km/h
        double pathLength = distanceOfPath.getValue();
        double speed = this.getSpeed();
        return pathLength / speed;
    }

    public List<Auction> getAvailableAuctions(List<TypedMessage> messages) {
        List<Auction> availableAuctions = new ArrayList<>();
        for (TypedMessage content : messages) {
            if (content.getType().equals(MessageType.NEW_PARCEL)) {
                Auction auction = ((AuctionMessage) content).getAuction();
                availableAuctions.add(auction);
                if (!this.auctions.contains(auction)) {
                    availableAuctions.add(auction);
                }
            }
        }
        return availableAuctions;
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


    public void sendDirectMessage(TypedMessage content, CommUser recipiant) {
        CommDevice device = this.commDevice.get();
        device.send(content, recipiant);
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

    public DynContractNet getCnet() {
        return this.cnet;
    }

    public Motor getMotor() { return this.motor; }

    public boolean wantsToBid(DroneParcel parcel) {
        Point delLoc = parcel.getDeliveryLocation();
        Point pickupLoc = parcel.getPickupLocation();
        RoadModel rm = this.getRoadModel();
        List<Point> pathToPickup = rm.getShortestPathTo(this, pickupLoc);
        List<Point> pathToDelivery = rm.getShortestPathTo(pickupLoc, delLoc);
        List<Point> pathToDepot = this.getPathToNearestChargeStation(delLoc);
        List<Point> fullPath = new ArrayList<>();
        fullPath.addAll(pathToPickup);
        fullPath.addAll(pathToDelivery);
        fullPath.addAll(pathToDepot);
        Double distance = rm.getDistanceOfPath(fullPath).getValue();
        return this.getMotor().possibleJourney(distance, this.getSpeed());
    }

    public List<Point> getPathToNearestChargeStation(Point loc) {
        RoadModel rm = this.getRoadModel();
        Iterator<DistributionCenter> it = rm.getObjectsOfType(DistributionCenter.class).iterator();
        DistributionCenter depot = it.next();
        Point depotPos = depot.getPosition().get();
        List<Point> shortestPath = rm.getShortestPathTo(loc, depotPos);
        Double distance = rm.getDistanceOfPath(shortestPath).getValue();
        while (it.hasNext()) {
            DistributionCenter nextDepot = it.next();
            Point nextDepotPos = nextDepot.getPosition().get();
            List<Point> nextShortesPath = rm.getShortestPathTo(loc, nextDepotPos);
            Double nextDistance = rm.getDistanceOfPath(nextShortesPath).getValue();
            if (nextDistance < distance) {
                distance = nextDistance;
                shortestPath = nextShortesPath;
            }
        }
        return shortestPath;
    }

    private void addAuctions(List<Auction> auctions) {
        this.auctions.addAll(auctions);
    }

    private void deleteAuctions(List<Auction> auctions) {
        this.auctions.removeAll(auctions);
    }

    /**
     * @pre wonAuctions is not empty
     * @param wonAuctions
     * @return
     */
    private Optional<Auction> getBestAuction(List<Auction> wonAuctions) {
        Auction bestAuction = wonAuctions.get(0);
        Bid myBestBid = bestAuction.getMyBid(this);
        int i = 1;
        while (i < wonAuctions.size()) {
            Auction auction = wonAuctions.get(i);
            Bid bid = auction.getMyBid(this);
            if (bid.getBid() < myBestBid.getBid()) {
                myBestBid = bid;
                bestAuction = auction;
            }
            i++;
        }
        if (this.wantsToBid(bestAuction.getParcel())) {
            return Optional.of(bestAuction);
        } else {
            return Optional.absent();
        }
    }

    public DroneState getState() {
        return state;
    }
}


