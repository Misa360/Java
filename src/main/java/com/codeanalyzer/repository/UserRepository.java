package com.codeanalyzer.repository;

import com.codeanalyzer.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    public void addUser(String handle, String platform) {
        String sql = "INSERT IGNORE INTO users (handle, platform) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, handle);
            pstmt.setString(2, platform);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<com.codeanalyzer.model.User> getAllUsersFull() {
        List<com.codeanalyzer.model.User> users = new ArrayList<>();
        String sql = "SELECT handle, platform, last_crawled_at FROM users";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new com.codeanalyzer.model.User(
                    rs.getString("handle"),
                    rs.getString("platform"),
                    rs.getString("last_crawled_at") != null ? rs.getString("last_crawled_at") : "Chưa crawl"
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
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

    public void deleteUser(String handle) {
        String sql = "DELETE FROM users WHERE handle = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, handle);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLastCrawlTime(String handle) {
        String sql = "UPDATE users SET last_crawled_at = NOW() WHERE handle = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, handle);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
