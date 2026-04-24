package com.codeanalyzer.ai;

import com.codeanalyzer.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIService {
    private static final String API_KEY = AppConfig.get("ai.api.key", "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");
    private static final String API_URL = AppConfig.get("ai.api.url", "https://api.groq.com/openai/v1/chat/completions");

    public String analyzeCode(String sourceCode) {
        try {
            if (sourceCode == null || sourceCode.length() < 10) return "Code quá ngắn.";
            if (sourceCode.length() > 5000) sourceCode = sourceCode.substring(0, 5000);

            String prompt = "Phân tích đoạn code sau bằng tiếng Việt ngắn gọn:\n" +
                    "1. Thuật toán và cấu trúc dữ liệu chính là gì?\n" +
                    "2. Đánh giá xem code này có khả năng là do AI viết không?\n" +
                    "3. Đánh giá trình độ người viết (Thấp/Trung bình/Giỏi).\n\n" +
                    "CODE:\n" + sourceCode;

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "llama-3.3-70b-versatile");

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "user").put("content", prompt));
            requestBody.put("messages", messages);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (body != null) body = body.trim();

            if (body == null || body.isEmpty() || !body.startsWith("{")) {
                return "Lỗi: Phản hồi từ AI không hợp lệ.";
            }

            JSONObject resJson = new JSONObject(body);

            if (resJson.has("choices")) {
                return resJson.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
            }
            if (resJson.has("error")) {
                return "Lỗi API Groq: " + resJson.getJSONObject("error").optString("message", resJson.getJSONObject("error").toString());
            }
            return "Lỗi phản hồi AI (Không xác định): " + resJson.toString();
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }
}
