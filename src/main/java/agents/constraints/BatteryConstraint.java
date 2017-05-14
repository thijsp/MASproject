package agents.constraints;

import agents.accessories.Battery;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class BatteryConstraint extends ConstraintHandler {

    private Battery battery;
    private final double minChargeRate;

    public BatteryConstraint(Battery battery) {
        this.minChargeRate = 0.2;
        this.battery = battery;
    }

    public double getMinChargeRate() {
        return minChargeRate;
    }

    public Battery getBattery() {
        return this.battery;
    }

    public boolean isSatisfied(Double newChargeRate) {
        return newChargeRate > this.getMinChargeRate();
    }

    public boolean isAllowedCharge(double newCharge) {
        double newChargeRate = newCharge/this.getBattery().getCapacity();
        return isSatisfied(newChargeRate);
    }
}
