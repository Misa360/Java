package com.codeanalyzer.repository;

import com.codeanalyzer.config.DatabaseConfig;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class SubmissionRepository {
    public void saveSubmission(String submissionId, String contestId, String handle, String code, 
                               String problemName, String lang, String verdict, int timeMs, int memBytes, long timeSeconds) {
        String sqlSub = "INSERT INTO submissions (submission_id, contest_id, handle, problem_name, programming_language, verdict, time_consumed_ms, memory_consumed_bytes, submission_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE problem_name = VALUES(problem_name), programming_language = VALUES(programming_language), verdict = VALUES(verdict), " +
                "time_consumed_ms = VALUES(time_consumed_ms), memory_consumed_bytes = VALUES(memory_consumed_bytes), submission_time = VALUES(submission_time)";
        
        String sqlCode = "INSERT INTO submission_codes (submission_id, source_code) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE source_code = VALUES(source_code)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Save metadata
                try (PreparedStatement pstmt = conn.prepareStatement(sqlSub)) {
                    pstmt.setString(1, submissionId);
                    pstmt.setString(2, contestId);
                    pstmt.setString(3, handle);
                    pstmt.setString(4, problemName);
                    pstmt.setString(5, lang);
                    pstmt.setString(6, verdict);
                    pstmt.setInt(7, timeMs);
                    pstmt.setInt(8, memBytes);
                    pstmt.setLong(9, timeSeconds);
                    pstmt.executeUpdate();
                }
                
                // Save code
                try (PreparedStatement pstmt = conn.prepareStatement(sqlCode)) {
                    pstmt.setString(1, submissionId);
                    pstmt.setString(2, code);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
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
        // Join with ai_analyses to get the latest analysis result for each submission
        String sql = "SELECT s.submission_id, s.handle, s.contest_id, s.problem_name, s.programming_language, s.verdict, " +
                     "s.time_consumed_ms, s.memory_consumed_bytes, s.submission_time, a.analysis_details as ai_analysis " +
                     "FROM submissions s " +
                     "LEFT JOIN (SELECT submission_id, MAX(id) as max_id FROM ai_analyses GROUP BY submission_id) latest_a " +
                     "ON s.submission_id = latest_a.submission_id " +
                     "LEFT JOIN ai_analyses a ON latest_a.max_id = a.id " +
                     "ORDER BY s.submission_time DESC";
        return conn.createStatement().executeQuery(sql);
    }

    public String getCodeById(String subId) {
        String sql = "SELECT source_code FROM submission_codes WHERE submission_id = ?";
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
        String sql = "INSERT INTO ai_analyses (submission_id, ai_probability, analysis_details) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            double probability = 0.0;
            try {
                JSONObject json = new JSONObject(result);
                probability = json.optDouble("ai_probability", 0.0);
            } catch (Exception e) {
                // Not a valid JSON or missing probability
            }
            
            pstmt.setString(1, subId);
            pstmt.setDouble(2, probability);
            pstmt.setString(3, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Object[]> getUserRankings() {
        List<Object[]> rankings = new ArrayList<>();
        // Lấy bài nộp và phân tích AI mới nhất của mỗi bài nộp
        String sql = "SELECT s.handle, a.analysis_details " +
                     "FROM submissions s " +
                     "LEFT JOIN (SELECT submission_id, MAX(id) as max_id FROM ai_analyses GROUP BY submission_id) latest_a " +
                     "ON s.submission_id = latest_a.submission_id " +
                     "LEFT JOIN ai_analyses a ON latest_a.max_id = a.id";
        
        java.util.Map<String, Integer> countMap = new java.util.HashMap<>();
        java.util.Map<String, Double> totalAiMap = new java.util.HashMap<>();
        java.util.Map<String, Integer> analyzedCountMap = new java.util.HashMap<>();

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String handle = rs.getString("handle");
                String aiRaw = rs.getString("analysis_details");
                
                countMap.put(handle, countMap.getOrDefault(handle, 0) + 1);
                
                if (aiRaw != null && aiRaw.startsWith("{")) {
                    try {
                        JSONObject json = new JSONObject(aiRaw);
                        if (json.has("ai_probability")) {
                            totalAiMap.put(handle, totalAiMap.getOrDefault(handle, 0.0) + json.getDouble("ai_probability"));
                            analyzedCountMap.put(handle, analyzedCountMap.getOrDefault(handle, 0) + 1);
                        }
                    } catch (Exception e) {
                        // Bỏ qua lỗi JSON
                    }
                }
            }
            
            List<String> handles = new ArrayList<>(countMap.keySet());
            handles.sort((h1, h2) -> countMap.get(h2).compareTo(countMap.get(h1)));
            
            for (String handle : handles) {
                int count = countMap.get(handle);
                int analyzedCount = analyzedCountMap.getOrDefault(handle, 0);
                double totalAi = totalAiMap.getOrDefault(handle, 0.0);
                
                String aiStr = (analyzedCount > 0) ? String.format("%.1f%%", totalAi / analyzedCount) : "N/A";
                rankings.add(new Object[]{handle, count, aiStr});
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rankings;
    }

    public List<String> getAiAnalysesForUser(String handle) {
        List<String> analyses = new ArrayList<>();
        String sql = "SELECT a.analysis_details FROM ai_analyses a " +
                     "JOIN submissions s ON a.submission_id = s.submission_id " +
                     "WHERE s.handle = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, handle);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String ai = rs.getString("analysis_details");
                    if (ai != null && !ai.trim().isEmpty() && ai.startsWith("{")) {
                        analyses.add(ai);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return analyses;
    }

    public Object[] getDashboardStats() {
        String sqlUsers = "SELECT COUNT(*) FROM users";
        String sqlSubmissions = "SELECT COUNT(*) FROM submissions";
        String sqlAnalyzed = "SELECT COUNT(DISTINCT submission_id) FROM ai_analyses";
        
        Object[] stats = {0, 0, 0};
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery(sqlUsers);
            if (rs.next()) stats[0] = rs.getInt(1);
            
            rs = stmt.executeQuery(sqlSubmissions);
            if (rs.next()) stats[1] = rs.getInt(1);
            
            rs = stmt.executeQuery(sqlAnalyzed);
            if (rs.next()) stats[2] = rs.getInt(1);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
}

