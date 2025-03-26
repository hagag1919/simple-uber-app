package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private UberServer server;
    private int clientId;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

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
        String username = in.readLine();
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
                    server.requestRide(this, pickupLocation, destination);
                } else if ("disconnect".equalsIgnoreCase(request)) {
                    server.removeCustomer(this);
                    socket.close();
                    break;
                } else if ("accept offer".equalsIgnoreCase(request)) {
                    String driverUsername = in.readLine();
                    int driverId = Integer.parseInt(in.readLine());
                    int fare = Integer.parseInt(in.readLine());
                    
                    RideRequest rideRequest = server.findRideRequestForCustomer(this);
                    if (rideRequest != null) {
                        Ride newRide = new Ride(
                            server.getDriverByUsername(driverUsername), 
                            this, 
                            rideRequest.getPickupLocation(), 
                            rideRequest.getDestination(), 
                            fare
                        );
                        
                        server.removeRideRequest(rideRequest);
                        
                        server.addActiveRide(newRide);
                        
                        sendMessage("Ride accepted with driver " + driverUsername);
                        newRide.getDriver().sendMessage("Ride confirmed by customer " + getUsername());
                    } else {
                        sendMessage("Error: No matching ride request found");
                    }
                } else if ("reject offer".equalsIgnoreCase(request)) {
                    String driverUsername = in.readLine();
                    
                    ClientHandler driver = server.getDriverByUsername(driverUsername);
                    if (driver != null) {
                        driver.sendMessage("Ride offer rejected by customer " + getUsername());
                        
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
                        sendMessage("Driver: " + activeRide.getDriver().getUsername());
                        sendMessage("Pickup: " + activeRide.getPickupLocation());
                        sendMessage("Destination: " + activeRide.getDestination());
                        sendMessage("Fare: " + activeRide.getFare());
                    } else {
                        sendMessage("No active ride found");
                    }
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
                if ("view ride requests".equalsIgnoreCase(request)) {
                    server.sendRideRequests(this);
                } else if (request.startsWith("offer ride")) {
                    String[] parts = request.split(" ");
                    if (parts.length == 3) {
                        String customerId = parts[2];
                        int fare = Integer.parseInt(in.readLine());
                        server.handleRideOffer(this, customerId, fare);
                    } else {
                        sendMessage("Invalid offer ride request format");
                    }
                } else if ("disconnect".equalsIgnoreCase(request)) {
                    socket.close();
                    break;
                } else {
                    sendMessage("Unknown request");
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
    }

    public String getUsername() {
        return username;
    }
}