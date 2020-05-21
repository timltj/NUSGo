import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class NUSGoPracticeBot extends TelegramLongPollingBot {
    public void onUpdateReceived(Update update) {

        String command = update.getMessage().getText();
        SendMessage message = new SendMessage();

        /* sample commands */
        if(command.equals("/myname")) { // prints your first name
            System.out.println(update.getMessage().getFrom().getFirstName());
            message.setText(update.getMessage().getFrom().getFirstName());
        } else if (command.equals("/mylastname")) { // prints your last name
            System.out.println(update.getMessage().getFrom().getLastName());
            message.setText(update.getMessage().getFrom().getLastName());
        } else if (command.equals("/myfullname")) { // prints your full name
            System.out.println(update.getMessage().getFrom().getFirstName()+ " " +update.getMessage().getFrom().getLastName());
            message.setText(update.getMessage().getFrom().getFirstName()+ " " +update.getMessage().getFrom().getLastName());
        }

        message.setChatId(update.getMessage().getChatId());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    public String getBotUsername() { 
        return "NUSGo_Practice_Bot";
    }

    public String getBotToken() {
        return "1160826295:AAHp8jFip_SsqY2DeBCXsHaBgmO4WxvxUz4";
    }
}
