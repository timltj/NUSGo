package BusResources;

import TeleBotResources.Main;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Hashtable;

public class Building {
    public  String name;
    PlacesSearchResult psr;
    static Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    ArrayList<BusStop> busStopLst;


    public Building(PlacesSearchResult psr) {
        this.psr = psr;
        this.busStopLst = new ArrayList<BusStop>();
        this.busStopLst.addAll(Main.busStopLst);
        this.name = psr.name;
    }

    public LatLng getLatLng() { return psr.geometry.location; }

    public ArrayList<BusStop> findNearestBusStops() { //Compares baed on LatLng distance, its free
        this.busStopLst.sort(new BusStopComparator(this));
        return busStopLst;
    }

    public Route findRoute(Building destination) {
        System.out.println("findRoute called");
        ArrayList<Directions> returnRoute = new ArrayList<Directions>();
        ArrayList<BusStop> busStopsFromOrigin = this.findNearestBusStops();
        System.out.println(busStopsFromOrigin);
        ArrayList<BusStop> busStopsFromDestination = destination.findNearestBusStops();
        System.out.println(busStopsFromDestination);

        double bestDuration = 9999;

        for (int i = 0; i < 3; i++) {
            BusStop currStartingBusStop = busStopsFromOrigin.get(i);
            for (int j = 0; j < 3; j++) {
//                System.out.println("loop " + i + "." + j);
                BusStop currDestinationBusStop = busStopsFromDestination.get(j);
//                System.out.println("this is keyset for " + currStartingBusStop.symbol + currStartingBusStop.directionsTable.keySet());
                if (currStartingBusStop.directionsTable.containsKey(currDestinationBusStop.symbol)) {

//                    System.out.println("first if in loop " + i + "." + j);

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

                    Directions currBusDirection = currStartingBusStop.directionsTable.get(currDestinationBusStop.symbol);

                    double durationOfWalkToStartingBusStop = startDistanceMatrix.rows[0].elements[0].duration.inSeconds;
                    double durationOfWalkFroDestinationBusStop = destinationDistanceMatrix.rows[0].elements[0].duration.inSeconds;

                    double currTotalDuration = durationOfWalkToStartingBusStop +
                            + durationOfWalkFroDestinationBusStop
                            + currBusDirection.durationInSeconds;

                    if (currTotalDuration < bestDuration) {
                        bestDuration = currTotalDuration;
                        returnRoute = new ArrayList<Directions>();
                        returnRoute.add(new Directions(this.name, currStartingBusStop.name, durationOfWalkToStartingBusStop));
                        returnRoute.add(currBusDirection);
                        returnRoute.add(new Directions(currDestinationBusStop.name, destination.name ,durationOfWalkFroDestinationBusStop));
                    }
                }
            }
        }
//        System.out.println("findRoute reached return, Route is " + returnRoute);
        return new Route(returnRoute);
    }


}
