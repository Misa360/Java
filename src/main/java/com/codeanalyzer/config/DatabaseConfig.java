package com.codeanalyzer.config;

import com.codeanalyzer.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String URL = AppConfig.get("db.url", "jdbc:mysql://localhost:3306/code_analyzer");
    private static final String USER = AppConfig.get("db.user", "root");
    private static final String PASS = AppConfig.get("db.pass", "Buingocanh@234");

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
