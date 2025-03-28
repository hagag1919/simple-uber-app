package server;

import client.Offer;
import client.Review;
import client.RideStatus;

import java.io.*;
import java.net.*;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private UberServer server;
    private int clientId;
    private BufferedReader in;
    private PrintWriter out;
    public  String username;
    public  boolean inRide = false;

    public ClientHandler(Socket socket, UberServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.clientId = server.getNextClientId();
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                String action = in.readLine();
                System.out.println("Received action: " + action);

                if ("register".equalsIgnoreCase(action)) {
                    handleRegistration();
                } else if ("login".equalsIgnoreCase(action)) {
                    handleLogin();
                }
                else if ("disconnect".equalsIgnoreCase(action)) {
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRegistration() throws IOException {
        String type = in.readLine();
        this.username = in.readLine();
        String password = in.readLine();


        System.out.println("Registration attempt: " + username + ", type: " + type);

        if (server.registerUser(username, password, type)) {
            out.println("Registration successful");
            if("customer".equalsIgnoreCase(type)) {
                server.addCustomer(clientId, this);
                handleCustomer();
            } else if("driver".equalsIgnoreCase(type)) {
                server.addDriver(clientId, this);
                handleDriver();
            }
        } else {
            out.println("Username already exists");
        }
    }

    private void handleLogin() throws IOException {
        String username = in.readLine();
        String password = in.readLine();

        System.out.println("Login attempt: " + username);

        if (server.authenticateUser(username, password)) {
            this.username = username;
            String userType = server.getUserType(username);
            out.println("Login successful");
            out.println(userType);

            if ("customer".equalsIgnoreCase(userType)) {
                System.out.println("Customer logged in: " + username);
                handleCustomer();
            } else if ("driver".equalsIgnoreCase(userType)) {
                System.out.println("Driver logged in: " + username);
                handleDriver();
            } else if ("admin".equalsIgnoreCase(userType)) {
                System.out.println("Admin logged in: " + username);
                handleAdmin();
            }
        } else {
            out.println("Invalid credentials");
        }
    }

    private void handleCustomer() {
        try {
            while (true) {
                String request = in.readLine();
                switch (request) {
                    case "1":
                        handleRideRequest();
                        break;
                    case "2":
                        handleRideStatus();
                        break;
                    case "3":
                        handleOffers();
                        break;
                    case "4":
                        handleRateDriver();
                        break;
                    case "5":
                        handleCustomerDisconnect();
                        return;
                    default:
                        sendMessage("Unknown request");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            server.removeCustomer(this);
        }
    }

    private void handleDriver() {
        try {
            while (true) {
                String request = in.readLine();
                switch (request) {
                    case "1":
                        server.sendRideRequests(this);
                        break;
                    case "2":
                        handleOfferRide();
                        break;
                    case "3":
                        handleDriverRideStatus();
                        break;
                    case "4":
                        handleUpdateRideStatus();
                        break;
                    case "5":
                        handleDriverDisconnect();
                        return;
                    default:
                        sendMessage("Unknown request");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRideRequest() throws IOException {
        String pickupLocation = in.readLine();
        String destination = in.readLine();
        server.requestRide(this, pickupLocation, destination, username);
        sendMessage("Ride request sent");
    }

    private void handleRideStatus() throws IOException {
        Ride activeRide = server.findActiveRideForCustomer(this);
        if (activeRide != null) {
            sendMessage("Current Ride Status:");
            sendMessage("Driver: " + activeRide.getDriver().username);
            sendMessage("Pickup: " + activeRide.getPickupLocation());
            sendMessage("Destination: " + activeRide.getDestination());
            sendMessage("Fare: " + activeRide.getFare());
        } else {
            sendMessage("No active ride found");
            sendMessage("");
        }
    }

    private void handleOffers() throws IOException {
        List<Offer> offers = server.getOffersForCustomer(this);
        if (offers.isEmpty()) {
            sendMessage("No offers available.");
            sendMessage("");
        } else {
            for (Offer offer : offers) {
                sendMessage("Offer from driver " + offer.getDriverName() + " with fare: " + offer.getFare());
            }
            String offerChoice = in.readLine();
            if ("1".equals(offerChoice)) {
                acceptOffer();
            } else if ("2".equals(offerChoice)) {
                rejectOffer();
            } else {
                sendMessage("Invalid option.");
            }
        }
    }

    private void acceptOffer() throws IOException {
        String driverUsername = in.readLine();
        int fare = server.getFareByDriverID(driverUsername, this.getUsername());
        if (fare == -1) {
            sendMessage("Invalid driver username");
            return;
        }
        RideRequest rideRequest = server.findRideRequestForCustomer(this);
        if (rideRequest != null) {
            Ride newRide = new Ride(
                    server.getDriverByUsername(driverUsername),
                    this,
                    rideRequest.getPickupLocation(),
                    rideRequest.getDestination(),
                    fare,
                    RideStatus.IN_PROGRESS
            );
            ClientHandler driver = server.getCustomerByUsername(driverUsername);
            this.inRide = true;
            driver.inRide = true;
            server.removeRideRequest(rideRequest);
            server.addActiveRide(newRide);
            sendMessage("Ride accepted with driver " + driverUsername);
            newRide.getDriver().sendMessage("Ride confirmed by customer " + username);
        } else {
            sendMessage("Error: No matching ride request found");
        }
        sendMessage(in.readLine());
    }

    private void rejectOffer() throws IOException {
        String driverUsername = in.readLine();
        ClientHandler driver = server.getDriverByUsername(driverUsername);
        if (driver != null) {
            driver.sendMessage("Ride offer rejected by customer " + username);
            RideRequest rideRequest = server.findRideRequestForCustomer(this);
            if (rideRequest != null) {
                server.redistributeRideRequest(rideRequest);
            }
        }
        sendMessage("Ride offer rejected");
    }

    private void handleCustomerDisconnect() throws IOException {
        if (inRide) {
            sendMessage("You are in a ride, you cannot disconnect");
        } else {
            server.removeCustomer(this);
            socket.close();
        }
    }

    private void handleOfferRide() throws IOException {
        String customerUsername = in.readLine();
        int fare = Integer.parseInt(in.readLine());
        if (!server.isValidCustomer(customerUsername)) {
            sendMessage("Invalid customer username");
            return;
        }
        Offer offer = new Offer(this.getUsername(), fare, customerUsername);
        server.addOffer(offer);
        sendMessage("Offer sent to customer " + customerUsername);
    }

    private void handleDriverRideStatus() throws IOException {
        Ride ride = server.getCurrentRideByDriver(this);
        if (ride != null) {
            sendMessage("Customer: " + ride.getCustomer().getUsername() + " accepted your offer");
            sendMessage("Ride Details:");
            sendMessage("Customer: " + ride.getCustomer().getUsername());
            sendMessage("Pickup: " + ride.getPickupLocation());
            sendMessage("Destination: " + ride.getDestination());
            sendMessage("Fare: " + ride.getFare());
        } else {
            sendMessage("No active ride found");
        }
    }

    private void handleUpdateRideStatus() throws IOException {
        String rideChoice = in.readLine();
        if ("1".equalsIgnoreCase(rideChoice)) {
            out.println("start ride");
            String customerId = in.readLine();
            ClientHandler customer = server.getCustomerByUsername(customerId);
            server.startRide(this, customer);
            sendMessage("Ride started with customer " + customer.getUsername());
        } else if ("2".equalsIgnoreCase(rideChoice)) {
            out.println("end ride");
            server.completeRide(this);
            sendMessage("Ride completed");
        } else {
            sendMessage("Invalid option, try again.");
        }
    }

    private void handleDriverDisconnect() throws IOException {
        if (inRide) {
            sendMessage("You are in a ride, you cannot disconnect");
        } else {
            socket.close();
        }
    }

    private void handleAdmin() {
        try {
            while (true) {
                String request = in.readLine();
                if ("get stats".equalsIgnoreCase(request)) {
                    server.getAdminStats(this);
                } else if ("disconnect".equalsIgnoreCase(request)) {
                    socket.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleRateDriver() throws IOException {
        String driverUsername = in.readLine();
        int rating = Integer.parseInt(in.readLine());
        String comments = in.readLine();

        Review review = new Review(username, rating, comments);
        server.addDriverReview(driverUsername, review);
        sendMessage("Thank you for rating the driver!");

        server.displayDriverRating(this, driverUsername);
    }

    public void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    public String getUsername() {
        return username;
    }
}