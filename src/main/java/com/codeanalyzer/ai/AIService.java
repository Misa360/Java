package com.codeanalyzer.ai;

import com.codeanalyzer.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIService {
    private static final String API_KEY = AppConfig.get("ai.api.key",
            "AIzaSyDfMaiT-H4anHoHwtHVcS2UQlNCftwmMOw");
    private static final String API_URL = AppConfig.get("ai.api.url",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");

    public String analyzeCode(String sourceCode) {
        try {
            if (sourceCode == null || sourceCode.length() < 10)
                return "{}";
            if (sourceCode.length() > 5000)
                sourceCode = sourceCode.substring(0, 5000);

            String prompt = "Bạn là một chuyên gia đánh giá lập trình viên. Hãy phân tích đoạn mã nguồn sau và trả về JSON chính xác theo cấu trúc:\n"
                    + "{\n"
                    + "  \"ds_algo\": \"Danh sách CTDL & thuật toán chính (VD: Segment Tree, DFS, ...)\",\n"
                    + "  \"ai_probability\": \"Xác suất (%) code này do AI/LLM viết (0-100)\",\n"
                    + "  \"skill_level\": \"Trình độ thực tế người viết (Thấp/Trung bình/Giỏi)\",\n"
                    + "  \"explanation\": \"Giải thích tại sao đánh giá như vậy\"\n"
                    + "}\n\n"
                    + "QUY TẮC QUAN TRỌNG:\n"
                    + "1. Nếu 'ai_probability' > 60%, bạn PHẢI đánh giá 'skill_level' là 'Thấp' hoặc 'Trung bình' (vì người dùng không tự viết).\n"
                    + "2. Chỉ đánh giá 'Giỏi' nếu code phức tạp, tối ưu và 'ai_probability' < 30%.\n"
                    + "3. Nếu code quá đơn giản (chỉ in ra kết quả, cộng trừ cơ bản), hãy đánh giá 'Thấp'.\n\n"
                    + "CODE CẦN PHÂN TÍCH:\n" + sourceCode;

            // Xây dựng payload cho Gemini
            JSONObject part = new JSONObject().put("text", prompt);
            JSONArray parts = new JSONArray().put(part);
            JSONObject contentObj = new JSONObject().put("parts", parts);
            JSONArray contents = new JSONArray().put(contentObj);

            JSONObject generationConfig = new JSONObject();
            generationConfig.put("response_mime_type", "application/json");

            JSONObject requestBody = new JSONObject();
            requestBody.put("contents", contents);
            requestBody.put("generationConfig", generationConfig);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?key=" + API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body != null)
                body = body.trim();

            if (body == null || body.isEmpty() || !body.startsWith("{")) {
                return "{\"error\": \"Phản hồi từ AI không hợp lệ.\"}";
            }

            JSONObject resJson = new JSONObject(body);

            if (resJson.has("candidates")) {
                String content = resJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                // Loại bỏ markdown code block nếu AI cố tình thêm vào
                content = content.trim();
                if (content.startsWith("```json")) {
                    content = content.substring(7);
                } else if (content.startsWith("```")) {
                    content = content.substring(3);
                }
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
                return content.trim();
            }
            if (resJson.has("error")) {
                return "{\"error\": \"Lỗi API Gemini: " + resJson.getJSONObject("error").optString("message", "Unknown")
                        + "\"}";
            }
            return "{\"error\": \"Lỗi không xác định\"}";
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
