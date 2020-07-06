import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class BusStop {
    String name;
    String symbol;
    LatLng latLong;
    Hashtable<String, Integer> ServiceOrder;
    Hashtable<String, Directions> directionsTable;

    public BusStop(String name, String symbol, double lat, double longi) {
        this.name = name;
        this.symbol = symbol;
        this.latLong = new LatLng(lat, longi);
        this.ServiceOrder = new Hashtable<String, Integer>();
        this.directionsTable = new Hashtable<String, Directions>();
    }

    public static void initialiseBusStops(Hashtable<String, BusStop> busStopTable) {
        try {
            File file = new File("Stops.xlsx");   //creating a new file instance
            FileInputStream fis = new FileInputStream(file);   //obtaining bytes from the file
            //creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);     //creating a Sheet object to retrieve object
            Iterator<Row> itr = sheet.iterator();    //iterating over excel file
            while (itr.hasNext()) {
                Row row = itr.next();
                int counter = 1;
                String busStopSymbol = "";
                String busStopName = "";
                double busStoplat = 0;
                double busStoplong = 0;
                Iterator<Cell> cellIterator = row.cellIterator();   //iterating over each column
                while (cellIterator.hasNext() && counter <= 4) {
                    Cell cell = cellIterator.next();
                    switch (counter) {
                        case 1: busStopName = cell.getStringCellValue(); counter++; break;
                        case 2: busStopSymbol = cell.getStringCellValue(); counter++; break;
                        case 3: busStoplat = cell.getNumericCellValue(); counter++; break;
                        case 4: busStoplong = cell.getNumericCellValue(); counter++; break;
                        default:
                    }
                }
                busStopTable.put(busStopSymbol, new BusStop(busStopName, busStopSymbol, busStoplat, busStoplong));
            }
            System.out.println("done populating");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Assigning ServiceOrder
        try {
            File file = new File("Services.xlsx");   //creating a new file instance
            FileInputStream fis = new FileInputStream(file);   //obtaining bytes from the file
            //creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            int sheetCount = 0;
            while (sheetCount <= 6) {
                XSSFSheet sheet = wb.getSheetAt(sheetCount);     //creating a Sheet object to retrieve object
                String currService = sheet.getSheetName();
                Iterator<Row> itr = sheet.iterator();    //iterating over excel file
                int rowCounter = 1;
                while (itr.hasNext()) {
                    Row row = itr.next();
                    BusStop currBusStop = busStopTable.get(row.getCell(6).getStringCellValue());
                    currBusStop.assignServiceOrder(currService, rowCounter);
                    rowCounter++;
                }
                sheetCount++;
            }
            System.out.println("done updating");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // populate common svc busstop with directions
        try {
            File file = new File("Services.xlsx");   //creating a new file instance
            FileInputStream fis = new FileInputStream(file);   //obtaining bytes from the file
            // creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            int sheetCount = 0;
            while (sheetCount <= 9) {
                XSSFSheet sheet = wb.getSheetAt(sheetCount);     //creating a Sheet object to retrieve object
                String currServiceString = sheet.getSheetName();
                int totalRowCount = sheet.getLastRowNum();
                Iterator<Row> itr = sheet.iterator();
                while (itr.hasNext()) {
                    Row row = itr.next();
                    String currBusStopString = row.getCell(6).getStringCellValue();
                    BusStop currBusStop =  busStopTable.get(currBusStopString);
                    int currentRow = row.getRowNum();
                    while(currentRow <= totalRowCount) {
                        String destinationBusStopString = sheet.getRow(currentRow).getCell(6).getStringCellValue();
                        currBusStop.assignDirection(sheet.getRow(currentRow).getCell(6).getStringCellValue()
                                , new Directions(currBusStopString, destinationBusStopString, currServiceString));
                        currentRow++;
                    }
                }
                sheetCount++;
            }
            System.out.println("done updating commons");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // handling walking distance bus stops
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyDiogFAcRSVJAVaUD0XUoEhJa8H6sgIU44")
                .build();
        System.out.println("cont initialised");
        try {
            File file = new File("Stops.xlsx");   // creating a new file instance
            FileInputStream fis = new FileInputStream(file);   // obtaining bytes from the file
            // creating Workbook instance that refers to .xlsx file


            BusStop[] busStopArr = new BusStop[busStopTable.values().size()];
            LatLng[] latLngArr = new LatLng[busStopTable.values().size()];
            int latLngCounter = 0;
            busStopTable.values().toArray(busStopArr);
            for (BusStop bs : busStopArr) {
                latLngArr[latLngCounter] = bs.latLong;
                latLngCounter++;
            }

            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);     //creating a Sheet object to retrieve object
            Iterator<Row> itr = sheet.iterator();    //iterating over excel file
            while (itr.hasNext()) {
                DistanceMatrixApiRequest DMAReq = DistanceMatrixApi.newRequest(geoApiContext);
                Row row = itr.next();
                String currBusStopString = row.getCell(1).getStringCellValue();
                BusStop currBusStop = busStopTable.get(currBusStopString);
                DistanceMatrix distanceMatrix = DMAReq.destinations(latLngArr).origins(currBusStop.latLong).mode(TravelMode.WALKING).await();
                for (int i = 0; i < busStopTable.values().size(); i++) {
                    String queryBusStopString = busStopArr[i].symbol;
                    long durationInSeconds = distanceMatrix.rows[0].elements[i].duration.inSeconds;
                    if (!currBusStopString.equals(queryBusStopString) && durationInSeconds < 180) {
                        currBusStop.directionsTable.put(queryBusStopString, new Directions(durationInSeconds));
                    }
                }
            }
            System.out.println("done final");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Directions getDirections(String destinationBusStop) {
        return directionsTable.get(destinationBusStop);
    }

    public void assignServiceOrder(String service, int order) {
        this.ServiceOrder.put(service, order);
    }

    public void assignDirection(String dest, Directions dir) {
        this.directionsTable.put(dest, dir);
    }
}
