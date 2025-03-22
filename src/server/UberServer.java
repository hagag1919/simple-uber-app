package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class UberServer implements Runnable {
    private int port = 6666;
    private ServerSocket serverSocket;
    private Map<Integer, ClientHandler> customers = new HashMap<>();
    private Map<Integer, ClientHandler> drivers = new HashMap<>();
    private int clientIdCounter = 0;

    public UberServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);
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

    public synchronized int getNextClientId() {
        return clientIdCounter++;
    }

    public synchronized void addCustomer(int id, ClientHandler clientHandler) {
        customers.put(id, clientHandler);
    }

    public synchronized void addDriver(int id, ClientHandler clientHandler) {
        drivers.put(id, clientHandler);
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