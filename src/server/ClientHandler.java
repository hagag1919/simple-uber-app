package server;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private UberServer server;
    private int clientId;
    private BufferedReader in;
    private PrintWriter out;

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
            String clientType = in.readLine();
            if ("customer".equalsIgnoreCase(clientType)) {
                server.addCustomer(clientId, this);
                handleCustomer();
            } else if ("driver".equalsIgnoreCase(clientType)) {
                server.addDriver(clientId, this);
                handleDriver();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCustomer() {

    }

    private void handleDriver() {

    }
}