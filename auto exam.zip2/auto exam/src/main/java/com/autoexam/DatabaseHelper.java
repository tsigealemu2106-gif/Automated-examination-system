package com.autoexam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    
    // SQLite automatically creates this file in your project folder! No server needed.
    private static final String URL = "jdbc:sqlite:autoexam_database.db";

    // 1. Establish the JDBC Connection
    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("JDBC Connection Error: " + e.getMessage());
        }
        return conn;
    }

    // 2. Create the SQL Tables if they don't exist
    public static void initializeDatabase() {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "username TEXT PRIMARY KEY, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            
            // Execute the SQL command
            stmt.execute(createUsersTable);
            System.out.println("JDBC Success: Database and Tables are ready!");
            
        } catch (SQLException e) {
            System.out.println("Table Creation Error: " + e.getMessage());
        }
    }

    // 3. Example of an SQL INSERT using PreparedStatements
    public static void testInsertUser(String username, String passwordHash, String role) {
        String sql = "INSERT INTO users(username, password_hash, role) VALUES(?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, role);
            pstmt.executeUpdate();
            
            System.out.println("SQL INSERT Success: Added " + username);
            
        } catch (SQLException e) {
            System.out.println("Insert Error: " + e.getMessage());
        }
    }
}