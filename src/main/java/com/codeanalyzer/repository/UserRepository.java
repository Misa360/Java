package com.codeanalyzer.repository;

import com.codeanalyzer.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    public void addUser(String handle) {
        String sql = "INSERT IGNORE INTO users (handle) VALUES (?)";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, handle);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        String sql = "SELECT handle FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(rs.getString("handle"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}
