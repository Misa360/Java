package com.codeanalyzer.crawl;

import com.codeanalyzer.service.CrawlService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CrawlScheduler {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;
    private final CrawlService crawlService;

    public CrawlScheduler(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    public void start(int frequencyHours) {
        if (currentTask != null && !currentTask.isCancelled()) {
            return;
        }
        System.out.println(">> Đã khởi tạo lịch quét định kỳ mỗi " + frequencyHours + " giờ.");
        // Chạy lần đầu sau 1 phút, sau đó lặp lại theo tần suất
        currentTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println(">> [Sự kiện Lịch] Bắt đầu quét toàn bộ hệ thống...");
                crawlService.crawlAllUsers(false);
            } catch (Exception e) {
                System.err.println(">> [Lỗi Lịch] " + e.getMessage());
                e.printStackTrace();
            }
        }, 1, frequencyHours * 60, TimeUnit.MINUTES);
    }

    public void stop() {
        if (currentTask != null) {
            currentTask.cancel(false);
        }
    }
}
