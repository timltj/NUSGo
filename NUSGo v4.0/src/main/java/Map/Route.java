package Map;

import java.util.ArrayList;

public class Route {
    ArrayList<Directions> directionsArrayList;

    public Route(ArrayList<Directions> directionsArrayList) {
        this.directionsArrayList = directionsArrayList;
    }

    @Override
    public String toString() {
        int directionCounter = 1;
        String returnMessage = "\n";
        for(Directions dir : directionsArrayList) {
            returnMessage += "\n" + directionCounter + ". " + dir.toString() + "\n";
            directionCounter++;
        }
        return returnMessage;
    }
}
