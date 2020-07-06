package BusResources;

public class Directions {
    private static final int WALK = 0;
    private static final int BUS = 1;

    private int mode;
    private long durationInSeconds;
    private int duration;
    private String busNumber;
    private String startingLocation;
    private String endingLocation;

    public Directions(String startingLocation, String endingLocation, String busNumber){
        this.mode = 1;
        this.busNumber = busNumber;
        this.startingLocation = startingLocation;
        this.endingLocation = endingLocation;
    }

    public Directions(long walkingDuration) {
        this.mode = 0;
        this.durationInSeconds = walkingDuration;
    }

    public void makeWalking(long duration) {
        this.durationInSeconds = duration;
        this.mode = 0;
    }

    public String toString() {
        if (mode == BUS) {return "Board " + this.busNumber + " from " + this.startingLocation + " and alight at " + this.endingLocation;}
        else {return "Walk";}
    }
}
