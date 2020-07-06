package TeleBotResources;

import BusResources.BusStop;
import TeleBotResources.NusGoPrototype3;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Hashtable;

public class Main {

    protected static Hashtable<String, BusStop> busStopTable = new Hashtable<String, BusStop>();

    public static void main(String[] args) {

        // initialising BusStops
        BusStop.initialiseBusStops(busStopTable);

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
