package server;

import client.Offer;
import client.Review;
import client.RideStatus;

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
    private List<Ride> totalRide = new ArrayList<>();
    private List<RideRequest> rideRequests = new ArrayList<>();

    private Map<String, List<Review>> driverReviews = new HashMap<>();


    private List<Offer> offers = new ArrayList<>();

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
        rideRequests.get(rideRequests.size()-1).printData();
    }

//    public void handleRideOffer(ClientHandler driver, String customerId, int fare) {
//        ClientHandler customer = null;
//        for (ClientHandler c : customers.values()) {
//            if (c.username.equals(customerId)) {
//                customer = c;
//                break;
//            }
//        }
//
//        if (customer != null) {
//            RideRequest matchingRequest = findRideRequestForCustomer(customer);
//            if (matchingRequest != null) {
//                customer.sendMessage("Driver " + driver.username +
//                                     " offers ride for " + fare +
//                                     " | Pickup: " + matchingRequest.getPickupLocation() +
//                                     " | Destination: " + matchingRequest.getDestination());
//            } else {
//                driver.sendMessage("Error: No matching ride request found");
//            }
//        } else {
//            driver.sendMessage("Error: Customer not found");
//        }
//    }

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

    public void rejectRideOffer(ClientHandler customer) {
        RideRequest rideRequest = findRideRequestForCustomer(customer);
        if (rideRequest == null) {
            customer.sendMessage("Error: No matching ride request found");
        } else {
            rideRequests.remove(rideRequest);
            redistributeRideRequest(rideRequest);
            customer.sendMessage("Ride offer rejected");
        }
    }

    public void completeRide(ClientHandler driver) {
        Ride completedRide = null;
        for (Ride ride : totalRide) {
            if (ride.getDriver().equals(driver) && ride.getStatus() == RideStatus.IN_PROGRESS) {
                completedRide = ride;
                break;
            }
        }

        if(completedRide == null) {
            driver.sendMessage("Error: No active ride found");
            return;
        }

        completedRide.updateStatus(RideStatus.COMPLETED);
        completedRide.getCustomer().sendMessage("Ride completed. Thank you for riding with us. Fare: ");
        completedRide.getDriver().sendMessage("Ride completed. Fare: " + completedRide.getFare());
    }

    //startRide
    public void startRide(ClientHandler driver, ClientHandler customerId) {
        Ride ride = findActiveRideForCustomer(customerId);
        if (ride == null) {
            driver.sendMessage("Error: No active ride found");
            return;
        }
        ride.updateStatus(RideStatus.IN_PROGRESS);
        driver.sendMessage("Ride started");
        customerId.sendMessage("Ride started");

    }

    public void getAdminStats(ClientHandler admin) {
        admin.sendMessage("Total active rides: " + totalRide.size());
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
        totalRide.add(ride);
        ride.getDriver().inRide = true;
        ride.getCustomer().inRide = true;
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
        for (Ride ride : totalRide) {
            if (ride.getCustomer().equals(customer)) {
                return ride;
            }
        }
        return null;
    }

    public boolean addOffer(Offer offer) {
        return offers.add(offer);
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public boolean isValidCustomer(String username) {
        return customers.values().stream().anyMatch(c -> c.username.equals(username));
    }

    public RideStatus getRideStatus(ClientHandler driver)
    {
        for (Ride ride : totalRide) {
            if (ride.getDriver().equals(driver)) {
                return ride.getStatus();
            }
        }
        return null;
    }

    Ride getCurrentRideByDriver(ClientHandler driver) {
        for (Ride ride : totalRide) {
            if (ride.getDriver().equals(driver) && ride.getStatus() == RideStatus.IN_PROGRESS){
                return ride;
            }
        }
        return null;
    }
    public ClientHandler getCustomerByUsername(String username) {
        for (ClientHandler customer : customers.values()) {
            if (customer.username.equals(username)) {
                return customer;
            }
        }
        return null;
    }

    public void addDriverReview(String driverUsername, Review review) {
        driverReviews.computeIfAbsent(driverUsername, k -> new ArrayList<>()).add(review);
    }

    public double getDriverRating(String driverUsername) {
        List<Review> reviews = driverReviews.get(driverUsername);
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        int totalRating = reviews.stream().mapToInt(Review::getRating).sum();
        return (double) totalRating / reviews.size();
    }

    public void displayDriverRating(ClientHandler client, String driverUsername) {
        double rating = getDriverRating(driverUsername);
        client.sendMessage("Driver " + driverUsername + " has an overall rating of: " + rating);
    }
    //getOffersForCustomer
    public List<Offer> getOffersForCustomer(ClientHandler customer) {
        List<Offer> customerOffers = new ArrayList<>();
        for (Offer offer : offers) {
            if (offer.getCustomerUsername().equals(customer.username)) {
                customerOffers.add(offer);
            }
        }
        return customerOffers;
    }

    public int getFareByDriverID(String driverUsername, String customerUsername)
    {
        for (Offer offer : offers) {
            if (offer.getDriverName().equals(driverUsername) && offer.getCustomerUsername().equals(customerUsername)) {
                return offer.getFare();
            }
        }
        return -1;
    }
}
