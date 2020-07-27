package Map;

import TeleBot.Main;
import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Iterator;

public class BusStop {
    private String name;
    private String symbol;
    private LatLng latLong;
    private Hashtable<String, Double> ServiceOrderTiming;
    private Hashtable<String, Directions> directionsTable;
    private static GeoApiContext geoApiContext;

    //getters
    public String getName() {return this.name;}
    public String getSymbol() {return this.symbol;}
    public LatLng getLatLong() {return this.latLong;}
    public Hashtable<String, Double> getServiceOrderTiming() {return  this.ServiceOrderTiming;}
    public Hashtable<String, Directions> getDirectionsTable() {return this.directionsTable;}
    public static GeoApiContext getGeoApiContext() {return geoApiContext;}

    public BusStop(String name, String symbol, double lat, double longi) {
        this.name = name;
        this.symbol = symbol;
        this.latLong = new LatLng(lat, longi);
        this.ServiceOrderTiming = new Hashtable<String, Double>();
        this.directionsTable = new Hashtable<String, Directions>();
    }

    public static void initialiseBusStops(Hashtable<String, BusStop> busStopTable) {
        try {
            File file = new File("Stops.xlsx"); // creating a new file instance
            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
            //creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0); // creating a Sheet object to retrieve object
            Iterator<Row> itr = sheet.iterator(); // iterating over excel file
            Row Heading = itr.next();
            while (itr.hasNext()) {
                Row row = itr.next();
                int counter = 1;
                String busStopSymbol = "";
                String busStopName = "";
                double busStoplat = 0;
                double busStoplong = 0;
                Iterator<Cell> cellIterator = row.cellIterator(); // iterating over each column
                while (cellIterator.hasNext() && counter <= 4) {
                    Cell cell = cellIterator.next();
                    switch (counter) {
                        case 1:
                            busStopSymbol = cell.getStringCellValue();
                            counter++;
                            break;
                        case 2:
                            busStopName = cell.getStringCellValue();
                            counter++;
                            break;
                        case 3:
                            busStoplat = cell.getNumericCellValue();
                            counter++;
                            break;
                        case 4:
                            busStoplong = cell.getNumericCellValue();
                            counter++;
                            break;
                        default:
                    }
                }
                busStopTable.put(busStopSymbol, new BusStop(busStopName, busStopSymbol, busStoplat, busStoplong));
            }
            System.out.println("1. Done populating busStopTable");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Assigning ServiceOrderTiming
        try {
            File file = new File("Services.xlsx"); // creating a new file instance
            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
            // creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            int sheetCount = 0;
            while (sheetCount <= 4) {
                XSSFSheet sheet = wb.getSheetAt(sheetCount); // creating a Sheet object to retrieve object
                String currService = sheet.getSheetName();
                Iterator<Row> itr = sheet.iterator(); // iterating over excel file
                Row Heading = itr.next();
                int rowCounter = 1;
                while (itr.hasNext()) {
                    Row row = itr.next();
                    BusStop currBusStop = busStopTable.get(row.getCell(0).getStringCellValue());
                    double Duration = row.getCell(6).getNumericCellValue();
                    currBusStop.assignServiceOrderTiming(currService, Duration);
                    rowCounter++;
                }
                sheetCount++;
            }
            System.out.println("2. Done Assigning Service Order Duration");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // populate common svc busstop with directions
        try {
            File file = new File("Services.xlsx"); // creating a new file instance
            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
            // creating Workbook instance that refers to .xlsx file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            int sheetCount = 0;
            while (sheetCount <= 4) {
                XSSFSheet sheet = wb.getSheetAt(sheetCount); // creating a Sheet object to retrieve object
                String currServiceString = sheet.getSheetName();
                int totalRowCount = sheet.getLastRowNum();
                Iterator<Row> itr = sheet.iterator();
                Row Heading = itr.next();
                while (itr.hasNext()) {
                    Row row = itr.next();
                    String currBusStopString = row.getCell(0).getStringCellValue();
                    double currDurationOnRoute = row.getCell(6).getNumericCellValue();
                    BusStop currBusStop = busStopTable.get(currBusStopString);
                    int currentRow = row.getRowNum() + 1;
                    while (currentRow <= totalRowCount) {
                        String destinationBusStopString = sheet.getRow(currentRow).getCell(0).getStringCellValue();

                        double durationInSeconds = sheet.getRow(currentRow).getCell(6).getNumericCellValue() - currDurationOnRoute;

                        if (durationInSeconds < 0) {
                            System.out.println("service " + currServiceString + ": direction from " + currBusStopString + "(" + currBusStop.ServiceOrderTiming.get(currServiceString) + ")" + " to " + sheet.getRow(currentRow).getCell(0).getStringCellValue() + "("
                                    + sheet.getRow(currentRow).getCell(6).getNumericCellValue() + ")");
                        }

                        currBusStop.assignDirection(destinationBusStopString
                                , new Directions(currBusStopString, destinationBusStopString, currServiceString, durationInSeconds));
                        currentRow++;
                    }
                }
                sheetCount++;
            }
            System.out.println("3. Done Assigning Common Service Stops");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Assigning Walkable Distances
        geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyC7XkFChbcLnqN-7Qj1i4W92mu0bdsTCFA")
                .build();
        System.out.println("4. GeoAPIContext Initialised");

//        try {
//            File file = new File("Stops.xlsx"); // creating a new file instance
//            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
//            // creating Workbook instance that refers to .xlsx file
//            BusStop[] busStopArr = new BusStop[busStopTable.values().size()];
//            LatLng[] latLngArr = new LatLng[busStopTable.values().size()];
//            int latLngCounter = 0;
//            busStopTable.values().toArray(busStopArr);
//            for (BusStop bs : busStopArr) {
//                latLngArr[latLngCounter] = bs.latLong;
//                latLngCounter++;
//            }
//
//            XSSFWorkbook wb = new XSSFWorkbook(fis);
//            XSSFSheet sheet = wb.getSheetAt(0); // creating a Sheet object to retrieve object
//            Iterator<Row> itr = sheet.iterator(); // iterating over excel file
//            Row Heading = itr.next();
//            while (itr.hasNext()) {
//                DistanceMatrixApiRequest DMAReq = DistanceMatrixApi.newRequest(geoApiContext);
//                Row row = itr.next();
//                String currBusStopString = row.getCell(0).getStringCellValue();
//                BusStop currBusStop = busStopTable.get(currBusStopString);
//                DistanceMatrix distanceMatrix = DMAReq.destinations(latLngArr).origins(currBusStop.latLong).mode(TravelMode.WALKING).await();
//                for (int i = 0; i < busStopTable.values().size(); i++) {
//                    String queryBusStopString = busStopArr[i].symbol;
//                    long durationInSeconds = distanceMatrix.rows[0].elements[i].duration.inSeconds;
//                    if (!currBusStopString.equals(queryBusStopString) && durationInSeconds < 180) {
//                        currBusStop.directionsTable.put(queryBusStopString, new Directions(durationInSeconds));
//                    }
//                }
//            }
//            System.out.println("5. Done Assigning Walkable Distances");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        //Assigning Driving Duration in between BusStops, run once only
//        try {
//            File file = new File("Services.xlsx"); // creating a new file instance
//            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
//            // creating Workbook instance that refers to .xlsx file
//            XSSFWorkbook wb = new XSSFWorkbook(fis);
//            System.out.println("wb assigned");
//            for (int i = 0; i <= 4; i++) {
//                XSSFSheet sheet = wb.getSheetAt(i);
//                System.out.println("sheet assigned");

//                Iterator<Row> rowIterator = sheet.iterator();
//                Row Header = rowIterator.next();
//                Row startRow = rowIterator.next();
//                startRow.createCell(5).setCellValue(0);
//                System.out.println("hit");
//                BusStop prevBusStop = busStopTable.get(startRow.getCell(0).getStringCellValue());
//                System.out.println("starting loop 1");
//                //Enter duration in between 2 concurrent stops
//                while (rowIterator.hasNext()) {
//                    Row currentRow = rowIterator.next();
//                    BusStop currBusStop = busStopTable.get(currentRow.getCell(0).getStringCellValue());
//                    DistanceMatrix DMA = DistanceMatrixApi.newRequest(geoApiContext)
//                            .origins(prevBusStop.latLong)
//                            .destinations(currBusStop.latLong)
//                            .mode(TravelMode.DRIVING)
//                            .awaitIgnoreError();
//                    long durationV = DMA.rows[0].elements[0].duration.inSeconds;
//                    currentRow.createCell(5).setCellValue(durationV);
//                    prevBusStop = currBusStop;
//                    System.out.println(prevBusStop.name + ": " + durationV);
//                }
//
//                fis.close();
//
//                FileOutputStream outFile = new FileOutputStream(new File("Services.xlsx"));
//                wb.write(outFile);
//                outFile.close();

//                System.out.println("starting loop 2");
//
//                //Summing concurrent stops to get DurationOnRoute
//                Iterator<Row> rowIterator2 = sheet.iterator();
//                System.out.println("second iterator created");
//                Row Header2 = rowIterator2.next();
//                Row startingRow = rowIterator2.next();
//                double prevDurr = 0;
//
//                while(rowIterator2.hasNext()) {
//                    Row currRow = rowIterator2.next();
//                    double currDurr = currRow.getCell(5).getNumericCellValue();
//                    currRow.createCell(6).setCellValue(currDurr + prevDurr);
//                    prevDurr += currDurr;
//                }
//            }
//
//            fis.close();
//
//            FileOutputStream outFile = new FileOutputStream(new File("Services.xlsx"));
//            wb.write(outFile);
//            outFile.close();

//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

        try {
            File file = new File("ServiceTimings.xlsx"); // creating a new file instance
            FileInputStream fis = new FileInputStream(file); // obtaining bytes from the file
            XSSFWorkbook wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            ServiceDay[] currServiceDayArrList = new ServiceDay[3];
            while (rowIterator.hasNext()) {
                Row currRow = rowIterator.next();
                Iterator<Cell> cellIterator = currRow.iterator();
                String currString = cellIterator.next().getStringCellValue();
                ServiceDay currServiceDay = new ServiceDay();

                if (currString.equals("nil")) {
                    int currServiceDayType = (int) cellIterator.next().getNumericCellValue();
                    currServiceDay = new ServiceDay(currServiceDayType);
                    double startTime = 0;
                    double endTime = 0;
                    int freq = 0;

                    int counter = 1;
                    while (cellIterator.hasNext()) {
                        if (counter == 1) {
                            Cell currentCell = cellIterator.next();
                            startTime = currentCell.getNumericCellValue();
                            counter++;
                        } else if (counter == 2) {
                            Cell currentCell = cellIterator.next();
                            endTime = currentCell.getNumericCellValue();
                            counter++;
                        } else {
                            Cell currentCell = cellIterator.next();
                            currServiceDay.assignServiceTime(new ServiceTime(startTime, endTime, freq));
                            counter = 1;
                            startTime = 0;
                            endTime = 0;
                            freq = 0;
                        }
                    }
                    currServiceDayArrList[currServiceDayType - 1] = (currServiceDay);
                } else {
                    Main.serviceTimingTable.put(currString, currServiceDayArrList);
                    currServiceDayArrList = new ServiceDay[3];
                }
            }
            System.out.println("serviceTimingTable populated");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Directions getDirections(String destinationBusStop) {
        return directionsTable.get(destinationBusStop);
    }

    public void assignServiceOrderTiming(String service, double duration) {
        this.ServiceOrderTiming.put(service, duration);
    }

    public void assignDirection(String dest, Directions dir) {
        if (!this.directionsTable.containsKey(dest)) this.directionsTable.put(dest, dir);
    }
}
