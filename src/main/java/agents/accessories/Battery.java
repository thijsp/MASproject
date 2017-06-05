package agents.accessories;

import agents.UAV;
import agents.constraints.BatteryConstraint;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Battery extends Accessory {

    public final double capacity; // in Ws
    private double charge; // in Ws
    private BatteryConstraint constraint;
    private final double chargingPower; // in Ws

    public Battery(double capacity, UAV user) {
        super(user);
        this.capacity = capacity;
        this.charge = capacity;
        this.constraint = new BatteryConstraint(this);
        this.chargingPower = capacity/5000;
    }

    public double getCapacity() {
        return capacity;
    }


    private void setCharge(double newCharge) {
        this.charge = newCharge;
    }

    public BatteryConstraint getConstraint() {
        return this.constraint;
    }

    public double getBatteryLevel() {
        return this.charge / this.capacity;
    }

    public void discharge(double consumedEnergy) {
        if (!this.enoughChargeFor(consumedEnergy)) {
            throw new IllegalStateException("Battery is used up until negative charge");
        }
        this.charge -= consumedEnergy;
    }

    public boolean enoughChargeFor(double plannedConsumption) {
        double newCharge = this.charge - plannedConsumption;
        return this.getConstraint().isAllowedCharge(newCharge);
    }

    public void chargeFor(TimeLapse time) {
        // TODO use available time
        this.charge = Math.min(this.capacity, this.charge + this.chargingPower);
    }

    public boolean isFull() {
        return this.charge == this.capacity;
    }
}
