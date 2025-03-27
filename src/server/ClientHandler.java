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
                if ("request ride".equalsIgnoreCase(request)) {
                    String pickupLocation = in.readLine();
                    String destination = in.readLine();
                    server.requestRide(this, pickupLocation, destination, username);

                    sendMessage("Ride request sent");
                    
                } else if ("disconnect".equalsIgnoreCase(request)) {
                    if (inRide) {
                        sendMessage("You are in a ride, you cannot disconnect");
                    } else {
                        server.removeCustomer(this);
                        socket.close();
                        break;
                    }
                } else if ("accept offer".equalsIgnoreCase(request)) {
                    String driverUsername = in.readLine();
                    int fare = Integer.parseInt(in.readLine());

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

                        server.removeRideRequest(rideRequest);
                        server.addActiveRide(newRide);

                        sendMessage("Ride accepted with driver " + driverUsername);
                        newRide.getDriver().sendMessage("Ride confirmed by customer " + username);
                    } else {
                        sendMessage("Error: No matching ride request found");
                    }
                } else if ("reject offer".equalsIgnoreCase(request)) {
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
                } else if ("check ride status".equalsIgnoreCase(request)) {
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
                } else if ("start ride".equalsIgnoreCase(request)) {
                    server.startRide(this, server.getCustomerByUsername(username));
                } else if ("complete ride".equalsIgnoreCase(request)) {
                    server.completeRide(this);
                } else if ("rate driver".equalsIgnoreCase(request)) {
                    handleRateDriver();
                } else if ("view offers".equalsIgnoreCase(request)) {
                    List<Offer> offers = server.getOffersForCustomer(this);
                    if (offers.isEmpty()) {
                        sendMessage("No offers available.");
                    } else {
                        for (Offer offer : offers) {
                            sendMessage("Offer from driver " + offer.getDriverName() + " with fare: " + offer.getFare());
                        }
                        sendMessage("1. Accept offer\n2. Reject offer");
                        String offerChoice = in.readLine();
                        if ("1".equals(offerChoice)) {
                            sendMessage("Enter driver username to accept offer: ");
                            String driverUsername = in.readLine();
                            out.println("accept offer");
                            out.println(driverUsername);
                            sendMessage(in.readLine());
                        } else if ("2".equals(offerChoice)) {
                            sendMessage("Enter driver username to reject offer: ");
                            String driverUsername = in.readLine();
                            out.println("reject offer");
                            out.println(driverUsername);
                            sendMessage(in.readLine());
                        } else {
                            sendMessage("Invalid option.");
                        }
                    }

                } else {
                    sendMessage("Unknown request");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            server.removeCustomer(this);
        }
    }
    private void handleRateDriver() throws IOException {
        sendMessage("Enter driver username: ");
        String driverUsername = in.readLine();
        sendMessage("Enter rating (1-5): ");
        int rating = Integer.parseInt(in.readLine());
        sendMessage("Enter comments: ");
        String comments = in.readLine();

        Review review = new Review(username, rating, comments);
        server.addDriverReview(driverUsername, review);
        sendMessage("Thank you for rating the driver!");

        server.displayDriverRating(this, driverUsername);
    }
    private void handleDriver() {
        try {
            while (true) {
                String request = in.readLine();
                if ("1".equalsIgnoreCase(request)) {
                    server.sendRideRequests(this);
                    // wait for the driver next action
                    continue;
                } else if ("2".equalsIgnoreCase(request)) {
                    sendMessage("Enter customer username to offer ride");
                    String customerUsername = in.readLine();
                    sendMessage("Enter fare amount");
                    int fare = Integer.parseInt(in.readLine());
                    if(!server.isValidCustomer(customerUsername))
                    {
                        sendMessage("Invalid customer username");
                        return;
                    }
                    Offer offer = new Offer(this.getUsername(), fare, customerUsername);
                    server.addOffer(offer);
                    sendMessage("Offer sent to customer " + customerUsername);
                    // if (parts.length == 3) {
                    //     String customerId = parts[2];
                    //     int fare = Integer.parseInt(in.readLine());
                    //     server.handleRideOffer(this, customerId, fare);
                    // } else {
                    //     sendMessage("Invalid offer ride request format");
                    // }
                    continue;
                } else if("3".equalsIgnoreCase(request)) {
                    Ride ride = server.getCurrentRideByDriver(this);
                    if (ride != null) {
                        sendMessage("Customer: " + ride.getCustomer().getUsername() + "accepted your offer");
                        sendMessage("Ride Details:");
                        sendMessage("Customer: " + ride.getCustomer().getUsername());
                        sendMessage("Pickup: " + ride.getPickupLocation());
                        sendMessage("Destination: " + ride.getDestination());
                        sendMessage("Fare: " + ride.getFare());
                    } else {
                        sendMessage("No active ride found");
                    }
                } else if ("4".equalsIgnoreCase(request)) {
                    String rideChoice = in.readLine();
                    if ("1".equalsIgnoreCase(rideChoice)) {
                        out.println("start ride");
                        sendMessage("Enter customer ID to start ride");
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

                } else if ("5".equalsIgnoreCase(request)) {
                    if(inRide) {
                        sendMessage("You are in a ride, you cannot disconnect");
                    } else {
                        socket.close();
                        break;
                    }

                } else {
                    sendMessage("Unknown request");
                    continue;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    public void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    public String getUsername() {
        return username;
    }
}