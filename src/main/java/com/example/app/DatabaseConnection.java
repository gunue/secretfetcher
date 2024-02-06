package com.example.app;

import com.zaxxer.hikari.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

import java.util.logging.Logger;

public class DatabaseConnection {
    // Logger
    private static final Logger logger = Logger.getLogger(DatabaseConnection.class.getName());

    // Singleton instance
    private static DatabaseConnection instance;

    // Connection parameters
    private static final String DATABASE_URL = "jdbc:postgresql://postgres:5432/test_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = SecretFetcher.getConjurInstance().getSecret("secretsdev/app_pass");
    
    // HikariCP connection pool
    private HikariDataSource ds;

    public DatabaseConnection() {
        connect();
    }

    // Singleton access method
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            logger.info("Creating a new instance of DatabaseConnection.");
            instance = new DatabaseConnection();
        } else {
            logger.info("Using existing instance of DatabaseConnection.");
        }
        return instance;
    }

    private void connect() {
        logger.info("Entering connect function");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DATABASE_URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);

        config.validate();

        ds = new HikariDataSource(config);
        logger.info("Exiting connect function");
    }

    // Function to test the connection
    public void testConnection() {
        System.out.println("Entering testConnection function");
        try (Connection connection = ds.getConnection()) {
            System.out.println("Database connected!");
            System.out.println("Exiting testConnection function");
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }
    }

    // Function to get data from the database
    public List<String> getData() {
        System.out.println("Entering getData function");
        List<String> data = new ArrayList<>();
        String sql = "SELECT * FROM test_table";
    
        try (Connection connection = ds.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
    
            // loop through the result set
            while (rs.next()) {
                String name = rs.getString("name");
                String value = rs.getString("value"); // Corrected data type
                data.add(name + "\t" + value);
                System.out.println("Retrieved row: " + name + "\t" + value); // Additional logging
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception: " + e.getMessage()); // Error logging
        }
        System.out.println("Exiting getData function");
        return data;
    }  
}
