package Map;

import java.util.ArrayList;

public class Route {
    private boolean noServiceFlag;
    private ArrayList<Directions> directionsArrayList;

    public Route(ArrayList<Directions> directionsArrayList) {
        this.noServiceFlag = false;
        this.directionsArrayList = directionsArrayList;
    }

    /* getters */
    public boolean getNoServiceFlag() {
        return this.noServiceFlag;
    }

    public ArrayList<Directions> getDirectionsArrayList() {
        return this.directionsArrayList;
    }

    @Override
    public String toString() {
        int directionCounter = 1;
        String returnMessage = "\n";
        for(Directions dir : directionsArrayList) {
            if (!dir.currentlyRunning) { // no running service detected
                this.noServiceFlag = true;
            }
            returnMessage += "\n" + directionCounter + ". " + dir.toString() + "\n";
            directionCounter++;
        }
        return returnMessage;
    }
}
