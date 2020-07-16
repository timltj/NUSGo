package BusResources;

import java.util.ArrayList;

public class Route { // a route is just an ArrayList<Directions>
    ArrayList<Directions> directionsArrayList;

    public Route(ArrayList<Directions> directionsArrayList) {
        this.directionsArrayList = directionsArrayList;
    }

    @Override
    public String toString() {
        int directionCounter = 1;
        String returnMessage = "";
        for(Directions dir : directionsArrayList) {
            returnMessage += "\n" + directionCounter + ". " + dir.toString();
            directionCounter++;
        }
        return returnMessage;
    }
}
