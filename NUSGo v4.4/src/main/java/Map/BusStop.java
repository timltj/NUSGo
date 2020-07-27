package Map;

import TeleBot.Main;
import com.google.gson.internal.bind.util.ISO8601Utils;
import com.google.maps.DistanceMatrixApi;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

public class BusStop {
    private String name;
    private String symbol;
    private LatLng latLong;
    private boolean crossable;
    private ArrayList<BusStop> neighbourLst = new ArrayList<>();
    private Hashtable<String, Integer> ServiceOrder; //hashmap of my own services
    private Hashtable<String, ArrayList<String>> neighbourServices;
    private Hashtable<String, Double> ServiceOrderTiming;
    private Hashtable<String, Directions> directionsTable;
    private static GeoApiContext geoApiContext = new GeoApiContext.Builder()
            .apiKey("AIzaSyC7XkFChbcLnqN-7Qj1i4W92mu0bdsTCFA")
            .build();

    //getters
    public String getName() {
        return this.name;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public LatLng getLatLong() {
        return this.latLong;
    }

    public Hashtable<String, Double> getServiceOrderTiming() {
        return this.ServiceOrderTiming;
    }

    public Hashtable<String, Directions> getDirectionsTable() {
        return this.directionsTable;
    }

    public boolean getCrossable() {return this.crossable;}

    public static GeoApiContext getGeoApiContext() {
        return geoApiContext;
    }

    public BusStop(String name, String symbol, double lat, double longi) {
        this.name = name;
        this.symbol = symbol;
        this.latLong = new LatLng(lat, longi);
        this.ServiceOrderTiming = new Hashtable<String, Double>();
        this.ServiceOrder = new Hashtable<String, Integer>();
        this.neighbourServices = new Hashtable<String, ArrayList<String>>();
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
            busStopTable.values().forEach(x -> Main.busStopLst.add(x));
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
                double Duration = 0;
                XSSFSheet sheet = wb.getSheetAt(sheetCount); // creating a Sheet object to retrieve object
                String currService = sheet.getSheetName();
                Iterator<Row> itr = sheet.iterator(); // iterating over excel file
                Row Heading = itr.next();

                while (itr.hasNext()) {
                    Row row = itr.next();
                    BusStop currBusStop = busStopTable.get(row.getCell(0).getStringCellValue());
                    Duration += row.getCell(5).getNumericCellValue();
                    double order = row.getCell(4).getNumericCellValue();
                    currBusStop.assignServiceOrderTiming(currService, order, Duration);
                    System.out.println(Duration + " " + order);
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
                if (currServiceString.equals("CC")) { //Special case cause looped
                    int totalRowCount = sheet.getLastRowNum();
                    Iterator<Row> itr = sheet.iterator();
                    Row Heading = itr.next();
                    double durationInSeconds = 0;
                    while (itr.hasNext()) {
                        Row row = itr.next();
                        int rowNumber = row.getRowNum();
                        String currBusStopString = row.getCell(0).getStringCellValue();
                        double currDurationOnRoute = row.getCell(6).getNumericCellValue();
                        BusStop currBusStop = busStopTable.get(currBusStopString);
                        int currentRow = row.getRowNum() + 1;
                        while (currentRow != rowNumber) {
                            if (currentRow == totalRowCount + 1) {
                                currentRow = 1;
                            } else {
                                String destinationBusStopString = sheet.getRow(currentRow).getCell(0).getStringCellValue();

                                durationInSeconds += sheet.getRow(currentRow).getCell(5).getNumericCellValue();

                                currBusStop.assignDirection(destinationBusStopString
                                        , new Directions(currBusStopString, destinationBusStopString, currServiceString, durationInSeconds));
                                currentRow++;
                            }
                        }
                        durationInSeconds = 0;
                    }
                } else {
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

                            double durationInSeconds = sheet.getRow(currentRow).getCell(6).getNumericCellValue();

                            currBusStop.assignDirection(destinationBusStopString
                                    , new Directions(currBusStopString, destinationBusStopString, currServiceString, durationInSeconds));
                            currentRow++;
                        }
                    }
                }
                sheetCount++;
            }
            System.out.println("3. Done Assigning Common Service Stops");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Help them find their neighbours
        //neighbour = walking distance = 60 seconds of walking
        for (BusStop bs : Main.busStopLst) {
            bs.assignNeighbours();
        }

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
                            System.out.println(startTime + ", " + endTime + ", " + currentCell.getNumericCellValue());
                            currServiceDay.assignServiceTime(new ServiceTime(startTime, endTime, (int) currentCell.getNumericCellValue()));
                            counter = 1;
                            startTime = 0;
                            endTime = 0;
                            freq = 0;
                        }
                    }
                    System.out.println("storing in arr");
                    currServiceDayArrList[currServiceDayType - 1] = (currServiceDay);
                } else {
                    System.out.println("storing overall");
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

    public void assignServiceOrderTiming(String service, double order, double duration) {
        this.ServiceOrderTiming.put(service, duration);
        this.ServiceOrder.put(service, (int) order);
        if (this.ServiceOrder.size() > 1) this.crossable = true;
    }

    public void assignDirection(String dest, Directions dir) {
        if (!this.directionsTable.containsKey(dest)) this.directionsTable.put(dest, dir);
    }

    public void assignNeighbours() {
        Building busStopBuilding = new Building(this);
        busStopBuilding.sortBusStopsByDistance();
        busStopBuilding.getBusStopLst().removeIf(x -> busStopBuilding.moreThan200Metres(x.getLatLong()));
        busStopBuilding.getBusStopLst().removeIf(x -> busStopBuilding.getName().equals(x.getName()));
        busStopBuilding.getBusStopLst().removeIf(x -> {
            DistanceMatrix walkingDistanceMatrix = DistanceMatrixApi.newRequest(BusStop.getGeoApiContext())
                    .mode(TravelMode.WALKING)
                    .origins(this.getLatLong())
                    .destinations(x.getLatLong())
                    .awaitIgnoreError();

            double duration = walkingDistanceMatrix.rows[0].elements[0].duration.inSeconds;
            if (duration <= 120) {
                this.directionsTable.put(x.getSymbol(), new Directions(this.getName() + " BusStop"
                        , x.getName() + " Bus Stop", duration));
                ArrayList<String> neighbourServices = new ArrayList<>();
                neighbourServices.addAll(x.ServiceOrder.keySet());
                this.neighbourServices.put(x.getSymbol(), neighbourServices);
            }
            return duration > 120;
        });
        this.neighbourLst = busStopBuilding.getBusStopLst();
    }

    public ArrayList<Route> useOnCrossBusStops(BusStop destinationBusStop){
        ArrayList<Route> finalRoutes = new ArrayList<>();
        ArrayList<Directions> returnRoute = new ArrayList<>();

        LocalTime time = LocalTime.now();
        LocalDate date = LocalDate.now();
        int day = date.getDayOfWeek().getValue();
        int serviceDayType;
        if (day <= 5) serviceDayType = 1;
        else if (day == 6) serviceDayType = 2;
        else serviceDayType = 3;
        System.out.println("time established");

        //Check for Common BusStops
        Directions commonBusService = this.directionsTable.get(destinationBusStop.getSymbol());
        if (commonBusService != null) {

            ServiceDay sd = Main.serviceTimingTable.get(commonBusService.getBusNumber())[serviceDayType - 1];
            System.out.println("sd found");
            if (sd == null || sd.checkServiceFreq(time) == -1) {
                commonBusService.setCurrentlyRunning(false);
            } else {
                commonBusService.addWaitingTime(sd.checkServiceFreq(time));
            }
            System.out.println("service timing handled");

            Route route = new Route(returnRoute, commonBusService.getDurationInSeconds());
            returnRoute.clear();
            returnRoute.add(commonBusService);
            finalRoutes.add(route);
            return finalRoutes;
        } else {
            //for every neighbour, check from services that i can use to get to destination busstop
            System.out.println("else called");
            for (BusStop neighbour : this.neighbourLst) {
                System.out.println("handling for " + this.getName() + "'s neighbour " + neighbour.getName());
                for (String Service : neighbour.ServiceOrder.keySet()) {
                    System.out.println(Service);
                    if (destinationBusStop.ServiceOrder.containsKey(Service)) {
                        if (Service.equals("CC") || destinationBusStop.ServiceOrder.get(Service) > neighbour.ServiceOrder.get(Service)) {
                            System.out.println("both ifs for " + Service);
                            returnRoute.clear();
                            double extraWaitingTime = 0;

                            Directions dir = this.directionsTable.get(neighbour.getSymbol());
                            Directions dirNeigh = neighbour.directionsTable.get(destinationBusStop.getSymbol());
                            System.out.println("directions done");
                            ServiceDay sdNeigh = Main.serviceTimingTable.get(dirNeigh.getBusNumber())[serviceDayType - 1];
                            System.out.println("sdNeigh done");

                            if (sdNeigh == null || sdNeigh.checkServiceFreq(time) == -1) {
                                dirNeigh.setCurrentlyRunning(false);
                            } else {
                                dirNeigh.addWaitingTime(sdNeigh.checkServiceFreq(time));
                                System.out.println(sdNeigh.checkServiceFreq(time));
                                extraWaitingTime+=(sdNeigh.checkServiceFreq(time)*60);
                            }

                            returnRoute.add(dir);
                            returnRoute.add(dirNeigh);
                            System.out.println("directions added");

                            double totalDuration = dir.getDuration()
                                    + dirNeigh.getDurationInSeconds()
                                    + extraWaitingTime;

                            System.out.println("totalDuration: " + totalDuration);

                            finalRoutes.add(new Route(returnRoute, totalDuration));
                        }
                    }
                }
            }
            System.out.println("final routes returned");
            return finalRoutes;
        }
    }

    private ArrayList<BusStop> findCrossStops(BusStop destinationBusStop) {
        ArrayList<BusStop> returningList = new ArrayList<>();
        for (String bs : this.getDirectionsTable().keySet()) {
            if (Main.busStopTable.get(bs).getCrossable()) {
                returningList.add(Main.busStopTable.get(bs));
            }
        }
        returningList.sort(new BusStopComparator(new Building(this))); //sort based on distance
        return returningList;
    }

    public ArrayList<Route> findRoute(BusStop destinationBusStop) {

        LocalTime time = LocalTime.now();
        LocalDate date = LocalDate.now();
        int day = date.getDayOfWeek().getValue();
        int serviceDayType;
        if (day <= 5) serviceDayType = 1;
        else if (day == 6) serviceDayType = 2;
        else serviceDayType = 3;

        ArrayList<Route> returnLst = new ArrayList<>();
        for (String Service : destinationBusStop.ServiceOrder.keySet()) {
            ArrayList<BusStop> stred = this.findCrossStops(destinationBusStop); //sort them
            System.out.println("crossable returned");
            for (BusStop bs : stred) {
                System.out.println("bs started for " + bs.getName());
                ArrayList<Route> currentRouteList = bs.useOnCrossBusStops(destinationBusStop);
                System.out.println("useOnCrossable returned");
                currentRouteList.forEach(x -> {
                    Directions dir = this.getDirectionsTable().get(bs.getSymbol());
                    ServiceDay sd = Main.serviceTimingTable.get(dir.getBusNumber())[serviceDayType - 1];
                    if (sd == null || sd.checkServiceFreq(time) == -1) {
                        dir.setCurrentlyRunning(false);
                    } else {
                        dir.addWaitingTime(sd.checkServiceFreq(time));
                    }
                    x.getDirectionsArrayList().add(0, dir);
                    x.addDuration(this.getDirectionsTable().get(bs.getSymbol()).getDurationInSeconds());
                    System.out.println(x.getDuration()/60);
                });
                returnLst.addAll(currentRouteList);
            }
        }
        returnLst.sort(new RouteComparator());
        return returnLst;
    }
}
