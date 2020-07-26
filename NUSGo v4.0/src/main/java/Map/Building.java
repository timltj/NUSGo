package Map;

import TeleBot.Main;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TravelMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

public class Building {
    public String name;
    PlacesSearchResult psr;
    static Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    ArrayList<BusStop> busStopLst;

    public Building(PlacesSearchResult psr) {
        this.psr = psr;
        this.busStopLst = new ArrayList<BusStop>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = psr.name;
    }

    public LatLng getLatLng() {
        return psr.geometry.location;
    }

    public ArrayList<BusStop> findNearestBusStops() { //Compares based on LatLng distance, its free
        this.busStopLst.sort(new BusStopComparator(this));
        return busStopLst;
    }

    public Route findRoute(Building destination) {
        ArrayList<Directions> returnRoute = new ArrayList<Directions>();
        ArrayList<BusStop> busStopsFromOrigin = this.findNearestBusStops();
        ArrayList<BusStop> busStopsFromDestination = destination.findNearestBusStops();

        DistanceMatrix walkingDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.geoApiContext)
                .mode(TravelMode.WALKING)
                .origins(this.getLatLng())
                .destinations(destination.getLatLng())
                .awaitIgnoreError();

        double bestDuration = walkingDistanceMatrix.rows[0].elements[0].duration.inSeconds;
        if (bestDuration <= 20) {
            returnRoute.add(new Directions(true));
        } else if (bestDuration <= 600) {
            returnRoute.add(new Directions(this.name, destination.name, bestDuration));
        } else {
            System.out.println("Walking duration considered " + bestDuration);
            returnRoute.add(new Directions(this.name, destination.name, bestDuration));

            LocalTime time = LocalTime.now();
            LocalDate date = LocalDate.now();
            int day = date.getDayOfWeek().getValue();
            int serviceDayType;
            if (day <= 5) serviceDayType = 1;
            else if (day == 6) serviceDayType = 2;
            else serviceDayType = 3;

            for (int i = 0; i < 3; i++) {
                BusStop currStartingBusStop = busStopsFromOrigin.get(i);
                for (int j = 0; j < 3; j++) {
                    BusStop currDestinationBusStop = busStopsFromDestination.get(j);
                    System.out.println("Considered: " + currStartingBusStop.name + " to " + currDestinationBusStop.name + " takes ");

                    if (currStartingBusStop.directionsTable.containsKey(currDestinationBusStop.symbol)) {
                        Directions currBusDirection = currStartingBusStop.directionsTable.get(currDestinationBusStop.symbol);
                        ServiceDay sd = Main.serviceTimingTable.get(currBusDirection.busNumber)[serviceDayType - 1];
                        if (sd == null) currBusDirection.setCurrentlyRunning(false);

                        DistanceMatrix startDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.geoApiContext)
                                .mode(TravelMode.WALKING)
                                .origins(this.getLatLng())
                                .destinations(currStartingBusStop.latLong)
                                .awaitIgnoreError();

                        DistanceMatrix destinationDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.geoApiContext)
                                .mode(TravelMode.WALKING)
                                .origins(currDestinationBusStop.latLong)
                                .destinations(destination.getLatLng())
                                .awaitIgnoreError();


                        double durationOfWalkToStartingBusStop = startDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                        double durationOfWalkFroDestinationBusStop = destinationDistanceMatrix.rows[0].elements[0].duration.inSeconds;

                        double currTotalDuration = durationOfWalkToStartingBusStop +
                                +durationOfWalkFroDestinationBusStop
                                + currBusDirection.durationInSeconds;

                        if (currTotalDuration < bestDuration) {
                            bestDuration = currTotalDuration;
                            returnRoute = new ArrayList<Directions>();
                            returnRoute.add(new Directions(this.name, currStartingBusStop.name + " BusStop", durationOfWalkToStartingBusStop));
                            returnRoute.add(currBusDirection);
                            returnRoute.add(new Directions(currDestinationBusStop.name + " BusStop", destination.name, durationOfWalkFroDestinationBusStop));
                        }
                    }
                }
            }
        }
        return new Route(returnRoute);
    }

    protected static double distance(LatLng LL1, LatLng LL2) {
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
