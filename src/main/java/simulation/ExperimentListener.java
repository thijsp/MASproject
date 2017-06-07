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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by thijspeirelinck on 5/06/17.
 */
public class ExperimentListener implements TickListener {

    private Simulator sim;
    private ArrayList<Integer> deliveredParcels = new ArrayList<>();
    private ArrayList<Integer> remainingParcels = new ArrayList<>();
    private ParcelGenerator parcelGen;
    private boolean done = false;

    ExperimentListener(Simulator sim, ParcelGenerator parcelGen) {
        this.parcelGen = parcelGen;
        this.sim = sim;
    }

    public boolean isDone() {
        return this.done;
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
        int remainingCount = depots.stream().mapToInt(DistributionCenter::getRemainingParcels).sum();
        int deliveredCount = deliveredParcels.size();
        remainingParcels.add(remainingCount);
        if (remainingCount == 0 && parcelGen.getParcels_generated() == parcelGen.getMaxParcels()
                && deliveredCount == parcelGen.getMaxParcels()) {
            sim.stop();
            this.done = true;
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

    public List<Long> getParcelsExistanceTime() {
        PDPModel pm = sim.getModelProvider().getModel(PDPModel.class);
        PDPModel.ParcelState delivered = PDPModel.ParcelState.DELIVERED;
        Collection<Parcel> deliveredParcels = pm.getParcels(delivered);
        return deliveredParcels.stream()
                .map(parcel -> ((DroneParcel)parcel).getExistanceTime())
                .collect(Collectors.toList());
    }

}
