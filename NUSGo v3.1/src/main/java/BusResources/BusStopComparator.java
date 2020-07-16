package BusResources;

import com.google.maps.model.LatLng;

import java.util.Comparator;

public class BusStopComparator implements Comparator<BusStop> {
    Building building;

    protected BusStopComparator(Building building) {
        this.building = building;
    }

    @Override
    public int compare(BusStop busStop, BusStop other) { //Compares distance with only distance
        LatLng buildingLatLng = building.getLatLng();
        double distToThis = distance(buildingLatLng, busStop.latLong);
        double distToOther = distance(buildingLatLng, other.latLong);
        if (distToThis < distToOther) {
            return -1;
        } else {
            return 1;
        }
    }

    private static double distance(LatLng LL1, LatLng LL2) {
        if (LL1.equals(LL2)) {
            return 0;
        } else {
            double theta = LL1.lng - LL2.lng;
            double dist = Math.sin(Math.toRadians(LL1.lat)) * Math.sin(Math.toRadians(LL2.lat))
                    + Math.cos(Math.toRadians(LL1.lat)) * Math.cos(Math.toRadians(LL2.lat)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515 * 1.609344 * 1000; // distance in meters
            return (dist);
        }
    }
}