package agents.accessories;

import agents.UAV;

/**
 * Created by thijspeirelinck on 14/05/2017.
 */
public class Accessory {

    private UAV drone;


    public Accessory(UAV user) {
        this.drone = user;
    }

    public UAV getUser() {
        return this.drone;
    }
}
