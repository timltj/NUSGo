package Map;

public class Directions {
    private static final int WALK = 0;
    private static final int BUS = 1;

    private int mode;
    public double durationInSeconds;
    private int duration;
    public String busNumber;
    private String startingLocation;
    private String endingLocation;
    private boolean sameLocation = false;
    public boolean currentlyRunning;

    public Directions(String startingLocation, String endingLocation, String busNumber, double durationInSeconds){
        this.mode = 1;
        this.busNumber = busNumber;
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
        this.durationInSeconds = durationInSeconds;
        this.duration = (int) Math.ceil(this.durationInSeconds / 60);
        this.currentlyRunning = true;
    }

    public Directions(String startingLocation, String endingLocation, double walkingDuration) {
        this.mode = 0;
        this.durationInSeconds = walkingDuration;
        this.duration = (int) Math.ceil(this.durationInSeconds / 60);
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
        this.currentlyRunning = true;
    }

    public Directions(boolean sameLocation) {
        this.sameLocation = sameLocation;
    }

    public void makeWalking(long duration) {
        this.durationInSeconds = duration;
        this.mode = 0;
    }

    public String toString() {
        if (this.sameLocation) { return "Walk Straight for 1 minute \n2. Turn Left and walk straight for 1 minute \n3. Turn Left and walk straight for 1 minute \n4. Turn Left and walk straight for 1 minute\n\nDon't disturb my bot leh...\nIt's the same place right...";}
        else if (mode == BUS) {
            if (currentlyRunning) {
                return "Board " + this.busNumber + " from " + this.startingLocation + " and alight at " + this.endingLocation + " after " + this.duration + " minutes";
            } else {
                return "This Service is not running at this moment, check /ServiceTimings for the next service.\n" + "Board " + this.busNumber + " from " + this.startingLocation + " and alight at " + this.endingLocation + " after " + this.duration + " minutes";
            }
        } else {return "Walk for " + this.duration + " minutes from " + startingLocation + " to " + endingLocation;}
    }

    public void setCurrentlyRunning(boolean CR) {this.currentlyRunning = CR; }
}
