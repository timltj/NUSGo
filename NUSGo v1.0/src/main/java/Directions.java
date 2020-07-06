class Directions {
    static int WALK = 0;
    static int BUS = 1;
    int mode;
    long durationInSeconds;
    int duration;
    String busNumber;
    String startingLocation;
    String endingLocation;

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
        else {return "walk";}
    }



}
