package Map;

import TeleBot.Main;
import com.vdurmont.emoji.EmojiParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

public class Route {
    private boolean noServiceFlag;
    private final ArrayList<Directions> directionsArrayList;
    private double duration; // seconds
    private double waitingDuration; //seconds

    public Route(ArrayList<Directions> directionsArrayList, double duration) {
        this.noServiceFlag = false;
        this.directionsArrayList = new ArrayList<>();
        this.directionsArrayList.addAll(directionsArrayList);
        this.duration = duration;
        directionsArrayList.forEach(dir -> this.addWaitingDuration((double) (dir.getWaitingTime())*60));
    }

    /* getters */
    public boolean getNoServiceFlag() {
        return this.noServiceFlag;
    }

    public ArrayList<Directions> getDirectionsArrayList() {
        return this.directionsArrayList;
    }

    public double getDuration() {
        return this.duration;
    }

    public double getWatingDuration() {
        return this.waitingDuration;
    }

    @Override
    public String toString() {
        int directionCounter = 1;
        String returnMessage = EmojiParser.parseToUnicode("\nDuration: " + Math.ceil(duration/60) + " min  |  :hourglass:Wait Time: " + Math.ceil(waitingDuration/60));
        for(Directions dir : directionsArrayList) {
            if (!dir.getCurrentlyRunning()) { // no running service detected
                this.noServiceFlag = true;
            }

            returnMessage += "\n\n" + directionCounter + ". " + dir.toString();
            directionCounter++;
        }
        return returnMessage;
    }

    public static LocalDateTime requestNextService(String busNumber) {
        System.out.println(busNumber + " requestNextService() called");
        LocalTime now = LocalTime.of(23,59);
        LocalDate Today = LocalDate.now();
        int day = Today.getDayOfWeek().getValue();
        int serviceDayType = 0;
        ServiceDay[] serviceDayArr = Main.serviceTimingTable.get(busNumber);
        System.out.println("gotten ServiceDay[] for " + busNumber);
        boolean nextDay = false;
        while (day <= 7) {
            if (day <= 5) {
                serviceDayType = 1;
            } else if (day == 6) {
                serviceDayType = 2;
            } else {
                serviceDayType = 3;
            }

            ServiceDay currServiceDay = serviceDayArr[serviceDayType - 1]; //here
            System.out.println(currServiceDay);
            if (currServiceDay != null) currServiceDay.assignStartAndEndTimes();
            if (currServiceDay != null && (now.compareTo(currServiceDay.getFirstServiceTime()) < 0 || nextDay)) break;
            else {
                System.out.println("day: " + day);
                if (day == 7) { day = 1; }
                else { day++; }
                Today = Today.plusDays(1);
                nextDay = true;
            }
        }
        return Today.atTime(serviceDayArr[serviceDayType - 1].getFirstServiceTime());
    }

    public static HashMap<String, LocalDateTime> requestAllService() {
        HashMap<String, LocalDateTime> returnMap = new HashMap<>();
        for (String busNumber: Main.serviceTimingTable.keySet()) {
            returnMap.put(busNumber, Route.requestNextService(busNumber));
        }
        return returnMap;
    }

    public void addDuration(double duration) {
        this.duration += duration;
    }
    public void addWaitingDuration(double waitingDuration) {
        this.waitingDuration += waitingDuration;
    }

    public void addDirection(Directions directions) {
        this.directionsArrayList.add(directions);
        this.duration += directions.getDurationInSeconds();
        if (directions.getMode()==1) {
            this.waitingDuration += directions.getWaitingTime()*60;
            System.out.println("waiting duration of " + directions.getWaitingTime() + " added");
        }
    }
    public void addDirection(int index, Directions directions) {
        this.directionsArrayList.add(index, directions);
        this.duration += directions.getDurationInSeconds();
        if (directions.getMode()==1) {
            this.waitingDuration += directions.getWaitingTime()*60;
            System.out.println("waiting duration of " + directions.getWaitingTime() + " added");
        }
    }
}