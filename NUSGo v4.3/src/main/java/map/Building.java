package map;

import telebot.Main;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TravelMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;

/** 
 * Encapsulates an NUS building.
 */
public class Building {
    private String name;
    private PlacesSearchResult psr;
    private LatLng latLng;
    private static Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    private ArrayList<BusStop> busStopLst;

    // getters
    public String getName() { 
        return this.name; 
    }

    public PlacesSearchResult getPsr() { 
        return this.psr; 
    }

    public ArrayList<BusStop> getBusStopLst() { 
        return this.busStopLst; 
    }

    public LatLng getLatLng() {
        return this.latLng;
    }
    
    /**
     * Constructs a Building object.
     * @param psr a PlacesSearchResult object
     */
    public Building(PlacesSearchResult psr) {
        this.psr = psr;
        this.busStopLst = new ArrayList<>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = psr.name;
        this.latLng = psr.geometry.location;
    }

    /**
     * Constructs a Building object.
     * @param busStop a BusStop object
     */
    public Building(BusStop busStop) {
        this.busStopLst = new ArrayList<>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = busStop.getName();
        this.latLng = busStop.getLatLong();
    }

    /**
     * Constructs a Building object.
     * @param name this building's name
     * @param currentLocation a LatLng object containing this building's coordinates
     */
    public Building(String name, LatLng currentLocation) {
        this.busStopLst = new ArrayList<>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = name;
        this.latLng = currentLocation;
    }

    public ArrayList<BusStop> sortBusStopsByDistance() {
        this.busStopLst.sort(new BusStopComparator(this));
        return busStopLst;
    }

    /**
     * Finds the possible routes between the start and destination buildings.
     * @param destination destination building
     */
    public ArrayList<Route> findRoute(Building destination) {
        ArrayList<Route> finalRoutes = new ArrayList<>();
        ArrayList<Directions> returnRoute = new ArrayList<>();
        ArrayList<BusStop> busStopsFromOrigin = this.sortBusStopsByDistance();
        ArrayList<BusStop> busStopsFromDestination = destination.sortBusStopsByDistance();

        DistanceMatrix walkingDistanceMatrix = DistanceMatrixApi
            .newRequest(BusStop.getGeoApiContext())
            .mode(TravelMode.WALKING)
            .origins(this.getLatLng())
            .destinations(destination.getLatLng())
            .awaitIgnoreError();

        double currDuration = walkingDistanceMatrix.rows[0].elements[0].duration.inSeconds;
        if (currDuration <= 20) {
            returnRoute.add(new Directions(true));
            finalRoutes.add(new Route(returnRoute, currDuration));
            return finalRoutes;
        } else {
            returnRoute.add(new Directions(this.name, 
                        destination.name, currDuration)); // add the walking distance
            finalRoutes.add(new Route(returnRoute, currDuration));

            LocalTime time = LocalTime.of(23, 59);
            LocalDate date = LocalDate.now();
            int day = date.getDayOfWeek().getValue();
            int serviceDayType;
            if (day <= 5) { 
                serviceDayType = 1; 
            } else if (day == 6) { 
                serviceDayType = 2; 
            } else { 
                serviceDayType = 3;
            }

            for (int i = 0; i < 3; i++) {
                BusStop currStartingBusStop = busStopsFromOrigin.get(i);
                for (int j = 0; j < 3; j++) {
                    BusStop currDestinationBusStop = busStopsFromDestination.get(j);
                    if (currStartingBusStop.getDirectionsTable().containsKey(currDestinationBusStop.getSymbol())) {
                        Directions currBusDirection = currStartingBusStop.getDirectionsTable().get(currDestinationBusStop.getSymbol());
                        ServiceDay sd = Main.serviceTimingTable.get(currBusDirection.getBusNumber())[serviceDayType - 1];
                        double extraWaitingTime = 0;
                        if (sd == null || sd.checkServiceFreq(time) == -1) {
                            currBusDirection.setCurrentlyRunning(false);
                        } else {
                            extraWaitingTime = sd.checkServiceFreq(time);
                            currBusDirection.addWaitingTime(extraWaitingTime);
                        }

                        DistanceMatrix startDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                                .mode(TravelMode.WALKING)
                                .origins(this.getLatLng())
                                .destinations(currStartingBusStop.getLatLong())
                                .awaitIgnoreError();

                        DistanceMatrix destinationDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                                .mode(TravelMode.WALKING)
                                .origins(currDestinationBusStop.getLatLong())
                                .destinations(destination.getLatLng())
                                .awaitIgnoreError();

                        double durationOfWalkToStartingBusStop = startDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                        double durationOfWalkFroDestinationBusStop = destinationDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                        double currTotalDuration = durationOfWalkToStartingBusStop +
                                +durationOfWalkFroDestinationBusStop
                                + currBusDirection.getDurationInSeconds()
                                + (extraWaitingTime * 60);

//                        System.out.println("considered: " + currStartingBusStop.getName() + " to " + currDestinationBusStop.getName() + ", duration: "
//                                + currTotalDuration);
//
//                        System.out.println("Total: " + currTotalDuration + ", walkToBusStop: " + durationOfWalkToStartingBusStop
//                        + ", bus ride: " + currBusDirection.getDurationInSeconds() + ", walk to destination: " + durationOfWalkFroDestinationBusStop
//                        + ", waiting time: " + (extraWaitingTime*60));

                        returnRoute = new ArrayList<>();
                        returnRoute.add(new Directions(this.name, currStartingBusStop.getName() + " BusStop", durationOfWalkToStartingBusStop));
                        returnRoute.add(currBusDirection);
                        returnRoute.add(new Directions(currDestinationBusStop.getName() + " BusStop", destination.name, durationOfWalkFroDestinationBusStop));
                        finalRoutes.add(new Route(returnRoute, currTotalDuration));
                    }
                }
            }
        }
        finalRoutes.sort(new RouteComparator());
        return finalRoutes;
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
