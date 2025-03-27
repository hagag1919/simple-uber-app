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
    private List<RideRequest> rideRequests = new ArrayList<>();

    public UberServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
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

    public void requestRide(ClientHandler customer, String pickupLocation, String destination,String username) {
        System.out.println("Ride requested by customer: " + username);
        RideRequest rideRequest = new RideRequest(customer, pickupLocation, destination);
        rideRequests.add(rideRequest);
        for (ClientHandler driver : drivers.values()) {
            driver.sendMessage("New ride request from " + username + 
                               " | Pickup: " + pickupLocation + 
                               " | Destination: " + destination);
        }
        customer.sendMessage("Ride requested. Waiting for driver offers.");
    }

    public void handleRideOffer(ClientHandler driver, String customerId, int fare) {
        ClientHandler customer = null;
        for (ClientHandler c : customers.values()) {
            if (c.username.equals(customerId)) {
                customer = c;
                break;
            }
        }

        if (customer != null) {
            RideRequest matchingRequest = findRideRequestForCustomer(customer);
            if (matchingRequest != null) {
                customer.sendMessage("Driver " + driver.username + 
                                     " offers ride for " + fare + 
                                     " | Pickup: " + matchingRequest.getPickupLocation() + 
                                     " | Destination: " + matchingRequest.getDestination());
            } else {
                driver.sendMessage("Error: No matching ride request found");
            }
        } else {
            driver.sendMessage("Error: Customer not found");
        }
    }

    public RideRequest findRideRequestForCustomer(ClientHandler customer) {
        for (RideRequest request : rideRequests) {
            if (request.getCustomer().equals(customer)) {
                return request;
            }
        }
        return null;
    }

    public void sendRideRequests(ClientHandler driver) {
        if (rideRequests.isEmpty()) {
            driver.sendMessage("No ride requests available");
            driver.sendMessage("");
        } else {
            driver.sendMessage("Available Ride Requests:");
            for (RideRequest request : rideRequests) {
                driver.sendMessage("Customer: " + request.getCustomer().username + 
                                   " | Pickup: " + request.getPickupLocation() + 
                                   " | Destination: " + request.getDestination());
            }
            driver.sendMessage("");
        }
    }

    public void rejectRideOffer(ClientHandler customer, int driverId) {
        ClientHandler driver = drivers.get(driverId);
        if (driver != null) {
            driver.sendMessage("Ride offer rejected by customer " + customer.username);
        } else {
            customer.sendMessage("Error: Driver not found");
        }
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
                customer.sendMessage("Ride completed by driver " + driver.username);
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
    public void removeCustomer(ClientHandler customer) {
        customers.values().removeIf(ch -> ch.equals(customer));
    }
    
    public ClientHandler getDriverByUsername(String username) {
        for (ClientHandler driver : drivers.values()) {
            if (driver.username.equals(username)) {
                return driver;
            }
        }
        return null;
    }
    
    public void removeRideRequest(RideRequest rideRequest) {
        rideRequests.remove(rideRequest);
    }
    
    public void addActiveRide(Ride ride) {
        activeRides.add(ride);
    }
    
    public void redistributeRideRequest(RideRequest rideRequest) {
        for (ClientHandler driver : drivers.values()) {
            driver.sendMessage("Redistributed Ride Request from " + 
                               rideRequest.getCustomer().username + 
                               " | Pickup: " + rideRequest.getPickupLocation() + 
                               " | Destination: " + rideRequest.getDestination());
        }
    }
    
    public Ride findActiveRideForCustomer(ClientHandler customer) {
        for (Ride ride : activeRides) {
            if (ride.getCustomer().equals(customer)) {
                return ride;
            }
        }
        return null;
    }
}