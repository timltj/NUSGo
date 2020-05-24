import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        ApiContextInitializer.init(); // init api context
        TelegramBotsApi botsApi = new TelegramBotsApi(); // create new bot api

        try {
            botsApi.registerBot(new NUSGoBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println("NUSGoBot successfully started!");
    }
}
