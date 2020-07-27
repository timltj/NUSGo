package Map;

import TeleBot.Main;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;

public class Route {
    private boolean noServiceFlag;
    private final ArrayList<Directions> directionsArrayList;
    private double duration;

    public Route(ArrayList<Directions> directionsArrayList, double duration) {
        this.noServiceFlag = false;
        this.directionsArrayList = directionsArrayList;
        this.duration = duration;
    }

    /* getters */
    public boolean getNoServiceFlag() {
        return this.noServiceFlag;
    }
    public ArrayList<Directions> getDirectionsArrayList() {
        return this.directionsArrayList;
    }
    public double getDuration() {return this.duration;}

    @Override
    public String toString() {
        int directionCounter = 1;
        String returnMessage = "\nTotal Duration: " + Math.ceil(duration/60) + " min";
        for(Directions dir : directionsArrayList) {
            if (!dir.getCurrentlyRunning()) { // no running service detected
                this.noServiceFlag = true;
            }
            returnMessage += "\n" + directionCounter + ". " + dir.toString();
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
                day++;
                Today = Today.plusDays(1);
                nextDay = true;
            }
        }
        System.out.println("returned!");
        return Today.atTime(serviceDayArr[serviceDayType - 1].getFirstServiceTime());
    }

    public static HashMap<String, LocalDateTime> requestAllService() {
        HashMap<String, LocalDateTime> returnMap = new HashMap<>();
        for (String busNumber: Main.serviceTimingTable.keySet()) {
            returnMap.put(busNumber, Route.requestNextService(busNumber));
        }
        return returnMap;
    }
}
