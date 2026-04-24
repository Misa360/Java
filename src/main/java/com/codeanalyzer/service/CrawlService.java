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
        if (logger != null) logger.accept(msg);
    }

    public void crawlUser(String handle) {
        SourceCodeCrawler crawler = new SourceCodeCrawler();
        try {
            // BƯỚC 1: LẤY DANH SÁCH ID TỪ API (100% THÀNH CÔNG)
            log(">> Đang gọi API lấy danh sách bài của: " + handle);
            userRepo.addUser(handle);
            JSONArray results = apiClient.getUserSubmissions(handle);

            // BƯỚC 2: KHỞI ĐỘNG SELENIUM
            crawler.initDriver();

            // BƯỚC 3: DUYỆT TỪNG ID LẤY ĐƯỢC TỪ API
            for (int i = 0; i < results.length(); i++) {
                try {
                    JSONObject sub = results.getJSONObject(i);
                    if (!sub.has("verdict") || !sub.getString("verdict").equals("OK"))
                        continue; // Chỉ lấy Accepted

                    String subId = String.valueOf(sub.getInt("id"));

                    // Kiểm tra xem bài nộp đã có trong DB chưa
                    if (submissionRepo.isSubmissionExist(subId)) {
                        log(">> Bài " + subId + " đã tồn tại trong DB, bỏ qua...");
                        continue;
                    }

                    int cId = sub.optInt("contestId", 0);
                    String contestId = String.valueOf(cId);

                    String url = "https://codeforces.com/contest/" + contestId + "/submission/" + subId;
                    if (cId >= 100000) {
                        url = "https://codeforces.com/gym/" + contestId + "/submission/" + subId;
                    }

                    log(">> Đang lấy bài " + subId + " tại: " + url);
                    String code = crawler.crawlSourceCode(contestId, subId);

                    if (code != null && code.length() > 10) {
                        submissionRepo.saveSubmission(subId, contestId, handle, code);
                        log("✅ Đã lưu thành công bài " + subId);
                    } else {
                        log("❌ Không lấy được code bài " + subId);
                    }

                    // Nghỉ 3 giây để tránh bị quét
                    Thread.sleep(3000);
                } catch (Exception ex) {
                    log("❌ Lỗi khi xử lý bài nộp: " + ex.getMessage());
                }
            }
            log("🎉 Hoàn tất quá trình crawl!");

        } catch (Exception e) {
            log("❌ Lỗi: " + e.getMessage());
        } finally {
            crawler.quit();
        }
    }

    public void crawlAllUsers() {
        List<String> users = userRepo.getAllUsers();
        log(">> Đang crawl định kỳ cho " + users.size() + " người dùng...");
        for (String user : users) {
            crawlUser(user);
        }
    }
}
