package client;

public class Offer {

    private  String customerUsername;
    private String driverName;
    private int fare;

    public Offer(String driverName, int fare, String customerUsername) {
        this.driverName = driverName;
        this.customerUsername = customerUsername;
        this.fare = fare;
    }

    public String getDriverName() {
        return driverName;
    }

    public int getFare() {
        return fare;
    }
    public String getCustomerUsername() {
        return customerUsername;
    }
}
