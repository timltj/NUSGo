package TeleBotResources;

import BusResources.BusStop;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Hashtable;

public class Main {

    public static Hashtable<String, BusStop> busStopTable = new Hashtable<String, BusStop>();
    public static ArrayList<BusStop> busStopLst = new ArrayList<BusStop>();

    public static void main(String[] args) {

        // initialising BusStops
        BusStop.initialiseBusStops(busStopTable);
        busStopTable.values().iterator().forEachRemaining(x -> busStopLst.add(x));

        ApiContextInitializer.init(); // init api context
        TelegramBotsApi botsApi = new TelegramBotsApi(); // create new bot api

        try {
            botsApi.registerBot(new NusGoPrototype3());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println("NUSGoBot successfully started!");
    }
}