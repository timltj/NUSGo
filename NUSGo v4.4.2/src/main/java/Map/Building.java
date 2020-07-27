package Map;

import TeleBot.Main;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TravelMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;

public class Building {
    private String name;
    private PlacesSearchResult psr;
    private LatLng latLng;
    private static Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    private ArrayList<BusStop> busStopLst;

    //getters
    public String getName() { return this.name; }
    public PlacesSearchResult getPsr() { return this.psr; }
    public ArrayList<BusStop> getBusStopLst() { return this.busStopLst; }
    public LatLng getLatLng() {
        return this.latLng;
    }

    public Building(PlacesSearchResult psr) {
        this.psr = psr;
        this.busStopLst = new ArrayList<>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = psr.name;
        this.latLng = psr.geometry.location;
    }

    public Building(BusStop busStop) {
        this.busStopLst = new ArrayList<>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = busStop.getName();
        this.latLng = busStop.getLatLong();
    }

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

    public ArrayList<Route> findRoute(Building destination) {
        ArrayList<Route> finalRoutes = new ArrayList<>();
        ArrayList<Directions> returnRoute = new ArrayList<>();
        ArrayList<BusStop> busStopsFromOrigin = this.sortBusStopsByDistance();
        ArrayList<BusStop> busStopsFromDestination = destination.sortBusStopsByDistance();
        DistanceMatrix[] walkToStartBusStopDistanceMatrix = new DistanceMatrix[3];
        DistanceMatrix[] walkFromEndBusStopDistanceMatrix = new DistanceMatrix[3];
        boolean commonAvailable = false;

        DistanceMatrix walkingDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                .mode(TravelMode.WALKING)
                .origins(this.getLatLng())
                .destinations(destination.getLatLng())
                .awaitIgnoreError();

        double walkingDuration = walkingDistanceMatrix.rows[0].elements[0].duration.inSeconds;

        if (walkingDuration <= 20) {
            returnRoute.add(new Directions(true));
            finalRoutes.add(new Route(returnRoute, walkingDuration));
            return finalRoutes;
        } else if (walkingDuration <= 300) {
            returnRoute.clear();
            returnRoute.add(new Directions(this.name, destination.name, walkingDuration)); //add the walking distance
            finalRoutes.add(0, new Route(returnRoute, walkingDuration));
            return finalRoutes;
        } else {

            LocalTime time = LocalTime.now();
            LocalDate date = LocalDate.now();
            int day = date.getDayOfWeek().getValue();
            int serviceDayType;
            if (day <= 5) serviceDayType = 1;
            else if (day == 6) serviceDayType = 2;
            else serviceDayType = 3;

            for (int i = 0; i < 3; i++) {
                BusStop currStartingBusStop = busStopsFromOrigin.get(i);

                DistanceMatrix startDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                        .mode(TravelMode.WALKING)
                        .origins(this.getLatLng())
                        .destinations(currStartingBusStop.getLatLong())
                        .awaitIgnoreError();

                walkToStartBusStopDistanceMatrix[i] = startDistanceMatrix;

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

                        DistanceMatrix destinationDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                                .mode(TravelMode.WALKING)
                                .origins(currDestinationBusStop.getLatLong())
                                .destinations(destination.getLatLng())
                                .awaitIgnoreError();

                        walkFromEndBusStopDistanceMatrix[j] = destinationDistanceMatrix;

                        double durationOfWalkToStartingBusStop = startDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                        double durationOfWalkFroDestinationBusStop = destinationDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                        double currTotalDuration = durationOfWalkToStartingBusStop +
                                + durationOfWalkFroDestinationBusStop
                                + currBusDirection.getDurationInSeconds();

                        commonAvailable = true;
                        returnRoute = new ArrayList<>();
                        returnRoute.add(new Directions(this.name, currStartingBusStop.getSymbol() + " Bus Stop", durationOfWalkToStartingBusStop));
                        returnRoute.add(currBusDirection);
                        returnRoute.add(new Directions(currDestinationBusStop.getSymbol() + " Bus Stop", destination.name, durationOfWalkFroDestinationBusStop));

//                        System.out.println("walk from " + this.name + " to " + currStartingBusStop.getName() + "Bus Stop\nTake " + currBusDirection.getBusNumber()
//                        + " from " + currBusDirection.getStartingLocation() + " bus stop " + " to " + currBusDirection.getEndingLocation() + " bus stop" +
//                                "\nwalk from " + currDestinationBusStop.getName() + " Bus Stop" + " to " + destination.name);
                        Route curr = new Route(returnRoute, currTotalDuration);
                        finalRoutes.add(curr);
                    }
                }
            }
        }
        finalRoutes.sort(new RouteComparator());
        System.out.println("route sorted");
        if (!commonAvailable) {
            System.out.println("common not avail");
            ArrayList<Route> finalSupplementRoutes = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                Directions walkToStartingBusStop = new Directions(this.name, busStopsFromOrigin.get(i).getSymbol() + " BusStop", walkToStartBusStopDistanceMatrix[i].rows[0].elements[0].duration.inSeconds);
                for (int j = 0; j < 3; j++) {
                    ArrayList<Route> supplementRoutes = new ArrayList<>();
                    Directions walkFromEndingBusStop = new Directions(busStopsFromDestination.get(j).getSymbol() + " BusStop", destination.getName(), walkToStartBusStopDistanceMatrix[i].rows[0].elements[0].duration.inSeconds);

                    supplementRoutes.addAll(busStopsFromOrigin.get(i).findRoute(busStopsFromDestination.get(j)));
                    System.out.println("new algo returned for " + busStopsFromOrigin.get(i).getName() + " and " +
                            busStopsFromDestination.get(j).getName());
                    supplementRoutes.forEach(x -> { //add the walking time and directions
                        x.addDirection(0, walkToStartingBusStop);
                        x.addDirection(walkFromEndingBusStop);
                    });
                    finalSupplementRoutes.addAll(supplementRoutes);
                }
            }
            finalSupplementRoutes.sort(new RouteComparator());
            finalRoutes.addAll(finalSupplementRoutes);
        }
        finalRoutes.forEach(x -> System.out.println("before: " + x));
        returnRoute.clear();
        returnRoute.add(new Directions(this.name, destination.name, walkingDuration)); //add the walking distance
        finalRoutes.add(0, new Route(returnRoute, walkingDuration));
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

    protected boolean moreThan200Metres(LatLng LL2) {
        return (distance(this.latLng, LL2) >= 200);
    }
}
