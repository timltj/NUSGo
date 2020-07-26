package TeleBot;

import Map.*;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class NUSGOPrototype4 extends TelegramLongPollingBot {
    // enum pages
    public enum Page {
        HOME, CURRENT_LOCATION_END, SELECT_BUS_START, SELECT_BUS_END, SEARCH_LOCATION_START,
        SEARCH_LOCATION_START_UPDATE, SEARCH_LOCATION_END, SEARCH_LOCATION_END_UPDATE, END;
    }

    // attributes
    Hashtable<String, BusStop> busStopTable = Main.busStopTable;
    BusStop startingBusStop;
    BusStop destinationBusStop;
    Building startingLocation;
    Building destinationLocation;
    GeoApiContext geoApiContext = BusStop.geoApiContext;
    PlacesSearchResult[] psrArr;
    ArrayList<PlacesSearchResult> prevPsrList = new ArrayList<PlacesSearchResult>();
    int prevPsrList_index = 0;

    // static fields
    private static Page page; // track current page
    private static int searchStartLocationViaTextCounter = 0; // searching for starting location via text

    // refresh bot
    public void refresh() {
        this.startingBusStop = null;
        this.destinationBusStop = null;
        this.startingLocation = null;
        this.destinationLocation = null;
        this.psrArr = null;
        this.prevPsrList = new ArrayList<PlacesSearchResult>();
        this.prevPsrList_index = 0;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long chat_id = update.getMessage().getChatId();
        String user_name = update.getMessage().getChat().getUserName();

        /* REPLY MESSAGE CONSTANTS */
        SendMessage message = new SendMessage();
        SendVenue venue = null;
        SendMessage route_message = null;
        String stopsList = "/29hmkter - 29 Heng Mui Keng Terrace\n/as5 - AS5\n/biz2 - BIZ 2\n" +
                "/bgmrt - Botanic Gardens MRT\n/othbldg - Oei Tiong Ham Building\n/cenlib - Central Library\n" +
                "/cgh - College Green Hostel\n/com2 - COM2 (CP13)\n/ea - EA\n/it - Information Technology\n" +
                "/i4 - innovation 4.0\n/krbt - Kent Ridge Bus Terminal\n/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                "/lt13 - LT13\n/lt27 - LT27\n/museum - Museum\n/opphssml - Opp HSSML\n/oppkrmrt - Opp Kent Ridge MRT\n" +
                "/oppnuss - Opp NUSS\n/opptcoms - Opp TCOMS\n/oppuhall - Opp UHall\n/oppuhc - Opp University Health Centre\n" +
                "/oppyih - Opp YIH\n/pgp15 - PGP Hse No 15\n/pgp7 - PGP Hse No 7\n/pgpr - Prince George's Park Residences\n" +
                "/pgp - Prince George's Park\n/raffleshall - Raffles Hall\n/s17 - S17\n/tcoms - TCOMS\n" +
                "/thejapanesesch - The Japanese Primary School\n/uhall - UHall\n/uhc - University Health Centre\n" +
                "/utown - University Town\n/ventus - Ventus\n/yih - YIH\n";
        SendMessage home_message = new SendMessage()
                .setChatId(chat_id)
                .setText(EmojiParser.parseToUnicode("Hello " + user_name + "! Welcome to NUSGoBot :bus: \n\nLet's get you to your NUS destination. " +
                        "\n\nPlease choose from one of the route finding options below."));
        SendMessage select_bus_start_message = new SendMessage()
                .setChatId(chat_id)
                .setText("Please select your starting bus stop from the list below " +
                        "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
        SendMessage search_location_start_message = message.setChatId(chat_id)
                .setText(EmojiParser.parseToUnicode("Please type your starting NUS location."));
        SendMessage end_message = new SendMessage()
                .setChatId(chat_id)
                .setText("Please type in the /restart command to re-enter a new query.");
        SendMessage destination_request_message = new SendMessage()
                .setChatId(chat_id)
                .setText("Please select your destination bus stop from the list below " +
                        "OR by typing '/' to reveal the command menu: \n\n" + stopsList);
        SendMessage previous_menu_message = new SendMessage()
                .setChatId(chat_id)
                .setText("To change your entry, type in the /back command to go back to the previous menu.");
        SendMessage help_message = new SendMessage()
                .setChatId(chat_id)
                .setText(EmojiParser.parseToUnicode("Hello " + user_name +
                        "! To start using NUSGoBot, click /start and select your preferred route finding option. " +
                        "\n\nRoute Finding Options\n\nSend Current Location - Send your current location and type in your destination bus stop for a route!" +
                        "\n\nSelect Starting Bus Stop - Select your starting and destination NUS bus stop for the quickest bus route!" +
                        "\n\nSearch NUS Location - Plan your journey in NUS beforehand by searching a starting and destination location in NUS. NUSGoBot will show you the best route!" +
                        "\n\nGeneric Commands\n/start - Starts NUSGoBot\n/restart - Restarts NUSGoBot for a new query\n/help - Opens up this help menu\n/back - Go back to the previous action" +
                        "\n\nBus Stop Commands\nThese commands are meant for users to easily choose their starting and destination bus stops."));
        SendMessage invalid_message = new SendMessage()
                .setChatId(chat_id)
                .setText("Please enter a valid NUSGoBot command.");

        /* START OF MESSAGE UPDATE CHECKS */
        if (update.hasMessage() && update.getMessage().hasText()) { // CHECK USER INPUT
            String text_message = update.getMessage().getText();
            if (text_message.equals("/start") || text_message.equals("/restart")) { // HOME page
                page = Page.HOME;

                refresh();

                message = home_message;

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
            } else if (text_message.equals("Select Starting Bus Stop") && page.equals(Page.HOME)) { // SELECT_BUS_START page
                page = Page.SELECT_BUS_START;
                message = select_bus_start_message;
            } else if (text_message.equals("Search NUS Location") && page.equals(Page.HOME)) { // SEARCH_LOCATION_START page
                page = Page.SEARCH_LOCATION_START;
                message = search_location_start_message;
            } else if (text_message.equals("/help")) { // HELP page
                message = help_message;
            } else if (text_message.equals("/back")) { // GO BACK TO PREVIOUS STEP
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> rowsInline = new ArrayList<KeyboardRow>();
                KeyboardRow rowInline = new KeyboardRow();
                switch (page) {
                    case HOME:
                        message = home_message;
                        refresh();
                        break;
                    case CURRENT_LOCATION_END:
                        page = Page.HOME;
                        message = home_message;

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
                        break;
                    case SELECT_BUS_START:
                    case SEARCH_LOCATION_START:
                    case SEARCH_LOCATION_START_UPDATE:
                        page = Page.HOME;
                        message = home_message;

                        refresh();

                        locationButton = new KeyboardButton()
                                .setText("Send Current Location")
                                .setRequestLocation(true);
                        rowInline.add(locationButton);
                        rowInline.add("Select Starting Bus Stop");
                        rowInline.add("Search NUS Location");
                        rowsInline.add(rowInline);

                        keyboardMarkup.setKeyboard(rowsInline);
                        keyboardMarkup.setOneTimeKeyboard(true);
                        message.setReplyMarkup(keyboardMarkup);
                        break;
                    case SELECT_BUS_END:
                        page = Page.SELECT_BUS_START;
                        message = select_bus_start_message;
                        break;
                    case SEARCH_LOCATION_END:
                        page = Page.SEARCH_LOCATION_START;
                        message = search_location_start_message;
                        refresh();
                        break;
                    case SEARCH_LOCATION_END_UPDATE:
                        page = Page.END;
                        message = end_message;
                        break;
                    case END:
                        message = end_message;
                        break;
                }
            } else if (page.equals(Page.CURRENT_LOCATION_END)) { // CURRENT_LOCATION_END page
                if (text_message.charAt(0) != '/') { // invalid command
                    message = invalid_message;
                } else {
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
            } else if (page.equals(Page.SELECT_BUS_START)) { // SELECT_BUS_START page
                if (text_message.charAt(0) != '/') { // invalid command
                    message = invalid_message;
                } else { // select start location
                    startingBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); // query the table starting busStop
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your starting bus stop is: " + startingBusStop.name);
                    venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol)
                            .setAddress(startingBusStop.name)
                            .setLatitude((float) startingBusStop.latLong.lat)
                            .setLongitude((float) startingBusStop.latLong.lng);
                }
            } else if (page.equals(Page.SELECT_BUS_END)) { // SELECT_BUS_END page
                if (text_message.charAt(0) != '/') { // invalid command
                    message = invalid_message;
                } else {
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
            } else if (page.equals(Page.SEARCH_LOCATION_START)
                    || page.equals(Page.SEARCH_LOCATION_END)) { // SEARCH_LOCATION_START or SEARCH_LOCATION_END page
                LatLng centralish = new LatLng(1.293928, 103.777572);

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
                if (searchStartLocationViaTextCounter >= 1) { // suggestions avaliable
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
                    returnMsg += "If it is none of these, please enter a new entry.";

                    if (searchStartLocationViaTextCounter == 1) { // single suggestion
                        prevPsrList = new ArrayList<PlacesSearchResult>(); // reset
                        prevPsrList.add(psrArr[0]); // record down current results
                    }

                    replyKeyboardMarkup.setKeyboard(rowsInLine)
                            .setOneTimeKeyboard(true);
                }

                if (page.equals(Page.SEARCH_LOCATION_START) && searchStartLocationViaTextCounter >= 1) {
                    page = Page.SEARCH_LOCATION_START_UPDATE;
                } else if (page.equals(Page.SEARCH_LOCATION_END) && searchStartLocationViaTextCounter >= 1) {
                    page = Page.SEARCH_LOCATION_END_UPDATE;
                }

                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(returnMsg);
                message.setReplyMarkup(replyKeyboardMarkup);
            } else if (page.equals(Page.SEARCH_LOCATION_START_UPDATE)
                    || page.equals(Page.SEARCH_LOCATION_END_UPDATE)) { // SEARCH_LOCATION_START_UPDATE or SEARCH_LOCATION_END_UPDATE page
                String returnMsg = "Please select the right option from the buttons below OR type /restart for other options."; // reply if location is not in NUS

                if (text_message.substring(1, 3).equals(". ")) { // check if this is a user selection
                    prevPsrList_index = Integer.parseInt(text_message.substring(0, 1)) - 1;
                    if (page.equals(Page.SEARCH_LOCATION_START_UPDATE)) {
                        page = Page.SEARCH_LOCATION_END;
                        startingLocation = new Building(prevPsrList.get(prevPsrList_index)); //Assigning starting building
                        returnMsg = "Your starting point is " + prevPsrList.get(prevPsrList_index).name + ". Please enter your destination location.";
                    } else {
                        destinationLocation = new Building(prevPsrList.get(prevPsrList_index));
                        returnMsg = "Your destination is " + prevPsrList.get(prevPsrList_index).name +
                                ".\n\nHere is how you get from " + startingLocation.name + " to " + destinationLocation.name;
                        Route answerRoute = startingLocation.findRoute(destinationLocation);
                        returnMsg += answerRoute.toString();
                    }
                }

                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText(returnMsg);
            } else { // INVALID INPUT
                message = invalid_message;
            }

            try {
                if (page.equals(Page.END)) { // get user to restart bot
                    execute(end_message);
                } else {
                    execute(message);
                    execute(venue);
                    if (page.equals(Page.SELECT_BUS_START)) {
                        execute(destination_request_message);
                        execute(previous_menu_message);
                        page = Page.SELECT_BUS_END;
                    } else {
                        execute(route_message);
                        page = Page.END;
                    }
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else if (update.getMessage().hasLocation() && page.equals(Page.HOME)) { // CURRENT_LOCATION_END page
            String returnMsg = "You are not at an NUS location. Please click /restart for other route planning options.";

            Float current_lat = update.getMessage().getLocation().getLatitude();
            Float current_long = update.getMessage().getLocation().getLongitude();

            if (current_lat > 1.292331 && current_lat < 1.306875
                    && current_long > 103.767406 && current_long < 103.786586) { // user is in NUS
                page = Page.CURRENT_LOCATION_END;
                double min_dist = Double.MAX_VALUE;
                for (BusStop bs : busStopTable.values()) { // find closest busstop to building
                    double starting_busstop_lat = bs.latLong.lat;
                    double starting_busstop_lng = bs.latLong.lng;

                    double cur_dist = distance(current_lat, current_long, starting_busstop_lat, starting_busstop_lng);

                    min_dist = Math.min(min_dist, cur_dist);

                    if (min_dist == cur_dist) { // update busstop
                        startingBusStop = bs;
                    }
                }

                returnMsg = "You are currently at: " + update.getMessage().getLocation().toString()
                        + "\n\nThe nearest bus stop to yous is: " + startingBusStop.name;

                venue = new SendVenue().setChatId(chat_id).setTitle(startingBusStop.symbol).setAddress(startingBusStop.name)
                        .setLatitude((float) startingBusStop.latLong.lat)
                        .setLongitude((float) startingBusStop.latLong.lng);
            }
            message = new SendMessage()
                    .setChatId(chat_id)
                    .setText(returnMsg);

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
        // return "1113433996:AAGPnthH2asB3wl8y8A3GZNy2ImEsIRUxXc";
    }

    /* METHOD TO CALCULATE DIST BY LATLNG */
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
