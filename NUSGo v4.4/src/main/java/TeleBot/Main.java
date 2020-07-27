package TeleBot;

import Map.BusStop;
import Map.ServiceDay;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.naming.directory.Attributes;
import java.util.ArrayList;
import java.util.Hashtable;

public class Main {
    public static Hashtable<String, BusStop> busStopTable = new Hashtable<String, BusStop>();
    public static ArrayList<BusStop> busStopLst = new ArrayList<BusStop>();
    public static  Hashtable<String, ServiceDay[]> serviceTimingTable = new Hashtable<String, ServiceDay[]>();

    public static void main(String[] args) {

        // initialising BusStops
        BusStop.initialiseBusStops(busStopTable);
//        for (String Service : serviceTimingTable.keySet()) {
//            ServiceDay[] arr = serviceTimingTable.get(Service);
//            System.out.println(Service);
//            arr[1].assignStartAndEndTimes();
//            System.out.println(arr[1].getFirstServiceTime());
//        }
        System.out.println(Main.serviceTimingTable.get("BB")[2]);
        busStopTable.values().iterator().forEachRemaining(x -> busStopLst.add(x));

        ApiContextInitializer.init(); // init api context
        TelegramBotsApi botsApi = new TelegramBotsApi(); // create new bot api

        try {
            botsApi.registerBot(new NUSGOPrototype4());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println("5. NUSGoBot successfully started!");
    }
}
