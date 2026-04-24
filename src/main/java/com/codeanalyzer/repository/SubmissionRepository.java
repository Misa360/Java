package com.codeanalyzer.repository;

import com.codeanalyzer.config.DatabaseConfig;
import java.sql.*;

public class SubmissionRepository {
    public void saveSubmission(String submissionId, String contestId, String handle, String code) {
        String sql = "INSERT INTO submissions (submission_id, contest_id, handle, source_code) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE source_code = VALUES(source_code)";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submissionId);
            pstmt.setString(2, contestId);
            pstmt.setString(3, handle);
            pstmt.setString(4, code);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isSubmissionExist(String subId) {
        String sql = "SELECT 1 FROM submissions WHERE submission_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ResultSet getAllSubmissions() throws SQLException {
        Connection conn = DatabaseConfig.getConnection();
        String sql = "SELECT submission_id, handle, contest_id, ai_analysis FROM submissions ORDER BY id DESC";
        return conn.createStatement().executeQuery(sql);
    }

    public String getCodeById(String subId) {
        String sql = "SELECT source_code FROM submissions WHERE submission_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return rs.getString("source_code");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void updateAnalysis(String subId, String result) {
        String sql = "UPDATE submissions SET ai_analysis = ? WHERE submission_id = ?";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, result);
            pstmt.setString(2, subId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
