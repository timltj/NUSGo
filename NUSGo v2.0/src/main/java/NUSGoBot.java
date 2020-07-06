import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Venue;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class NUSGoBot extends TelegramLongPollingBot {

    Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    BusStop startingBusStop;
    BusStop destinationBusStop;
    private static boolean selectButtonFlag = false; // track self-select button click
    private static boolean startSentFlag = false; // track if user has sent start location
    private static boolean restartFlag = false; // restart to query again

    @Override
    public void onUpdateReceived(Update update) {
        long chat_id = update.getMessage().getChatId();
        SendMessage message = new SendMessage();
        SendVenue venue = null;
        SendMessage destination_request_message = null;
        SendMessage route_message = null;
        SendMessage restart_message = new SendMessage()
                .setChatId(chat_id)
                .setText("Please type in the /restart command to re-enter a new query.");
        SendMessage previous_menu_message = new SendMessage()
                .setChatId(chat_id)
                .setText("To reselect starting bus stop, type in the /back command to go back to the previous menu.");

        if (update.hasMessage() && update.getMessage().hasText()) { // CHECK USER INPUT
            String text_message = update.getMessage().getText();
            if (text_message.equals("/start") || text_message.equals("/restart")) { // command - start NUSGoBot
                selectButtonFlag = false;
                startSentFlag = false;
                restartFlag = false;
                String user_name = update.getMessage().getChat().getUserName();

                message.setChatId(chat_id)
                        .setText(EmojiParser.parseToUnicode("Hello " + user_name +
                                "! Welcome to NUSGoBot :bus: \n\nLet's get you to the nearest " +
                                "NUS bus stop.\n\nSend your current location or Select the starting bus stop."));

                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> rowsInline = new ArrayList<KeyboardRow>();
                KeyboardRow rowInline = new KeyboardRow();

                KeyboardButton locationButton = new KeyboardButton()
                        .setText("Send Current Location")
                        .setRequestLocation(true);
                rowInline.add(locationButton);
                rowInline.add("Select Starting Bus Stop");
                rowsInline.add(rowInline);

                keyboardMarkup.setKeyboard(rowsInline);
                keyboardMarkup.setOneTimeKeyboard(true);
                message.setReplyMarkup(keyboardMarkup);
            } else if (text_message.equals("Select Starting Bus Stop")) { // USER CLICKS SELECT STARTING BUS STOP BUTTON
                selectButtonFlag = true;
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Please select your starting bus stop from the list below OR by typing " +
                                "'/' to reveal the command menu: \n\n" +
                                "/as7 - AS7\n/biz2 - BIZ 2\n/bgmrt - Botanic Gardens MRT\n" +
                                "/bukittimahbtc2 - BTC - Oei Tiong Ham Building\n/cenlib - Central Library\n/cgh - College Green Hostel\n" +
                                "/com2 - COM2 (CP13)\n/comcen - Computer Centre\n/blkeaopp - EA\n" +
                                "/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                                "/lt13 - LT13\n/lt29 - LT29\n/museum - Museum\n" +
                                "/hssmlopp - Opp HSSML\n/krmrtopp - Opp Kent Ridge MRT\n/nussopp - Opp NUSS\n" +
                                "/pgp12opp - Opp PGP Hse No 12\n/uhallopp - Opp UHall\n/staffclubopp - Opp University Health Centre\n" +
                                "/yihopp - Opp YIH\n/pgp12 - PGP Hse No 12\n/pgp1415 - PGP Hse No 14 and No 15\n" +
                                "/pgp7 - PGP Hse No 7\n/pgp - PGPR\n/pgpt - Prince George's Park\n" +
                                "/raffles - Raffles Hall\n/s17 - S17\n/uhall - UHall\n" +
                                "/staffclub - University Health Centre\n/utown - University Town\n/lt13opp - Ventus (Opp LT13)\n/yih - YIH\n");
            } else if (text_message.equals("/back")) { // USER GOES BACK TO RESELECT
                startSentFlag = false;
                selectButtonFlag = true;
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Please select your starting bus stop from the list below OR by typing " +
                                "'/' to reveal the command menu: \n\n" +
                                "/as7 - AS7\n/biz2 - BIZ 2\n/bgmrt - Botanic Gardens MRT\n" +
                                "/bukittimahbtc2 - BTC - Oei Tiong Ham Building\n/cenlib - Central Library\n/cgh - College Green Hostel\n" +
                                "/com2 - COM2 (CP13)\n/comcen - Computer Centre\n/blkeaopp - EA\n" +
                                "/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                                "/lt13 - LT13\n/lt29 - LT29\n/museum - Museum\n" +
                                "/hssmlopp - Opp HSSML\n/krmrtopp - Opp Kent Ridge MRT\n/nussopp - Opp NUSS\n" +
                                "/pgp12opp - Opp PGP Hse No 12\n/uhallopp - Opp UHall\n/staffclubopp - Opp University Health Centre\n" +
                                "/yihopp - Opp YIH\n/pgp12 - PGP Hse No 12\n/pgp1415 - PGP Hse No 14 and No 15\n" +
                                "/pgp7 - PGP Hse No 7\n/pgp - PGPR\n/pgpt - Prince George's Park\n" +
                                "/raffles - Raffles Hall\n/s17 - S17\n/uhall - UHall\n" +
                                "/staffclub - University Health Centre\n/utown - University Town\n/lt13opp - Ventus (Opp LT13)\n/yih - YIH\n");
            } else { // USER HAS SELECTED BUS STOP
                if (text_message.charAt(0) != '/') { // invalid command
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Please enter a valid bus stop command.");
                } else if (selectButtonFlag && !startSentFlag) { // select start location
                    startingBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); //query the table for the busStop
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your starting bus stop is: " + startingBusStop.name);
                    venue = new SendVenue().setChatId(chat_id).setTitle("Name Here").setAddress("Address Here")
                            .setLatitude((float) startingBusStop.latLong.lat)
                            .setLongitude((float) startingBusStop.latLong.lng)
                            .setTitle(startingBusStop.symbol)
                            .setAddress(startingBusStop.name);;
                    destination_request_message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Please select your destination bus stop from the list below OR by typing " +
                                    "'/' to reveal the command menu: \n\n" +
                                    "/as7 - AS7\n/biz2 - BIZ 2\n/bgmrt - Botanic Gardens MRT\n" +
                                    "/bukittimahbtc2 - BTC - Oei Tiong Ham Building\n/cenlib - Central Library\n/cgh - College Green Hostel\n" +
                                    "/com2 - COM2 (CP13)\n/comcen - Computer Centre\n/blkeaopp - EA\n" +
                                    "/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                                    "/lt13 - LT13\n/lt29 - LT29\n/museum - Museum\n" +
                                    "/hssmlopp - Opp HSSML\n/krmrtopp - Opp Kent Ridge MRT\n/nussopp - Opp NUSS\n" +
                                    "/pgp12opp - Opp PGP Hse No 12\n/uhallopp - Opp UHall\n/staffclubopp - Opp University Health Centre\n" +
                                    "/yihopp - Opp YIH\n/pgp12 - PGP Hse No 12\n/pgp1415 - PGP Hse No 14 and No 15\n" +
                                    "/pgp7 - PGP Hse No 7\n/pgp - PGPR\n/pgpt - Prince George's Park\n" +
                                    "/raffles - Raffles Hall\n/s17 - S17\n/uhall - UHall\n" +
                                    "/staffclub - University Health Centre\n/utown - University Town\n/lt13opp - Ventus (Opp LT13)\n/yih - YIH\n");
                } else if (startSentFlag) { // select destination to get route
                    /* QUERY DATABASE FOR NAME, ADDRESS AND COORDINATES */
                    String destinationBusStopString = text_message.substring(1).toUpperCase();
                    destinationBusStop = busStopTable.get(destinationBusStopString);
                    Directions finalD = startingBusStop.getDirections(destinationBusStopString);

                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your destination bus stop is: " + destinationBusStop.name);
                    venue = new SendVenue().setChatId(chat_id).setTitle("Name Here").setAddress("Address Here")
                            .setLatitude((float)destinationBusStop.latLong.lat)
                            .setLongitude((float)destinationBusStop.latLong.lng)
                            .setTitle(destinationBusStopString)
                            .setAddress(destinationBusStop.name);
                    route_message = new SendMessage()
                            .setChatId(chat_id)
                            .setText(finalD.toString());
                }
            }

            try {
                if (restartFlag) { // get user to restart bot
                    execute(restart_message);
                } else {
                    execute(message);
                    execute(venue);
                    if (selectButtonFlag && !startSentFlag) {
                        execute(destination_request_message);
                        execute(previous_menu_message);
                        startSentFlag = true;
                    } else {
                        execute(route_message);
                        restartFlag = true;
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.getMessage().hasLocation() && !selectButtonFlag) { // USER CLICK SEND CURRENT LOCATION BUTTON
            startSentFlag = true;
            message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("For simulation's sake, the nearest bus stop to you is:\n\nCOM 2");
            startingBusStop = busStopTable.get("COM2");
            venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol).setAddress(startingBusStop.name)
                    .setLatitude((float)startingBusStop.latLong.lat)
                    .setLongitude((float)startingBusStop.latLong.lng);
            destination_request_message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("Please select your destination bus stop from the list below OR by typing " +
                            "'/' to reveal the command menu: \n\n" +
                            "/as7 - AS7\n/biz2 - BIZ 2\n/bgmrt - Botanic Gardens MRT\n" +
                            "/bukittimahbtc2 - BTC - Oei Tiong Ham Building\n/cenlib - Central Library\n/cgh - College Green Hostel\n" +
                            "/com2 - COM2 (CP13)\n/comcen - Computer Centre\n/blkeaopp - EA\n" +
                            "/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                            "/lt13 - LT13\n/lt29 - LT29\n/museum - Museum\n" +
                            "/hssmlopp - Opp HSSML\n/krmrtopp - Opp Kent Ridge MRT\n/nussopp - Opp NUSS\n" +
                            "/pgp12opp - Opp PGP Hse No 12\n/uhallopp - Opp UHall\n/staffclubopp - Opp University Health Centre\n" +
                            "/yihopp - Opp YIH\n/pgp12 - PGP Hse No 12\n/pgp1415 - PGP Hse No 14 and No 15\n" +
                            "/pgp7 - PGP Hse No 7\n/pgp - PGPR\n/pgpt - Prince George's Park\n" +
                            "/raffles - Raffles Hall\n/s17 - S17\n/uhall - UHall\n" +
                            "/uhc - University Health Centre\n/utown - University Town\n/lt13opp - Ventus (Opp LT13)\n/yih - YIH\n");
            try {
                execute(message);
                execute(venue);
                execute(destination_request_message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "NUSGo_Practice_Bot";
    }

    @Override
    public String getBotToken() {
        return "1160826295:AAHp8jFip_SsqY2DeBCXsHaBgmO4WxvxUz4";
    }
}