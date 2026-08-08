package com.atlas.core;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnectivityTest {

    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .load();

        String url = dotenv.get("DB_URL");
        String username = dotenv.get("DB_USERNAME");
        String password = dotenv.get("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("Connected to: " + url);

            ResultSet rs = stmt.executeQuery(
                    "SELECT column_name, data_type FROM information_schema.columns "
                    + "WHERE table_name = 'chunks' ORDER BY ordinal_position");

            System.out.println("\nchunks table columns:");
            while (rs.next()) {
                System.out.printf("  %-15s %s%n", rs.getString("column_name"), rs.getString("data_type"));
            }
        }
    }
}
