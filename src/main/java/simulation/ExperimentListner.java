package simulation;

import agents.DistributionCenter;
import agents.DroneParcel;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentListner implements TickListener {

    private Simulator sim;
    private ArrayList<Integer> deliveredParcels = new ArrayList<>();
    private ArrayList<Integer> remainingParcels = new ArrayList<>();
    private ParcelGenerator parcelGen;

    ExperimentListner(Simulator sim, ParcelGenerator parcelGen) {
        this.parcelGen = parcelGen;
        this.sim = sim;
    }
    @Override
    public void tick(TimeLapse timeLapse) {
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
        RoadModel rm = sim.getModelProvider().getModel(RoadModel.class);
        PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);
        PDPModel.ParcelState delivered = PDPModel.ParcelState.DELIVERED;
        Collection<Parcel> deliveredParcels = pm.getParcels(delivered);
        this.deliveredParcels.add(deliveredParcels.size());
        Set<DistributionCenter> depots = rm.getObjectsOfType(DistributionCenter.class);
        int remainingParcel = 0;
        for (DistributionCenter depot : depots) {
            remainingParcel += depot.getRemainingParcels();
        }
        remainingParcels.add(remainingParcel);
        if (remainingParcel == 0 && parcelGen.getParcels_generated() > 0 ) {
            try {
                sim.stop();
            }
            catch (Exception e) {
                    System.err.println("caught error:" + e.getMessage());
            }
        }
    }

    public ArrayList<Integer> getDeliveredParcels() {
        return this.deliveredParcels;
    }

    public ArrayList<Integer> getRemainingParcels() {
        return this.remainingParcels;
    }

    public long getAverageDeliveryTime() {
        PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);
        PDPModel.ParcelState delivered = PDPModel.ParcelState.DELIVERED;
        Collection<Parcel> deliveredParcels = pm.getParcels(delivered);
        long sum = 0;
        for (Parcel parcel : deliveredParcels) {
            sum += ((DroneParcel)parcel).getDeliveryTime();
        }
        return sum / deliveredParcels.size();
    }

    public long getAverageExistanceTime() {
        PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);
        PDPModel.ParcelState delivered = PDPModel.ParcelState.DELIVERED;
        Collection<Parcel> deliveredParcels = pm.getParcels(delivered);
        long sum = 0;
        for (Parcel parcel : deliveredParcels) {
            sum += ((DroneParcel)parcel).getExistanceTime();
        }
        return sum / deliveredParcels.size();
    }

}
