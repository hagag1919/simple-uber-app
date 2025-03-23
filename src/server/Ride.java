package server;

public class Ride {
    private ClientHandler driver;
    private ClientHandler customer;

    public Ride(ClientHandler driver, ClientHandler customer) {
        this.driver = driver;
        this.customer = customer;
    }

    public ClientHandler getDriver() {
        return driver;
    }

    public ClientHandler getCustomer() {
        return customer;
    }
}
