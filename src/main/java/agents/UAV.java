package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
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
import org.apache.commons.math3.random.RandomGenerator;

public class UAV extends Vehicle implements CommUser {
    private static final double SPEED = 3000.0D;
    private static final double RANGE = 3.0D;
    private static final double RELIABILITY = 1.0D;
    private static final int CAPACITY = 1;
    private RandomGenerator rnd;
    private Optional<DroneParcel> parcel;
    private DistributionCenter depot;
    private Optional<CommDevice> commDevice;

    UAV(RandomGenerator rnd, DistributionCenter depot) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(SPEED).build());
        this.rnd = rnd;
        this.depot = depot;
        this.commDevice = Optional.absent();

    }


    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        this.parcel = Optional.absent();
    }

    protected void tickImpl(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();

        Point depotPosition = rm.getPosition(this.depot);
        Point pos = rm.getPosition(this);

        if (this.parcel.isPresent()) {
            if (rm.getPosition(this).equals(this.parcel.get().getDeliveryLocation())) {
                pm.deliver(this, this.parcel.get(), time);
                this.parcel = Optional.absent();
            } else {
                rm.moveTo(this, this.parcel.get().getDeliveryLocation(), time);
            }
        } else {
            if (pos.equals(depotPosition)) {
                DroneParcel parcel = this.depot.getRandomParcel();
                pm.pickup(this, parcel, time);
                assert rm.containsObject(parcel);
                this.parcel = Optional.of(parcel);
            } else {
                rm.moveTo(this, depotPosition, time);
            }
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
}
