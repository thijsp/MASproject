package agents.accessories;

import agents.UAV;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Motor extends Accessory {

    private Battery powerSource;
    private final double power;
    private final double maxSpeed;
    private UAV drone;

    public Motor(UAV uav, Battery battery, Double power) {
        super(uav);
        this.powerSource = battery;
        this.power = power;
        this.drone = uav;
        this.maxSpeed = uav.getMaxSpeed();
    }

    public Battery getPowerSource() {
        return powerSource;
    }

    public UAV getDrone() {
        return this.drone;
    }

    public double getPowerUsed(double speed) {
        // TODO don't use linear relation here
        double modulation = speed/this.maxSpeed;
        return modulation * this.power;
    }

    public double getConsumedEnergy(double time, double speed) {
        double consumedPower = this.getPowerUsed(speed);
        return time * consumedPower;
    }

    public void drainPower(double travelTime, double speed) {
         double consumedEnergy = this.getConsumedEnergy(travelTime, speed);
         this.getPowerSource().discharge(consumedEnergy);
    }

    public boolean canFlyTime(double travelTime, double speed) {
        double consumptionPlanned = this.getConsumedEnergy(travelTime, speed);
        return this.getPowerSource().enoughChargeFor(consumptionPlanned);
    }

    public boolean canFlyDistance(double distance, double speed) {
        double travelTime = distance/speed * 3600; // in sec;
        return this.canFlyTime(travelTime, speed);
    }
}
