package agents;

import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * Created by anthony on 5/11/17.
 */
public class DroneParcel extends Parcel {
    public DroneParcel(Point source, Point destination) {
        super(Parcel.builder(source, destination).buildDTO());
    }

    @Override
    public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
        super.initRoadPDP(pRoadModel, pPdpModel);
    }
}
