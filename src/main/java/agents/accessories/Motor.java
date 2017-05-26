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
        super();
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
        double modulation = speed/this.maxSpeed;
        return modulation * this.power;
    }

    public double getConsumedEnergy(double time, double speed) {
        double consumedPower = this.getPowerUsed(speed);
        return time * consumedPower;
    }

    public void fly(double travelTime, double speed) {
         double consumedEnergy = this.getConsumedEnergy(travelTime, speed);
         this.getPowerSource().discharge(consumedEnergy);
    }

    public boolean canFly(double travelTime, double speed) {
        double consumptionPlanned = this.getConsumedEnergy(travelTime, speed);
        return this.getPowerSource().enoughChargeFor(consumptionPlanned);
    }

    public boolean possibleJourney(double distance, double speed) {
        double travelTime = distance/speed * 3600; // in sec;
        return this.canFly(travelTime, speed);
    }
}
