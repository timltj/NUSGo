package Map;

import java.time.LocalTime;
import java.util.ArrayList;

public class ServiceDay {
    private static final int WEEKDAY = 1;
    private static final int SATURDAY = 2;
    private static final int SUNDAYORPH = 3;
    protected ArrayList<ServiceTime> serviceTimeArrayList;
    protected LocalTime firstServiceTime;
    protected LocalTime lastServiceTime;

    int type;

    public ServiceDay() {}

    public ServiceDay(int type) {
        this.type = type;
        this.serviceTimeArrayList = new ArrayList<ServiceTime>();
    }

    public void assignServiceTime(ServiceTime serviceTime) {
        this.serviceTimeArrayList.add(serviceTime);
    }

    public double checkServiceFreq(LocalTime now) {
        firstServiceTime = serviceTimeArrayList.get(0).startTime;
        lastServiceTime = serviceTimeArrayList.get(serviceTimeArrayList.size() - 1).endTime;
        if (now.compareTo(firstServiceTime) == -1) return -1;
        else if (now.compareTo(lastServiceTime) == 1) return -1;
        else {
            ServiceTime currServiceTime = serviceTimeArrayList.get(0);
            for (ServiceTime st : serviceTimeArrayList) {
                if(st.findCurrServiceTime(now)) {
                    currServiceTime = st;
                    break;
                }
            }
            return currServiceTime.serviceFrequency;
        }
    }
}
