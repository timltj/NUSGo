package BusResources;

public class Directions {
    private static final int WALK = 0;
    private static final int BUS = 1;

    private int mode;
    public double durationInSeconds;
    private double duration;
    public String busNumber;
    private String startingLocation;
    private String endingLocation;

    public Directions(String startingLocation, String endingLocation, String busNumber, double durationInSeconds){
        this.mode = 1;
        this.busNumber = busNumber;
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
        this.durationInSeconds = durationInSeconds;
        this.duration = durationInSeconds / 60;
    }

    public Directions(String startingLocation, String endingLocation, double walkingDuration) {
        this.mode = 0;
        this.durationInSeconds = walkingDuration;
        this.duration = walkingDuration / 60;
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
    }

    public void makeWalking(long duration) {
        this.durationInSeconds = duration;
        this.mode = 0;
    }

    public String toString() {
        if (mode == BUS) {return "Board " + this.busNumber + " from " + this.startingLocation + " and alight at " + this.endingLocation;}
        else {return "Walk for " + this.duration + " minutes from " + startingLocation + " to " + endingLocation;}
    }
}
