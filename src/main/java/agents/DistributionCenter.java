package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.Depot;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.*;
import com.google.common.base.Optional;

import java.util.*;


public class DistributionCenter extends Depot implements CommUser {

    private Deque<DroneParcel> availableParcels = new LinkedList<>();
    private Optional<CommDevice> commDevice;
    private static final double RANGE = 6.0D;
    private static final double RELIABILITY = 1.0D;

    DistributionCenter(Point position, double capacity) {
        super(position);
        this.setCapacity(capacity);
        this.commDevice = Optional.absent();
    }

    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
    }

    public void addParcel(DroneParcel p) {
        this.availableParcels.add(p);
    }

    public DroneParcel getRandomParcel() {
        return availableParcels.pop();
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
}
