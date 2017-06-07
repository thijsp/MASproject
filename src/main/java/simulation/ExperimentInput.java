package simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentInput {

    private static final List<Integer> MAXPARCEL_LIST = new ArrayList<>();
    public final int DEPOTS = 3;
    public final int UAVS = 20;
    public int STEPS;

    ExperimentInput(int steps, int minParcels, int maxParcels, boolean parcelExp) {
        this.STEPS = steps;
        if (parcelExp) {
            this.setMaxParcelList(steps, minParcels, maxParcels);
        }
    }

    private void setMaxParcelList(int steps, int minParcels, int maxParcels) {
        for (int i = minParcels; i <= maxParcels; i += maxParcels/steps) {
            MAXPARCEL_LIST.add(i);
        }
    }

    public List<Integer> getParcelList() {
        return MAXPARCEL_LIST;
    }

}
