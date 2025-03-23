package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AdminInterface {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Scanner scanner;

    public AdminInterface(String serverAddress, int port) throws IOException {
        this.socket = new Socket(serverAddress, port);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        try {
            System.out.println("Admin Login");
            System.out.print("Enter username: ");
            String username = scanner.nextLine();
            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            out.println("login");
            out.println(username);
            out.println(password);

            String response = in.readLine();
            System.out.println(response);

            if ("Login successful".equals(response)) {
                handleAdminMenu();
            } else {
                System.out.println("Invalid credentials");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAdminMenu() throws IOException {
        while (true) {
            System.out.println("Admin Menu:");
            System.out.println("1. View statistics");
            System.out.println("2. Disconnect");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();
            out.println(choice);

            if ("1".equals(choice)) {
                viewStatistics();
            } else if ("2".equals(choice)) {
                closeConnection();
                break;
            } else {
                System.out.println("Invalid option, try again.");
            }
        }
    }

    private void viewStatistics() throws IOException {
        out.println("get stats");
        System.out.println("Statistics:");
        System.out.println(in.readLine());
        System.out.println(in.readLine());
        System.out.println(in.readLine());
    }

    private void closeConnection() throws IOException {
        out.println("disconnect");
        socket.close();
        System.out.println("Disconnected from server.");
    }

    public static void main(String[] args) {
        try {
            AdminInterface admin = new AdminInterface("localhost", 12345);
            admin.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}