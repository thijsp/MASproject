package agents;

/**
 * Created by thijspeirelinck on 11/05/2017.
 */

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

public class UAV extends Vehicle {
    private static final double SPEED = 1000.0D;
    private static final int CAPACITY = 1;
    // private Optional<Parcel> curr = Optional.absent();
    private RandomGenerator rnd;
    private Optional<DroneParcel> parcel;
    private DistributionCenter depot;

    UAV(RandomGenerator rnd, DistributionCenter depot) {
        super(VehicleDTO.builder().capacity(CAPACITY).speed(SPEED).build());
        this.rnd = rnd;
        this.depot = depot;

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

}
