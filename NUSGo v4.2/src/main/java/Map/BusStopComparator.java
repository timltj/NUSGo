package Map;

import com.google.maps.model.LatLng;

import java.util.Comparator;

public class BusStopComparator implements Comparator<BusStop> {
    private Building building;

    protected BusStopComparator(Building building) {
        this.building = building;
    }

    @Override
    public int compare(BusStop busStop, BusStop other) { // compares distance with only distance
        LatLng buildingLatLng = building.getLatLng();
        double distToThis = Building.distance(buildingLatLng, busStop.getLatLong());
        double distToOther = Building.distance(buildingLatLng, other.getLatLong());
        if (distToThis < distToOther) return -1;
        else return 1;
    }
}
