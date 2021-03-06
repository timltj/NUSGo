package TeleBot;

import Map.*;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;
import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVenue;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class NUSGoBot extends TelegramLongPollingBot {

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
    SendVenue navigateToDestination;
    GeoApiContext geoApiContext = BusStop.getGeoApiContext();
    PlacesSearchResult[] psrArr;
    ArrayList<PlacesSearchResult> prevPsrList = new ArrayList<PlacesSearchResult>();
    int prevPsrList_index = 0;
    HashMap<Long, String> ePassStore = new HashMap<>();
    boolean ePassRequest = false;

    // static fields
    private Page page; // track current page
    private int searchStartLocationViaTextCounter = 0; // searching for starting location via text

    // refresh bot
    public void refresh() {
        this.startingBusStop = null;
        this.destinationBusStop = null;
        this.startingLocation = null;
        this.destinationLocation = null;
        this.navigateToDestination = null;
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
        String stopsList = "/as5 - AS5\n/biz2 - BIZ 2\n" +
                "/cenlib - Central Library\n" +
                "/com2 - COM2 (CP13)\n/ea - EA\n/it - Information Technology\n" +
                "/krmrt - Kent Ridge MRT\n/kv - Kent Vale\n" +
                "/lt13 - LT13\n/lt27 - LT27\n/museum - Museum\n/opphssml - Opp HSSML\n/oppkrmrt - Opp Kent Ridge MRT\n" +
                "/oppnuss - Opp NUSS\n/opptcoms - Opp TCOMS\n/oppuhall - Opp UHall\n/oppuhc - Opp University Health Centre\n" +
                "/oppyih - Opp YIH\n/pgp15 - PGP Hse No 15\n/pgp7 - PGP Hse No 7\n/pgpr - Prince George's Park Residences\n" +
                "/pgp - Prince George's Park\n/raffleshall - Raffles Hall\n/s17 - S17\n/tcoms - TCOMS\n" +
                "/uhall - UHall\n/uhc - University Health Centre\n" +
                "/utown - University Town\n/ventus - Ventus\n/yih - YIH\n";
        SendMessage home_message = new SendMessage()
                .setChatId(chat_id)
                .setText(EmojiParser.parseToUnicode("Hello " + user_name + "! Welcome to NUSGoBot :bus:" +
                        "\n\nTo get to your NUS destination, please choose from one of the route finding options below." +
                        "\n\nSend us a Screenshot of your ePass and we'll send it to you everytime you type /pass"));
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
        SendMessage service_timings_enabled_message = new SendMessage()
                .setChatId(chat_id);
        SendMessage service_timings_disabled_message = new SendMessage()
                .setChatId(chat_id)
                .setText("This command is invalid as you have not searched for an NUSGoBot bus route.");
        SendMessage help_message = new SendMessage()
                .setChatId(chat_id)
                .setText(EmojiParser.parseToUnicode("Hello " + user_name +
                        "! To start using NUSGoBot, click /start and select your preferred route finding option. " +
                        "\n\n1. Route Finding Options\n\nSend Current Location:round_pushpin:- Send your current location and type in your destination bus stop for a route!" +
                        "\n\nSelect Starting Bus Stop:busstop:- Select your starting and destination NUS bus stop for the quickest bus route!" +
                        "\n\nSearch NUS Location:mag:- Plan your journey in NUS beforehand by searching a starting and destination location in NUS. NUSGoBot will show you the best route!" +
                        "\n\n2. Generic Commands\n/start - Starts NUSGoBot\n/restart - Restarts NUSGoBot for a new query\n/help - Opens up this help menu\n/back - Go back to the previous action" +
                        "\n\n3. Bus Stop Commands\nThese commands are meant for users to easily choose their starting and destination bus stops."));
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
            } else if (text_message.equals("/servicetimings")) { // service timings request
                HashMap<String, LocalDateTime> answerDateTimeMap = Route.requestAllService();
                String returnText = "";

                for (String busNumber : answerDateTimeMap.keySet()) {
                    LocalDateTime answerDateTime = answerDateTimeMap.get(busNumber);
                    returnText += "Bus " + busNumber + " next available service:\n"
                            + answerDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) + "\n"
                            + answerDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) + "\n\n";
                }
                message.setText(returnText);
            } else if (text_message.contains("servicetimings")) { // contains servicetimings but does not match entirely
                LocalDateTime answerDateTime = Route.requestNextService(text_message.substring(15));

                message.setText("Bus " + text_message.substring(15) + " next available service: " +
                        "\n\n" + answerDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)) +
                        "\n" + answerDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));

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
                        refresh();
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
            } else if (text_message.equals("/pass")) {
                ePassRequest = true;
                String passID = ePassStore.get(chat_id);
                SendPhoto sendPhoto = new SendPhoto()
                        .setChatId(chat_id)
                        .setPhoto(passID);
                try {
                    execute(sendPhoto);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (text_message.contains("/navigateto") && !text_message.equals("/navigateto")) {
                String actual = text_message.substring(11).toUpperCase();
                if (actual.equals("DESTINATION")) {
                    try {
                        execute(navigateToDestination);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    BusStop chosen = Main.busStopTable.get(actual);
                    SendVenue currBusStop = new SendVenue()
                            .setLatitude((float) chosen.getLatLong().lat)
                            .setLongitude((float) chosen.getLatLong().lng)
                            .setChatId(chat_id)
                            .setTitle("Here's how you navigate to " + actual + " Bus Stop")
                            .setAddress(actual + " Bus Stop");
                    try {
                        execute(currBusStop);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (page.equals(Page.SELECT_BUS_START)) { // SELECT_BUS_START page
                if (text_message.charAt(0) != '/') { // invalid command
                    message = invalid_message;
                } else { // select start location
                    startingBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); // query the table starting busStop
                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your starting bus stop is: " + startingBusStop.getName());
                }
            } else if (page.equals(Page.SELECT_BUS_END)) { // SELECT_BUS_END page
                if (text_message.charAt(0) != '/') { // invalid command
                    message = invalid_message;
                } else {
                    destinationBusStop = busStopTable.get(text_message.substring(1).toUpperCase()); // query the table destination busStop
                    navigateToDestination = new SendVenue()
                            .setChatId(chat_id)
                            .setLatitude((float) destinationBusStop.getLatLong().lat)
                            .setLongitude((float) destinationBusStop.getLatLong().lng)
                            .setTitle("Here's how to get to your destination: " + destinationBusStop.getSymbol() + " Bus Stop")
                            .setAddress(destinationBusStop.getName() + " Bus Stop");

                    ArrayList<Route> finalR = new Building(startingBusStop).findRoute(new Building(destinationBusStop));

                    message = new SendMessage()
                            .setChatId(chat_id)
                            .setText("Your destination bus stop is: " + destinationBusStop.getName());
                    venue = new SendVenue().setChatId(chat_id).setTitle(destinationBusStop.getSymbol())
                            .setAddress(destinationBusStop.getName())
                            .setLatitude((float) destinationBusStop.getLatLong().lat)
                            .setLongitude((float) destinationBusStop.getLatLong().lng);

                    String returnMsg = "Here are some ways to get from " + startingBusStop.getName() + " Bus Stop to "
                            + destinationBusStop.getName() + " Bus Stop: " +
                            "\n\n===================================";
                    int anyhow = 0;
                    for (Route r : finalR) {
                        if (anyhow > 4) break;
                        returnMsg += "\n\n" + r.toString() + "\n\n===================================";
                        anyhow++;
                    }
                    route_message = new SendMessage()
                            .setChatId(chat_id)
                            .setText(returnMsg);
                }
            } else if (page.equals(Page.SEARCH_LOCATION_START)
                    || page.equals(Page.SEARCH_LOCATION_END)
                    || page.equals(Page.CURRENT_LOCATION_END)) { // SEARCH_LOCATION_START or SEARCH_LOCATION_END page

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

                    replyKeyboardMarkup.setKeyboard(rowsInLine)
                            .setOneTimeKeyboard(true);

                    if (prevPsrList.isEmpty()) { // valid local but invalid NUS
                        returnMsg = "We are unable to find this location in NUS." +
                                "\nPlease enter a valid location OR type /restart for other options.";
                    } else if (page.equals(Page.SEARCH_LOCATION_START)) {
                        page = Page.SEARCH_LOCATION_START_UPDATE;
                    } else {
                        page = Page.SEARCH_LOCATION_END_UPDATE;
                    }
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
                        navigateToDestination = new SendVenue()
                                .setChatId(chat_id)
                                .setLatitude((float) destinationLocation.getLatLng().lat)
                                .setLongitude((float) destinationLocation.getLatLng().lng)
                                .setTitle("Here's how to get to your destination: " + destinationLocation.getName())
                                .setAddress(destinationLocation.getName());

                        returnMsg = "\n\nHere are some ways to get from " + startingLocation.getName() + " to " + destinationLocation.getName()
                        + "\n\n===================================";
                        ArrayList<Route> answerRoutes = startingLocation.findRoute(destinationLocation);
                        int anyhow = 0;

                        for (Route route : answerRoutes) {
                            if (anyhow > 2) break;
                            returnMsg += "\n" + route + "\n\n===================================";
                            anyhow++;
                        }
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
                } else if (ePassRequest) {
                    ePassRequest = false;
                } else {
                    execute(message);
                    if (page.equals(Page.SELECT_BUS_START) && startingBusStop != null) {
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

        } else if (update.getMessage().hasPhoto()) {
            List<PhotoSize> photos = update.getMessage().getPhoto();
            String f_id = photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null).getFileId();
            ePassStore.put(chat_id, f_id);
            message = new SendMessage()
                    .setChatId(chat_id)
                    .setText("Your photo has been saved. Type /pass anytime and the bot will send it to you.");
            try {
                execute(message);
            } catch (Exception e) {
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

                startingLocation = new Building(update.getMessage().getLocation().toString(), new LatLng(current_lat, current_long));
                returnMsg = "You are currently at: " + update.getMessage().getLocation().toString()
                + "\nPlease type your destination";

            startingLocation = new Building("King Edward VII Hall", new LatLng(1.291745, 103.780611));
            returnMsg = "You are currently at: King Edward VII Hall"
                    + "\nPlease type your destination";
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
