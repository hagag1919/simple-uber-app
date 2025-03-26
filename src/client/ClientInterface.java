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
        out.println("register");
        System.out.print("Enter user type (customer/driver): ");
        String type = scanner.nextLine();
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if(!"customer".equalsIgnoreCase(type) && !"driver".equalsIgnoreCase(type)) {
            System.out.println("Invalid user type");
            return;
        }

        out.println(type);
        out.println(username);
        out.println(password);

        String response = in.readLine();
        if("Registration successful".equals(response)) {
            System.out.println("Registration successful");
            if("customer".equalsIgnoreCase(type)) {
                handleCustomerMenu();
            } else if("driver".equalsIgnoreCase(type)) {
                handleDriverMenu();
            }
        } else {
            System.out.println("Username already exists");
        }
    }

    private void handleLogin() throws IOException {
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
            System.out.println("3. Accept ride offer");
            System.out.println("4. Reject ride offer");
            System.out.println("5. Disconnect");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            out.println(choice);
    
            if ("1".equals(choice)) {
                out.println("request ride");
                System.out.print("Enter pickup location: ");
                String pickupLocation = scanner.nextLine();
                out.println(pickupLocation);
                System.out.print("Enter destination: ");
                String destination = scanner.nextLine();
                out.println(destination);
                System.out.println(in.readLine()); 
            } else if ("2".equals(choice)) {
                out.println("check ride status");
                String response;
                while (!(response = in.readLine()).isEmpty()) {
                    System.out.println(response);
                }
            } else if ("3".equals(choice)) {
                System.out.print("Enter driver username to accept: ");
                String driverUsername = scanner.nextLine();
                out.println("accept offer");
                out.println(driverUsername);
                
                System.out.print("Enter driver ID: ");
                String driverId = scanner.nextLine();
                out.println(driverId);
                
                System.out.print("Enter fare amount: ");
                String fare = scanner.nextLine();
                out.println(fare);
                
                System.out.println(in.readLine());
            } else if ("4".equals(choice)) {
                System.out.print("Enter driver username to reject: ");
                String driverUsername = scanner.nextLine();
                out.println("reject offer");
                out.println(driverUsername);
                System.out.println(in.readLine());
            } else if ("5".equals(choice)) {
                closeConnection();
                break;
            } else {
                System.out.println("Invalid option, try again.");
            }
        }
    }

    private void handleDriverMenu() throws IOException {
        while (true) {
            System.out.println("Driver Menu:");
            System.out.println("1. View ride requests");
            System.out.println("2. Offer a fare");
            System.out.println("3. Disconnect");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            out.println(choice);

            if ("1".equals(choice)) {
                String response;
                while (!(response = in.readLine()).isEmpty()) {
                    System.out.println(response);
                }
            } else if ("2".equals(choice)) {
                System.out.print("Enter customer username: ");
                String customerId = scanner.nextLine();
                out.println("offer ride " + customerId);
                System.out.print("Enter fare amount: ");
                String fare = scanner.nextLine();
                out.println(fare);
                System.out.println(in.readLine());
            } else if ("3".equals(choice)) {
                closeConnection();
                break;
            } else {
                System.out.println("Invalid option, try again.");
            }
        }
    }

    private void closeConnection() throws IOException {
        out.println("disconnect");
        socket.close();
        System.out.println("Disconnected from server.");
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