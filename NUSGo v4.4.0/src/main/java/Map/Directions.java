package Map;

import com.vdurmont.emoji.EmojiParser;

public class Directions {
    private static final int WALK = 0;
    private static final int BUS = 1;

    private int mode;
    private double durationInSeconds;
    private int duration;
    private String busNumber;
    private String startingLocation;
    private String endingLocation;
    private boolean sameLocation = false;
    private boolean currentlyRunning;
    private double waitingTime = 10;

    public int getMode() { return this.mode; }
    public double getDurationInSeconds() { return this.durationInSeconds; }
    public int getDuration() { return this.duration; }
    public String getBusNumber() { return this.busNumber;}
    public String getStartingLocation() { return this.startingLocation; }
    public String getEndingLocation() { return this.endingLocation; }
    public boolean getCurrentlyRunning() { return this.currentlyRunning; }
    public double getWaitingTime() { return this.waitingTime; }

    public Directions(String startingLocation, String endingLocation, String busNumber, double durationInSeconds) {
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
        if (this.sameLocation) {
            return EmojiParser.parseToUnicode("Walk Straight:walking:for 1 minute" +
                    "\n2. Turn Left and Walk Straight:walking:for 1 minute" +
                    "\n3. Turn Left and Walk Straight:walking:for 1 minute" +
                    "\n4. Turn Left and Walk Straight:walking:for 1 minute" +
                    "\n\nDon't disturb my bot leh...\nIt's the same place right...");
        } else if (mode == BUS) {
            if (currentlyRunning) {
                return EmojiParser.parseToUnicode("Board " + this.busNumber + ":oncoming_bus:(Bus Freq: " +
                        this.waitingTime + "min) from " + this.startingLocation + " and alight at " +
                        this.endingLocation + " after " + this.duration + " minutes");
            } else {
                return EmojiParser.parseToUnicode("Board " + this.busNumber + ":oncoming_bus: from " +
                        this.startingLocation + " and alight at " + this.endingLocation + " after " +
                        this.duration + " minutes" + "\n:exclamation:This service is not running at this moment, check /" + "servicetimings" +
                        this.busNumber + " for the next service.");
            }
        } else {
            return EmojiParser.parseToUnicode("Walk:walking:for " +
                    this.duration + " minutes from " + startingLocation + " to " + endingLocation);
        }
    }

    public void setCurrentlyRunning(boolean CR) {
        this.currentlyRunning = CR;
    }

    public void addWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
    }
}
