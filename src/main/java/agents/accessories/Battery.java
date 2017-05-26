package agents.accessories;

import agents.constraints.BatteryConstraint;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Battery extends Accessory {

    private final double capacity; // in Ws
    private double charge; // in Ws
    private BatteryConstraint constraint;
    private final double chargingPower; // in Ws

    public Battery(double capacity) {
        super();
        this.capacity = capacity;
        this.charge = capacity;
        this.constraint = new BatteryConstraint(this);
        this.chargingPower = capacity/500;
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

    public double getChargePercentage() {
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

    public void charge() {
        this.charge = Math.min(this.capacity, this.charge + this.chargingPower);
    }
}
