import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        ApiContextInitializer.init(); // init API context
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(); // create new bot API 

        try {
            telegramBotsApi.registerBot(new ZaurEduBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        
    }
}
