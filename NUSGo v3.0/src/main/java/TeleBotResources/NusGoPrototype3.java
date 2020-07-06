package TeleBotResources;

import BusResources.BusStop;
import BusResources.Directions;

import com.google.maps.FindPlaceFromTextRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class NusGoPrototype3 extends TelegramLongPollingBot {

    Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    BusStop startingBusStop;
    BusStop destinationBusStop;
    GeoApiContext geoApiContext = BusStop.geoApiContext;
    PlacesSearchResult[] psrArr;
    String search_text_message;

    ArrayList<PlacesSearchResult> prevPsrList = new ArrayList<PlacesSearchResult>();
    int prevPsrList_index = 0;

    private static boolean selectButtonFlag = false; // track self-select button click
    private static boolean searchButtonFlag = false; // track search NUS location button click
    private static boolean startSentFlag = false; // track if user has sent start location
    private static boolean restartFlag = false; // restart to query again
    private static int searchStartLocationViaTextCounter = 0; //searching for starting location via text
    private static boolean enteringLocation = false;

    private static final String stopsList = "/as7 - AS7\n/biz2 - BIZ 2\n/bgmrt - Botanic Gardens MRT\n" +
            "/bukittimahbtc2 - BTC - Oei Tiong Ham Building\n/cenlib - Central Library\n/cgh - College Green Hostel\n" +
            "/com2 - COM2 (CP13)\n/comcen - Computer Centre\n/blkeaopp - EA\n" +
            "/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
            "/lt13 - LT13\n/lt29 - LT29\n/museum - Museum\n" +
            "/hssmlopp - Opp HSSML\n/krmrtopp - Opp Kent Ridge MRT\n/nussopp - Opp NUSS\n" +
            "/pgp12opp - Opp PGP Hse No 12\n/uhallopp - Opp UHall\n/uhcopp - Opp University Health Centre\n" +
            "/yihopp - Opp YIH\n/pgp12 - PGP Hse No 12\n/pgp1415 - PGP Hse No 14 and No 15\n" +
            "/pgp7 - PGP Hse No 7\n/pgp - PGPR\n/pgpt - Prince George's Park\n" +
            "/raffles - Raffles Hall\n/s17 - S17\n/uhall - UHall\n" +
            "/uhc - University Health Centre\n/utown - University Town\n/lt13opp - Ventus (Opp LT13)\n/yih - YIH\n";

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
                searchButtonFlag = false;
                enteringLocation = false;
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
                rowInline.add("Search NUS Location");
                rowsInline.add(rowInline);

                keyboardMarkup.setKeyboard(rowsInline);
                keyboardMarkup.setOneTimeKeyboard(true);
                message.setReplyMarkup(keyboardMarkup);
            } else if (text_message.equals("Select Starting Bus Stop")) { // SELECT STARTING BUS STOP BUTTON
                selectButtonFlag = true;
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Please select your starting bus stop from the list below " +
                                "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
            } else if (text_message.equals("Search NUS Location")) { // SEARCH NUS LOCATION BUTTON
                searchButtonFlag = true;
                enteringLocation = true;
                message.setChatId(chat_id)
                        .setText(EmojiParser.parseToUnicode("Please type your starting NUS location."));
            } else if (text_message.equals("/back")) { // USER GOES BACK TO RESELECT
                startSentFlag = false;
                selectButtonFlag = true;
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Please select your starting bus stop from the list below " +
                                "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
            } else if (text_message.equals("Yes")) { // USER CONFIRMS BUILDING SUGGESTION
                String returnMsg = "This is not an NUS location.";

                // building's latlng
                double building_lat = prevPsrList.get(prevPsrList_index).geometry.location.lat;
                double building_lng = prevPsrList.get(prevPsrList_index).geometry.location.lng;

                double min_dist = Double.MAX_VALUE;
                BusStop closest_busstop = null;

                for (BusStop bs : busStopTable.values()) { // find closest busstop to building
                    double starting_busstop_lat = bs.latLong.lat;
                    double starting_busstop_lng = bs.latLong.lng;

                    double cur_dist = distance(building_lat, building_lng, starting_busstop_lat, starting_busstop_lng);

                    min_dist = Math.min(min_dist, cur_dist);

                    if (min_dist == cur_dist) { // update busstop
                        closest_busstop = bs;
                    }
                }

                System.out.println(min_dist);

                // update message
                if (!startSentFlag) {
                    startSentFlag = true;
                    startingBusStop = closest_busstop;
                    returnMsg = "The closest bus stop to your starting location is: " + closest_busstop.name + "\n\nPlease enter your destination NUS Building.";
                    venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol)
                            .setAddress(startingBusStop.name)
                            .setLatitude((float) startingBusStop.latLong.lat)
                            .setLongitude((float) startingBusStop.latLong.lng);
                } else {
                    returnMsg = "The closest bus stop to your destination location is: " + closest_busstop.name;
                    // send route message
                    destinationBusStop = closest_busstop; // destination busstop
                    Directions finalD = startingBusStop.getDirections(destinationBusStop.symbol);
                    venue = new SendVenue().setChatId(chat_id).setTitle(destinationBusStop.symbol)
                            .setAddress(destinationBusStop.name)
                            .setLatitude((float) destinationBusStop.latLong.lat)
                            .setLongitude((float) destinationBusStop.latLong.lng);
                    route_message = new SendMessage()
                            .setChatId(chat_id)
                            .setText(finalD.toString());
                }

                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(returnMsg);
            } else if (text_message.equals("No")) { // USER REJECTS BUILDING SUGGESTION
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Please enter another location OR type /restart for other options.");
            } else if (searchButtonFlag) { // USER TYPES NUS LOCATION
                search_text_message = text_message;
                LatLng centralish = new LatLng(1.293928, 103.777572);
                // commented
                FindPlaceFromTextRequest findPlaceFromTextRequest = PlacesApi.findPlaceFromText(geoApiContext, text_message, FindPlaceFromTextRequest.InputType.TEXT_QUERY)
                        .locationBias(new FindPlaceFromTextRequest.LocationBiasRectangular(new LatLng(1.293142, 103.768978), new LatLng(1.293656, 103.787131)));
                psrArr = findPlaceFromTextRequest.awaitIgnoreError().candidates;
                // commented
                PlacesSearchResponse placesSearchResponse = PlacesApi.textSearchQuery(geoApiContext, text_message)
                        .location(new LatLng(1.296617, 103.774557))
                        .radius(700)
                        .region("sg")
                        .awaitIgnoreError();
                psrArr = placesSearchResponse.results;
                searchStartLocationViaTextCounter = psrArr.length;

                String returnMsg = "We are unable to find this location in NUS." +
                        "\nPlease enter a valid location OR type /restart for other options.";
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                if (searchStartLocationViaTextCounter > 1 && search_text_message.charAt(1) != '.') {
                    List<KeyboardRow> rowsInLine = new ArrayList<KeyboardRow>();
                    returnMsg = "We found a number of possible addresses, please select one: \n\n";
                    int counter = 1;

                    prevPsrList = new ArrayList<PlacesSearchResult>(); // reset

                    for (PlacesSearchResult psr : psrArr) {
                        LatLng psrLatLng = psr.geometry.location;
                        double psrLat = psrLatLng.lat;
                        double psrLng = psrLatLng.lng;
                        if (psrLat < 1.292331 || psrLng < 103.767406 || psrLat > 1.306875 || psrLng > 103.786586) {
                        } else {
                            returnMsg += counter + ". " + psr.name + "\n" + "Located at: " + psr.formattedAddress + "\n\n";
                            KeyboardRow rowInLine = new KeyboardRow();
                            rowInLine.add(counter + ". " + psr.name + "\n" + psr.formattedAddress);
                            rowsInLine.add(rowInLine);
                            counter++;

                            prevPsrList.add(psr); // record down current results
                        }
                    }
                    returnMsg += "\nIf it is none of these, please enter a new entry.";

                    if (counter == 1) { // no NUS locations shown
                        returnMsg = "We are unable to find this location in NUS." +
                                "\nPlease enter a valid location OR type /restart for other options.";
                    }

                    replyKeyboardMarkup.setKeyboard(rowsInLine)
                            .setOneTimeKeyboard(true);
                } else if (searchStartLocationViaTextCounter == 1) { // possible NUS location
                    prevPsrList = new ArrayList<PlacesSearchResult>(); // reset
                    prevPsrList.add(psrArr[0]); // record down current results

                    returnMsg = "Did you mean " + psrArr[0].name + " Located at: "
                            + psrArr[0].formattedAddress + "\n\nClick Yes to confirm OR No to enter a new entry.";

                    replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rowsInLine = new ArrayList<KeyboardRow>();

                    KeyboardRow keyboardRow = new KeyboardRow();
                    keyboardRow.add("Yes");
                    keyboardRow.add("No");
                    rowsInLine.add(keyboardRow);
                    replyKeyboardMarkup
                            .setOneTimeKeyboard(true)
                            .setKeyboard(rowsInLine);
                } else if (search_text_message.substring(1, 3).equals(". ")) { // CHECK IF THIS IS A SELECTION REQUEST
                    prevPsrList_index = Integer.parseInt(search_text_message.substring(0, 1)) - 1;
                    returnMsg = "Did you mean " + prevPsrList.get(prevPsrList_index).name + " Located at: "
                            + prevPsrList.get(prevPsrList_index).formattedAddress + "\n\nClick Yes to confirm OR No to enter a new entry.";

                    replyKeyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rowsInLine = new ArrayList<KeyboardRow>();

                    KeyboardRow keyboardRow = new KeyboardRow();
                    keyboardRow.add("Yes");
                    keyboardRow.add("No");
                    rowsInLine.add(keyboardRow);
                    replyKeyboardMarkup
                            .setOneTimeKeyboard(true)
                            .setKeyboard(rowsInLine);
                }
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(returnMsg);
                message.setReplyMarkup(replyKeyboardMarkup);
            } else { // USER HAS SELECTED BUS STOP
                if (text_message.charAt(0) != '/') { // invalid command
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Please enter a valid bus stop command.");
                } else if (selectButtonFlag && !startSentFlag) { // select start location
                    startingBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); // query the table starting busStop
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your starting bus stop is: " + startingBusStop.name);
                    venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol)
                            .setAddress(startingBusStop.name)
                            .setLatitude((float) startingBusStop.latLong.lat)
                            .setLongitude((float) startingBusStop.latLong.lng);
                    destination_request_message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Please select your destination bus stop from the list below " +
                                    "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
                } else if (startSentFlag) { // select destination to get route
                    destinationBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); // query the table destination busStop
                    Directions finalD = startingBusStop.getDirections(destinationBusStop.symbol);
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your destination bus stop is: " + destinationBusStop.name);
                    venue = new SendVenue().setChatId(chat_id).setTitle(destinationBusStop.symbol)
                            .setAddress(destinationBusStop.name)
                            .setLatitude((float) destinationBusStop.latLong.lat)
                            .setLongitude((float) destinationBusStop.latLong.lng);
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
                    } else if (searchButtonFlag && startSentFlag) {
                        execute(route_message);
                    } else {
                        execute(route_message);
                        restartFlag = true;
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.getMessage().hasLocation() && !selectButtonFlag) { // SEND CURRENT LOCATION BUTTON
            startSentFlag = true;
            message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("For simulation's sake, the nearest bus stop to you is:\n\nCOM 2");
            startingBusStop = busStopTable.get("COM2");
            venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol).setAddress(startingBusStop.name)
                    .setLatitude((float) startingBusStop.latLong.lat)
                    .setLongitude((float) startingBusStop.latLong.lng);
            destination_request_message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("Please select your destination bus stop from the list below " +
                            "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
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

    /* method to calculate distance by latlng */
    private static double distance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515 * 1.609344 * 1000; // distance in meters
            return (dist);
        }
    }
}