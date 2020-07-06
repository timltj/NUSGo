public class Location {
    /* attributes */
    private String name;
    private String address;
    private double latitude;
    private double longitude;

    /* constructors */
    public Location() {
        super();
    }

    public Location(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /* getters */
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongtitude() {
        return longitude;
    }
}