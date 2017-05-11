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

public class UAV extends Vehicle {
    private static final double SPEED = 1000.0D;
    private Optional<Parcel> curr = Optional.absent();

    UAV(Point startPosition, int capacity) {
        super(VehicleDTO.builder().capacity(capacity).startPosition(startPosition).speed(SPEED).build());
    }

    public void afterTick(TimeLapse timeLapse) {
    }

    protected void tickImpl(TimeLapse time) {
        RoadModel rm = this.getRoadModel();
        PDPModel pm = this.getPDPModel();
        if(time.hasTimeLeft()) {
            if(!this.curr.isPresent()) {
                this.curr = Optional.fromNullable(RoadModels.findClosestObject(rm.getPosition(this), rm, Parcel.class));
            }

            if(this.curr.isPresent()) {
                boolean inCargo = pm.containerContains(this, (Parcel)this.curr.get());
                if(!inCargo && !rm.containsObject((RoadUser)this.curr.get())) {
                    this.curr = Optional.absent();
                } else if(inCargo) {
                    rm.moveTo(this, ((Parcel)this.curr.get()).getDeliveryLocation(), time);
                    if(rm.getPosition(this).equals(((Parcel)this.curr.get()).getDeliveryLocation())) {
                        pm.deliver(this, (Parcel)this.curr.get(), time);
                    }
                } else {
                    rm.moveTo(this, (RoadUser)this.curr.get(), time);
                    if(rm.equalPosition(this, (RoadUser)this.curr.get())) {
                        pm.pickup(this, (Parcel)this.curr.get(), time);
                    }
                }
            }

        }
    }
}
