package server;

import client.RideStatus;

public class Ride {
    private ClientHandler driver;
    private ClientHandler customer;
    private String pickupLocation;
    private String destination;
    private int fare;
    private RideStatus status;

    public Ride(ClientHandler driver, ClientHandler customer, 
                String pickupLocation, String destination, int fare, RideStatus status) {
        this.driver = driver;
        this.customer = customer;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.fare = fare;
        this.status = status;
    }

    public ClientHandler getDriver() { return driver; }
    public ClientHandler getCustomer() { return customer; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDestination() { return destination; }
    public int getFare() { return fare; }
    public RideStatus getStatus() { return status; }

    public void updateStatus(RideStatus newStatus) {
        this.status = newStatus;
    }
}

class RideRequest {
    private ClientHandler customer;
    private String pickupLocation;
    private String destination;

    public RideRequest(ClientHandler customer, String pickupLocation, String destination) {
        this.customer = customer;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
    }

    public ClientHandler getCustomer() {
        return customer;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public String getDestination() {
        return destination;
    }
}