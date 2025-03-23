package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class UberServer implements Runnable {
    private int port = 12345;
    private ServerSocket serverSocket;
    private Map<Integer, ClientHandler> customers = new HashMap<>();
    private Map<Integer, ClientHandler> drivers = new HashMap<>();
    private Map<String, String> userCredentials = new HashMap<>();
    private Map<String, String> userTypes = new HashMap<>();
    private int clientIdCounter = 0;
    private List<Ride> activeRides = new ArrayList<>();

    public UberServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
        // Add admin credentials
        userCredentials.put("admin", "123");
        userTypes.put("admin", "admin");
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int getNextClientId() {
        return clientIdCounter++;
    }

    public void addCustomer(int id, ClientHandler clientHandler) {
        customers.put(id, clientHandler);
    }

    public void addDriver(int id, ClientHandler clientHandler) {
        drivers.put(id, clientHandler);
    }

    public boolean registerUser(String username, String password, String type) {
        if (userCredentials.containsKey(username)) {
            return false;
        }
        userCredentials.put(username, password);
        userTypes.put(username, type);
        return true;
    }

    public boolean authenticateUser(String username, String password) {
        return (userCredentials.containsKey(username) && userCredentials.get(username).equals(password));
    }

    public String getUserType(String username) {
        return userTypes.get(username);
    }

    public void requestRide(ClientHandler customer) {
        System.out.println("Ride requested by customer: " + customer.getUsername());
        for (ClientHandler driver : drivers.values()) {
            driver.sendMessage("New ride request available!");
        }
        // Send confirmation to the customer
        customer.sendMessage("Ride requested. Waiting for driver offers.");
    }

    public void handleRideOffer(ClientHandler driver, int fare) {
        // Fixed to match the client side expected format
        for (ClientHandler customer : customers.values()) {
            customer.sendMessage("Driver " + driver.getUsername() + " offers ride for " + fare);
        }
    }

    public void startRide(ClientHandler driver, ClientHandler customer) {

        if (customer == null) {
            driver.sendMessage("Error: Customer not found");
            return;
        }

        activeRides.add(new Ride(driver, customer));
        driver.sendMessage("Ride started with customer " + customer.getUsername());
        customer.sendMessage("Ride started with driver " + driver.getUsername());
    }

    public void completeRide(ClientHandler driver) {
        Ride completedRide = null;
        for (Ride ride : activeRides) {
            if (ride.getDriver().equals(driver)) {
                completedRide = ride;
                break;
            }
        }

        if (completedRide != null) {
            ClientHandler customer = completedRide.getCustomer();
            if (customer != null) {
                customer.sendMessage("Ride completed by driver " + driver.getUsername());
            }

            activeRides.remove(completedRide);
            driver.sendMessage("Ride completed");
        } else {
            driver.sendMessage("No active ride found to complete");
        }
    }

    public void getAdminStats(ClientHandler admin) {
        admin.sendMessage("Total active rides: " + activeRides.size());
        admin.sendMessage("Total customers: " + customers.size());
        admin.sendMessage("Total drivers: " + drivers.size());
    }

    public ClientHandler getCustomerById(int id) {
        return customers.get(id);
    }

    public static void main(String[] args) {
        try {
            UberServer server = new UberServer();
            new Thread(server).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}