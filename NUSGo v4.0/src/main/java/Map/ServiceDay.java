package Map;

import java.util.ArrayList;

public class ServiceDay {
    private static final int WEEKDAY = 1;
    private static final int SATURDAY = 2;
    private static final int SUNDAYORPH = 3;
    protected ArrayList<ServiceTime> serviceTimeArrayList;
    int type;

    public ServiceDay() {}

    public ServiceDay(int type) {
        this.type = type;
        this.serviceTimeArrayList = new ArrayList<ServiceTime>();
    }

    public void assignServiceTime(ServiceTime serviceTime) {
        this.serviceTimeArrayList.add(serviceTime);
    }

}
