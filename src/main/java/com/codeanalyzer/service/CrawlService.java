package com.codeanalyzer.service;

import com.codeanalyzer.crawl.CodeforcesApiClient;
import com.codeanalyzer.crawl.SourceCodeCrawler;
import com.codeanalyzer.repository.SubmissionRepository;
import com.codeanalyzer.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.function.Consumer;

public class CrawlService {
    private CodeforcesApiClient apiClient = new CodeforcesApiClient();
    private SubmissionRepository submissionRepo = new SubmissionRepository();
    private UserRepository userRepo = new UserRepository();
    private Consumer<String> logger;

    public void setLogger(Consumer<String> logger) {
        this.logger = logger;
    }

    private void log(String msg) {
        if (logger != null)
            logger.accept(msg);
    }

    public boolean isDataOutdated() {
        List<com.codeanalyzer.model.User> users = userRepo.getAllUsersFull();
        if (users.isEmpty()) return false;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long now = System.currentTimeMillis();
        boolean allUncrawled = true;

        for (com.codeanalyzer.model.User user : users) {
            if (user.getLastCrawledAt() != null && !user.getLastCrawledAt().equals("Chưa crawl")) {
                allUncrawled = false;
                try {
                    java.util.Date lastCrawl = sdf.parse(user.getLastCrawledAt());
                    if (now - lastCrawl.getTime() > 24 * 60 * 60 * 1000L) {
                        return true;
                    }
                } catch (Exception e) {
                    return true;
                }
            }
        }
        return allUncrawled;
    }

    public void crawlUser(String handle) {
        // Kiểm tra xem user thuộc platform nào
        List<com.codeanalyzer.model.User> allUsers = userRepo.getAllUsersFull();
        String platform = "Codeforces";
        for (com.codeanalyzer.model.User u : allUsers) {
            if (u.getHandle().equals(handle)) {
                platform = u.getPlatform();
                break;
            }
        }

        SourceCodeCrawler crawler = new SourceCodeCrawler();
        try {
            log(">> Đang kiểm tra bài nộp mới của: " + handle);
            userRepo.addUser(handle, "Codeforces");
            JSONArray results = apiClient.getUserSubmissions(handle);

            // Tìm những bài nộp mới chưa có trong DB
            java.util.List<JSONObject> newSubmissions = new java.util.ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject sub = results.getJSONObject(i);
                if (sub.has("verdict") && sub.getString("verdict").equals("OK")) {
                    String subId = String.valueOf(sub.getInt("id"));
                    if (!submissionRepo.isSubmissionExist(subId)) {
                        newSubmissions.add(sub);
                    }
                }
            }

            if (newSubmissions.isEmpty()) {
                log(">> " + handle + " không có bài nộp mới.");
                userRepo.updateLastCrawlTime(handle);
                return;
            }

            log(">> Tìm thấy " + newSubmissions.size() + " bài nộp mới. Đang khởi động trình duyệt...");
            crawler.initDriver();

            int count = 0;
            for (JSONObject sub : newSubmissions) {
                try {
                    String subId = String.valueOf(sub.getInt("id"));
                    int cId = sub.optInt("contestId", 0);
                    String contestId = String.valueOf(cId);

                    String url = "https://codeforces.com/contest/" + contestId + "/submission/" + subId;
                    if (cId >= 100000) {
                        url = "https://codeforces.com/gym/" + contestId + "/submission/" + subId;
                    }

                    long creationTimeSeconds = sub.optLong("creationTimeSeconds", 0);
                    String problemName = sub.has("problem") ? sub.getJSONObject("problem").optString("name", "Unknown")
                            : "Unknown";
                    String programmingLanguage = sub.optString("programmingLanguage", "Unknown");
                    String verdict = sub.optString("verdict", "Unknown");
                    int timeConsumedMillis = sub.optInt("timeConsumedMillis", 0);
                    int memoryConsumedBytes = sub.optInt("memoryConsumedBytes", 0);

                    log(">> Đang lấy mã nguồn bài " + subId + "...");
                    String code = crawler.crawlSourceCode(contestId, subId);

                    if (code != null && code.length() > 10) {
                        submissionRepo.saveSubmission(subId, contestId, handle, code, problemName, programmingLanguage,
                                verdict, timeConsumedMillis, memoryConsumedBytes, creationTimeSeconds);
                        log("Đã lưu bài " + subId);
                        count++;
                    } else {
                        // Đánh dấu bài này vào DB để lần quét sau bỏ qua không mất thời gian tải lại
                        submissionRepo.saveSubmission(subId, contestId, handle, "HIDDEN_CODE_ACCESS_DENIED", problemName, programmingLanguage, verdict, timeConsumedMillis, memoryConsumedBytes, creationTimeSeconds);
                        // Đánh dấu ai_analysis luôn để AI không tốn tiền phân tích chữ HIDDEN_CODE
                        submissionRepo.updateAnalysis(subId, "{\"error\":\"hidden_code\"}");
                        log("Bỏ qua bài " + subId + " (Không có quyền xem mã nguồn hoặc bị ẩn).");
                    }

                    Thread.sleep(2000);
                } catch (Exception ex) {
                    log("Lỗi bài nộp: " + ex.getMessage());
                }
            }
            userRepo.updateLastCrawlTime(handle);
            log("Hoàn tất. Đã thêm " + count + " bài mới cho " + handle);

        } catch (Exception e) {
            log("Lỗi crawl: " + e.getMessage());
        } finally {
            crawler.quit();
        }
    }

    public void crawlAllUsers(boolean force) {
        List<com.codeanalyzer.model.User> users = userRepo.getAllUsersFull();
        log(">> Đang kiểm tra danh sách " + users.size() + " người dùng để crawl...");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long now = System.currentTimeMillis();

        for (com.codeanalyzer.model.User user : users) {
            if (!force && user.getLastCrawledAt() != null && !user.getLastCrawledAt().equals("Chưa crawl")) {
                try {
                    java.util.Date lastCrawl = sdf.parse(user.getLastCrawledAt());
                    // Nếu đã crawl trong vòng 12 giờ qua thì bỏ qua
                    if (now - lastCrawl.getTime() < 12 * 60 * 60 * 1000L) {
                        log(">> Bỏ qua " + user.getHandle() + " vì mới crawl gần đây (" + user.getLastCrawledAt()
                                + ")");
                        continue;
                    }
                } catch (Exception e) {
                    // Ignore parse error, proceed to crawl
                }
            }
            crawlUser(user.getHandle());
        }
    }
}
