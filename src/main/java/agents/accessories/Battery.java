package agents.accessories;

import agents.UAV;
import agents.constraints.BatteryConstraint;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Battery extends Accessory {

    private final double capacity; // in Ws
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

    public double getCharge() {
        return charge;
    }

    private void setCharge(double newCharge) {
        this.charge = newCharge;
    }

    public BatteryConstraint getConstraint() {
        return this.constraint;
    }

    public double getBatteryLevel() {
        return this.getCharge()/this.getCapacity();
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

    public void charge() {
        this.charge = Math.min(this.capacity, this.charge + this.chargingPower);
    }

    public boolean isFull() {
        return this.charge == this.capacity;
    }
}
