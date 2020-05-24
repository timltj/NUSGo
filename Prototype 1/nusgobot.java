import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class nusgobot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        SendMessage message = new SendMessage();
        long chat_id = update.getMessage().getChatId();
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text_message = update.getMessage().getText();
            if (text_message.equals("/start")) {
                String user_name = update.getMessage().getChat().getUserName();
                message
                        .setChatId(chat_id)
                        .setText(EmojiParser.parseToUnicode("Hello " + user_name + "! Welcome to NUSGoBot :bus: \n\nLet's get you to the nearest" +
                                "NUS bus stop.\n\nSend your current location or Enter any building name in NUS."));

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> rowsInline = new ArrayList<>();
                KeyboardRow rowInline = new KeyboardRow();
                KeyboardButton locationButton = new KeyboardButton()
                        .setText("Send Current Location")
                        .setRequestLocation(true);
                rowInline.add(locationButton);
                rowInline.add("Enter Building Name");
                rowsInline.add(rowInline);
                keyboardMarkup.setKeyboard(rowsInline);
                keyboardMarkup.setOneTimeKeyboard(true);
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (text_message.equals("Send Current Location")) {
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("For the sake of the simulation, your current location is");
            }
        } else if (update.hasMessage() && update.getMessage().hasLocation()) {
            message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("For simulation's sake, The nearest busstop to you is\n\nCOM 2\n\nBusServices:\nA1\nA2\nD1 to BIZ2\nD1 to UTown");
            SendVenue venue = new SendVenue().setChatId(chat_id).setTitle("COM 2").setAddress("15 Computing Dr, Singapore 117418")
                    .setLatitude(1.294260f)
                    .setLongitude(103.773806f);
            try {
                execute(message);
                execute(venue);
            } catch (TelegramApiException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "nusgobot";
    }

    @Override
    public String getBotToken() {
        return "722613425:AAFRgld4xxo22MswjAp3pywqazpMiPc4hp4";
    }
}
