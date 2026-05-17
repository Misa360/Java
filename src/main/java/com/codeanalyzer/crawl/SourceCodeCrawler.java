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
        try {
            // Tự động tìm và tắt dứt điểm các tiến trình Chrome đang dùng chung thư mục profile (nếu người dùng quên tắt)
            Runtime.getRuntime().exec(new String[]{"powershell", "-Command", "Get-CimInstance Win32_Process -Filter \"Name = 'chrome.exe'\" | Where-Object CommandLine -match 'chrome_profile' | Stop-Process -Force"}).waitFor();
            Thread.sleep(1000); // Đợi 1 chút để HDH kịp nhả file lock
        } catch (Exception e) {
            // Bỏ qua lỗi
        }

        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--user-data-dir=" + System.getProperty("user.dir") + "/chrome_profile");
        options.addArguments("--disable-blink-features=AutomationControlled");
        // Thêm các tuỳ chọn ẩn danh để tránh bị phát hiện là bot
        options.setExperimentalOption("excludeSwitches", new String[] { "enable-automation" });
        options.setExperimentalOption("useAutomationExtension", false);

        try {
            ChromeDriver chromeDriver = new ChromeDriver(options);
            // Vô hiệu hóa cờ webdriver để vượt mặt Cloudflare JS Challenge
            chromeDriver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", 
                java.util.Map.of("source", "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"));
            driver = chromeDriver;
            wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, 
                "Không thể khởi động trình duyệt.\nLý do: Có thể bạn đang mở một cửa sổ Chrome cũ chưa đóng (hoặc tiến trình chạy ngầm bị kẹt).\n\nVui lòng đóng hết các cửa sổ Chrome do Tool bật ra, hoặc khởi động lại máy tính nếu cần, rồi bấm Crawl lại.", 
                "Lỗi Kẹt Trình Duyệt", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        // BƯỚC KHỞI ĐỘNG: Vào trang chủ Codeforces 1 lần duy nhất để nạp Session/Cookie
        driver.get("https://codeforces.com/");
        try { Thread.sleep(3000); } catch (InterruptedException e) {}

        // LUÔN LUÔN hiển thị hộp thoại chờ người dùng kiểm tra đăng nhập trước khi chạy tiếp
        javax.swing.JOptionPane.showMessageDialog(null, 
            "Trình duyệt quét Codeforces đã được mở!\n\n" +
            "👉 Bạn hãy KIỂM TRA cửa sổ Chrome vừa hiện lên xem đã đăng nhập Codeforces chưa.\n" +
            "👉 Nếu chưa, hãy tiến hành Đăng nhập (hoặc giải quyết Captcha nếu bị chặn).\n\n" +
            "Sau khi chắc chắn bạn đã vào được Codeforces với tư cách thành viên, hãy bấm OK ở đây để bắt đầu lấy mã nguồn.", 
            "Kiểm tra Đăng nhập Codeforces", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    public String crawlSourceCode(String contestId, String submissionId) {
        if (driver == null) return null; // Bỏ qua nếu chưa khởi tạo được trình duyệt
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

            // 3. Đợi và lấy code (Tối đa 10s cho chắc)
            try {
                WebElement codeArea = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.presenceOfElementLocated(By.id("program-source-text")));
                JavascriptExecutor js = (JavascriptExecutor) driver;
                // Dùng innerText thay vì textContent để giữ lại các ký tự xuống dòng (newlines) của mã nguồn
                return (String) js.executeScript("return document.getElementById('program-source-text').innerText;");
            } catch (Exception ex) {
                // Phương án dự phòng (Fallback): Tìm thẻ pre đầu tiên chứa class prettyprint
                WebElement preArea = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("pre.prettyprint")));
                JavascriptExecutor js = (JavascriptExecutor) driver;
                return (String) js.executeScript("return arguments[0].innerText;", preArea);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void quit() {
        if (driver != null) driver.quit();
    }
}
