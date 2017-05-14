package agents.accessories;

import agents.constraints.BatteryConstraint;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Battery extends Accessory {

    private final double capacity; // in Ws
    private double charge; // in Ws
    private BatteryConstraint constraint;

    public Battery(Double capacity) {
        super();
        this.capacity = capacity;
        this.charge = capacity;
        this.constraint = new BatteryConstraint(this);
    }

    public double getCapacity() {
        return capacity;
    }

    public double getCharge() {
        return charge;
    }

    private void setCharge(Double newCharge) {
        this.charge = newCharge;
    }

    public BatteryConstraint getConstraint() {
        return this.constraint;
    }

    public double getChargeRate() {
        return this.getCharge()/this.getCapacity();
    }

    public void discharge(double consumedEnergy) {
        if (!this.enoughChargeFor(consumedEnergy)) throw new IllegalStateException("Battery is used up until negative charge");
        double newCharge = this.charge - consumedEnergy;
        this.charge = newCharge;
    }

    public boolean enoughChargeFor(double plannedConsumption) {
        double newCharge = this.charge - plannedConsumption;
        return this.getConstraint().isAllowedCharge(newCharge);
    }
}