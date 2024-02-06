package com.example.app;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    // Server instance as a static member
    private static HttpServer server;

    public static void main(String[] args) {
        logger.info("Entering main function");

        // Accessing the singleton instance of DatabaseConnection
        DatabaseConnection databaseConnection = DatabaseConnection.getInstance();

        databaseConnection.testConnection();

        startServer();

        //String secret = SecretFetcher.getConjurInstance().getSecret();
        //logger.info("Secret retrieved: " + secret);

        logger.info("Exiting main function");
    }

    // Function to start the server and expose the API endpoint
    public static void startServer() {
        logger.info("Entering startServer function");
        try {
            server = HttpServer.create(new InetSocketAddress(8000), 0);
            //server.createContext("/data", new DataHandler());
            server.createContext("/secret", new SecretHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            logger.info("Server started on port 8000");
        } catch(IOException e) {
            logger.severe("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
        logger.info("Exiting startServer function");
    }
    
// Data endpoint
static class DataHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(DataHandler.class.getName());

    @Override
    public void handle(HttpExchange t) throws IOException {
        logger.info("Entering handle function");

        // Use the singleton instance to call getData()
        List<String> data = DatabaseConnection.getInstance().getData();
        
        String response;
        if (data.isEmpty()) {
            response = "No data available";
        } else {
            response = String.join(", ", data);
        }

        t.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();

        logger.info("Exiting handle function");
    }
}
    // Secret endpoint handler
static class SecretHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(SecretHandler.class.getName());

    @Override
    public void handle(HttpExchange t) throws IOException {
        logger.info("SecretHandler invoked");
        try {
            String secret = SecretFetcher.getConjurInstance().getSecret("secretsdev/app_pass");
            String response = "Fetched secret: " + secret;
            t.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            logger.info("Response sent successfully");
        } catch (Exception e) {
            logger.severe("Error in SecretHandler: " + e.getMessage());
            e.printStackTrace();
            String response = "Error fetching secret";
            t.sendResponseHeaders(500, response.getBytes().length);
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    }
}

