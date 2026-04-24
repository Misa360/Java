package com.codeanalyzer.crawl;

import com.codeanalyzer.service.CrawlService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrawlScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CrawlService crawlService;

    public CrawlScheduler(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    public void start() {
        // Chạy lần đầu sau 1 phút, sau đó lặp lại mỗi 24 giờ
        scheduler.scheduleAtFixedRate(() -> {
            try {
                crawlService.crawlAllUsers();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 24 * 60, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
