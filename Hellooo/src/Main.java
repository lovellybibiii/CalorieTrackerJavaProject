import javax.swing.*;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        // Database configuration
        String url = "jdbc:mysql://localhost:3306/calorie_tracker";
        String user = "root";
        String password = "Bothina20051234567"; // Change to your password

        Connection connection = null;
        boolean connected = false;

        try {
            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully!");

            // Try to connect
            connection = DriverManager.getConnection(url, user, password);
            connected = true;
            System.out.println("Successfully connected to MySQL database!");

            // Test connection
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATABASE()");
            if (rs.next()) {
                System.out.println("Connected to database: " + rs.getString(1));
            }
            stmt.close();

        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found!");
            JOptionPane.showMessageDialog(null,
                    "MySQL Driver not found!\n" +
                            "Please add mysql-connector-java.jar to your classpath.\n" +
                            "Application will run in local mode.",
                    "Driver Error", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Failed to connect to database:\n" +
                            "Error: " + e.getMessage() + "\n\n" +
                            "Application will run in local mode.",
                    "Connection Error", JOptionPane.WARNING_MESSAGE);
        }

        final boolean isConnected = connected;
        final Connection finalConnection = connection;

        // Run GUI
        SwingUtilities.invokeLater(() -> {
            CalorieTrackerGUI gui = new CalorieTrackerGUI(finalConnection);
            gui.setVisible(true);

            if (isConnected) {
                JOptionPane.showMessageDialog(gui,
                        "Welcome to Calorie Tracker \n\n" +
                                "HOW TO USE:\n" +
                                "1. Select food from the dropdown list\n" +
                                "2. Enter quantity in grams\n" +
                                "3. Calories are calculated automatically\n" +
                                "4. Select meal type and click 'Add Meal'\n\n" ,
                        "Welcome", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(gui,
                        "Running in local mode!\n" +
                                "Your meals will only be saved during this session.",
                        "Local Mode", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Shutdown hook to close connection
        Connection finalConnection1 = connection;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (finalConnection1 != null) {
                try {
                    finalConnection1.close();
                    System.out.println("Database connection closed on shutdown");
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }));
    }
}