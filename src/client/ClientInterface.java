package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClientInterface {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    public ClientInterface(String serverAddress, int port) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            System.out.println("Welcome to Uber!");
            while (true) {
                System.out.println("1. Register\n2. Login\n3. Exit");
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine();
                out.println(choice);

                switch (choice) {
                    case "1":
                        handleRegistration();
                        break;
                    case "2":
                        handleLogin();
                        break;
                    case "3":
                        closeConnection();
                        return;
                    default:
                        System.out.println("Invalid option, try again.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleRegistration() throws IOException {
        if (socket.isClosed()) {
            this.socket = new Socket("localhost", 12345);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }
        out.println("register");
        System.out.print("Enter user type (customer/driver): ");
        String type = scanner.nextLine();
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (!"customer".equalsIgnoreCase(type) && !"driver".equalsIgnoreCase(type)) {
            System.out.println("Invalid user type");
            return;
        }

        out.println(type);
        out.println(username);
        out.println(password);

        String response = in.readLine();
        if ("Registration successful".equals(response)) {
            System.out.println("Registration successful");
            if ("customer".equalsIgnoreCase(type)) {
                handleCustomerMenu();
            } else if ("driver".equalsIgnoreCase(type)) {
                handleDriverMenu();
            }
        } else {
            System.out.println("Username already exists");
        }
    }

    private void handleLogin() throws IOException {
        if (socket.isClosed()) {
            this.socket = new Socket("localhost", 12345);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        out.println("login");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        out.println(username);
        out.println(password);

        String response = in.readLine();
        System.out.println(response);

        if ("Login successful".equals(response)) {
            String userType = in.readLine();
            if ("customer".equalsIgnoreCase(userType)) {
                handleCustomerMenu();
            } else if ("driver".equalsIgnoreCase(userType)) {
                handleDriverMenu();
            }
        }
    }

    private void handleCustomerMenu() throws IOException {
        while (true) {
            System.out.println("Customer Menu:");
            System.out.println("1. Request a ride");
            System.out.println("2. Check ride status");
            System.out.println("3. View/Respond to offers");
            System.out.println("4. Rate driver");
            System.out.println("5. Disconnect");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            out.println(choice);

            switch (choice) {
                case "1":
                    System.out.print("Enter pickup location: ");
                    String pickupLocation = scanner.nextLine();
                    out.println(pickupLocation);
                    System.out.print("Enter destination: ");
                    String destination = scanner.nextLine();
                    out.println(destination);
                    System.out.println(in.readLine());
                    break;
                case "2":
                    out.println("2");
                    String response;
                    while ((response = in.readLine()) != null && !response.isEmpty()) {
                        System.out.println(response);
                    }
                    break;
                case "3":
                    String offersResponse;
                    if ((offersResponse = in.readLine()) != null && !offersResponse.isEmpty()) {
                        System.out.println(offersResponse);
                        System.out.println("1. Accept offer\n2. Reject offer");
                        System.out.print("Choose an option: ");
                        String offerChoice = scanner.nextLine();
                        out.println(offerChoice);
                        if ("1".equals(offerChoice)) {
                            System.out.print("Enter driver username: ");
                            String driverUsername = scanner.nextLine();
                            out.println(driverUsername);
                            System.out.println(in.readLine());
                        } else if ("2".equals(offerChoice)) {
                            System.out.print("Enter driver username: ");
                            String driverUsername = scanner.nextLine();
                            out.println(driverUsername);
                            System.out.println(in.readLine());
                        } else {
                            System.out.println("Invalid option.");
                        }
                    } else {
                        System.out.println("No offers available");

                    }

                    break;
                case "4":
                    rateDriver();
                    break;
                case "5":
                    // out.println("5");
                    if (closeConnection())
                        return;
                    break;
                default:
                    System.out.println("Invalid option, try again.");
            }
        }
    }

    private void rateDriver() throws IOException {
        out.println("4");
        System.out.print("Enter driver username: ");
        String driverUsername = scanner.nextLine();
        out.println(driverUsername);
        System.out.print("Enter rating (1-5): ");
        int rating = Integer.parseInt(scanner.nextLine());
        out.println(rating);
        System.out.print("Enter comments: ");
        String comments = scanner.nextLine();
        out.println(comments);
        System.out.println(in.readLine());
    }

    private void handleDriverMenu() throws IOException {
        while (true) {
            System.out.println("Driver Menu:");
            System.out.println("1. Get ride requests");
            System.out.println("2. Offer a fare");
            System.out.println("3. View ride status");
            System.out.println("4. Update ride status");
            System.out.println("5. get account reviews");
            System.out.println("6. Disconnect");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            out.println(choice);

            switch (choice) {
                case "1":
                    String requestsResponse;
                    while (!(requestsResponse = in.readLine()).isEmpty()) {
                        System.out.println(requestsResponse);
                    }
                    break;
                case "2":
                    System.out.print("Enter customer username: ");
                    String customerUsername = scanner.nextLine();
                    out.println(customerUsername);
                    System.out.print("Enter fare amount: ");
                    int fare = Integer.parseInt(scanner.nextLine());
                    out.println(fare);
                    System.out.println(in.readLine());
                    break;
                case "3":
                    out.println("check ride status");
                    String statusResponse;
                    while ((statusResponse = in.readLine()) != null && !statusResponse.isEmpty()) {
                        System.out.println(statusResponse);
                    }

                    break;
                case "4":
                    System.out.println("1. Start ride\n2. End ride");
                    System.out.print("Choose an option: ");
                    String rideChoice = scanner.nextLine();
                    out.println(rideChoice);
                    if ("1".equals(rideChoice) || "2".equals(rideChoice)) {
                        String rideStatus;
                        while ((rideStatus = in.readLine()) != null && !rideStatus.isEmpty()) {
                            System.out.println(rideStatus);
                        }
                    } else {
                        System.out.println("Invalid option.");
                    }
                    break;
                case "5":
                    out.println("5");
                    String reviewsResponse;
                    while ((reviewsResponse = in.readLine()) != null && !reviewsResponse.isEmpty()) {
                        System.out.println(reviewsResponse);
                    }
                    break;
                case "6":

                    if (closeConnection()) {
                        return;
                    }
                    break;
                default:
                    System.out.println("Invalid option, try again.");
            }
        }
    }

    private boolean closeConnection() throws IOException {
        out.println("5");
        // wait for server to close connection
        // wait for server to send message
        String message;
        while ((message = in.readLine()) != null && !message.isEmpty()) {
            System.out.println(message);
        }
        // if(message.contains(""))
        return socket.isClosed();
    }

    public static void main(String[] args) {
        try {
            ClientInterface client = new ClientInterface("localhost", 12345);
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}