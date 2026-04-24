package com.codeanalyzer.crawl;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class SourceCodeCrawler {
    private WebDriver driver;
    private WebDriverWait wait;

    public void initDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-data-dir=" + System.getProperty("user.dir") + "/chrome_profile");
        options.addArguments("--disable-blink-features=AutomationControlled");
        // Thêm các tuỳ chọn ẩn danh để tránh bị phát hiện là bot
        options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // BƯỚC KHỞI ĐỘNG: Vào trang chủ Codeforces 1 lần duy nhất để nạp Session/Cookie
        driver.get("https://codeforces.com/");
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
    }

    public String crawlSourceCode(String contestId, String submissionId) {
        try {
            int cId = Integer.parseInt(contestId);
            // CHIẾN THUẬT: Truy cập trực tiếp (Nhanh và ổn định)
            String url = "https://codeforces.com/contest/" + contestId + "/submission/" + submissionId;
            if (cId >= 100000) {
                url = "https://codeforces.com/gym/" + contestId + "/submission/" + submissionId;
            }

            driver.get(url);
            Thread.sleep(1000);

            // 1. Kiểm tra chặn truy cập (Not Allowed)
            if (driver.getPageSource().contains("You are not allowed to view the requested page")) {
                driver.navigate().refresh();
                Thread.sleep(1500);

                // Nếu vẫn bị, thử sang link problemset
                if (driver.getPageSource().contains("You are not allowed to view the requested page")) {
                    driver.get("https://codeforces.com/problemset/submission/" + contestId + "/" + submissionId);
                    Thread.sleep(1500);
                }
            }

            // 2. Kiểm tra Cloudflare nhanh
            if (driver.getTitle().contains("Just a moment") || driver.getTitle().contains("Verify")) {
                int waitCf = 0;
                while (driver.getTitle().contains("Just a moment") && waitCf < 60) {
                    Thread.sleep(2000);
                    waitCf += 2;
                }
            }

            // 3. Đợi và lấy code (Tối đa 3s cho nhanh)
            WebElement codeArea = new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("program-source-text")));

            JavascriptExecutor js = (JavascriptExecutor) driver;
            return (String) js.executeScript("return document.getElementById('program-source-text').innerText;");
        } catch (Exception e) {
            return null;
        }
    }

    public void quit() {
        if (driver != null) driver.quit();
    }
}
