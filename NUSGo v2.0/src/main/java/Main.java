import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import static org.apache.poi.ss.usermodel.CellType.NUMERIC;
import static org.apache.poi.ss.usermodel.CellType.STRING;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {

    static Hashtable<String, BusStop> busStopTable = new Hashtable<String, BusStop>();

    public static void main(String[] args) {

        // initialising BusStops
        BusStop.initialiseBusStops(busStopTable);

        ApiContextInitializer.init(); // init api context
        TelegramBotsApi botsApi = new TelegramBotsApi(); // create new bot api

        try {
            botsApi.registerBot(new NUSGoBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        System.out.println("NUSGoBot successfully started!");

        /* BUILDINGS WEB SCRAPPER */
        /*
        WebClient client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);

        try {
            HtmlPage page = client.getPage(baseUrl);
            //List<HtmlElement> items = (List<HtmlElement>) page.getByXPath(".//div[@id='td_content']");
            List<HtmlElement> items = (List<HtmlElement>) page.getByXPath(".//div[@id='td_content']");
            if (items.isEmpty()) {
                System.out.println("No items found");
            } else {
                for(HtmlElement htmlItem : items) {
                    HtmlElement spanAddress = ((HtmlElement) htmlItem.getFirstByXPath(".//div[@class='row']"));
                    HtmlElement itemLongitude = ((HtmlElement) htmlItem.getFirstByXPath(".//a/span[@class='result-price']"));
                    HtmlElement itemLatitude = ((HtmlElement) htmlItem.getFirstByXPath(".//a/span[@class='result-price']"));

                    String itemAddress = spanAddress == null ? "invalid" : spanAddress.asText();

                    Location location = new Location(itemAnchor.asText(), itemAddress,
                            Double.parseDouble(itemLongitude.asText()), Double.parseDouble(itemLatitude.asText()));

                    System.out.println(htmlItem);
                    System.out.println(spanAddress);
                }
            }
            //System.out.println(page.asXml()); // print page xml
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }
}