package com.codeanalyzer.crawl;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CodeforcesApiClient {
    public JSONArray getUserSubmissions(String handle) throws Exception {
        String apiUrl = "https://codeforces.com/api/user.status?handle=" + handle + "&from=1&count=10";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String body = response.body();
        if (body != null) body = body.trim();

        // Kiểm tra xem response có phải JSON không
        if (body == null || body.isEmpty() || !body.startsWith("{")) {
            throw new Exception("API Codeforces trả về nội dung không hợp lệ (có thể bị chặn hoặc rate-limit). Vui lòng thử lại sau.");
        }

        JSONObject json = new JSONObject(body);
        if (!json.getString("status").equals("OK")) {
            throw new Exception("Lỗi API: " + json.optString("comment"));
        }
        return json.getJSONArray("result");
    }
    public JSONObject getUserInfo(String handle) {
        try {
            String apiUrl = "https://codeforces.com/api/user.info?handles=" + handle;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(apiUrl)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            if (body != null && body.startsWith("{")) {
                JSONObject json = new JSONObject(body);
                if (json.optString("status").equals("OK")) {
                    return json.getJSONArray("result").getJSONObject(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
