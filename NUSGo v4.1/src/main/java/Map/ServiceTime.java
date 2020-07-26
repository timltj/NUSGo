package Map;

import java.time.LocalTime;

public class ServiceTime {
    LocalTime startTime;
    LocalTime endTime;
    double serviceFrequency;

    public ServiceTime(double start, double end, double serviceFrequency) {
        int startHour = (int) Math.floor(start/100);
        int startMin = (int) ((start - startHour * 100));
        this.startTime = LocalTime.of(startHour, startMin);

        int endHour = (int) Math.floor(end/100);
        int endMin = (int) ((end - endHour * 100));
        this.endTime = LocalTime.of(endHour, endMin);

        this.serviceFrequency = serviceFrequency;
    }

    public boolean findCurrServiceTime(LocalTime now) {
        if (now.compareTo(startTime) == -1 && now.compareTo(endTime) == 1) return true;
        else return false;
    }
}
