package com.codeanalyzer.ui;

import com.codeanalyzer.crawl.CrawlScheduler;
import com.codeanalyzer.service.CrawlService;
import com.codeanalyzer.ui.panel.CrawlStatusPanel;
import com.codeanalyzer.ui.panel.UserManagementPanel;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private CrawlService crawlService = new CrawlService();
    private CrawlScheduler scheduler;
    private CrawlStatusPanel statusPanel;

    public MainFrame() {
        setTitle("Hệ thống Phân tích Code AI - Pro");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Khởi động Scheduler mặc định 24h (dùng chung cho toàn app)
        scheduler = new CrawlScheduler(crawlService);
        // scheduler.start(24); // Đã tắt tự động chạy khi khởi động

        JTabbedPane mainTabs = new JTabbedPane();

        // Tab 1: Dashboard
        com.codeanalyzer.ui.panel.DashboardPanel dashboardPanel = new com.codeanalyzer.ui.panel.DashboardPanel();
        mainTabs.addTab("1. Dashboard", dashboardPanel);

        // Tab 2: User Management
        UserManagementPanel userPanel = new UserManagementPanel();
        mainTabs.addTab("2. User Management", userPanel);

        // Tab 3: Crawl Scheduler — truyền scheduler đã tạo, tránh tạo mới
        com.codeanalyzer.ui.panel.CrawlSchedulerPanel schedulerPanel = new com.codeanalyzer.ui.panel.CrawlSchedulerPanel(
                crawlService, scheduler);
        mainTabs.addTab("3. Crawl & Task", schedulerPanel);

        // Tab 4: AI Analysis & Code Detail
        statusPanel = new CrawlStatusPanel(crawlService);
        // crawlService.setLogger(statusPanel::log); // Xóa dòng này để thông báo crawl
        // hiện ở tab 3
        mainTabs.addTab("4. AI Analysis", statusPanel);

        // --- Cảnh Báo Cập Nhật ---
        JPanel pnlWarning = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlWarning.setBackground(new Color(255, 235, 238)); // Light red
        JLabel lblWarning = new JLabel("Đã quá 24h chưa cập nhật dữ liệu mới!");
        lblWarning.setForeground(new Color(211, 47, 47)); // Red
        lblWarning.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JButton btnUpdateNow = new JButton("Bắt đầu cập nhật");
        btnUpdateNow.setBackground(new Color(244, 67, 54));
        btnUpdateNow.setForeground(Color.WHITE);

        JProgressBar warningProgress = new JProgressBar();
        warningProgress.setIndeterminate(true);
        warningProgress.setVisible(false);
        warningProgress.setStringPainted(true);
        warningProgress.setString("Đang quét...");

        pnlWarning.add(lblWarning);
        pnlWarning.add(btnUpdateNow);
        pnlWarning.add(warningProgress);

        if (crawlService.isDataOutdated()) {
            add(pnlWarning, BorderLayout.NORTH);
        }

        btnUpdateNow.addActionListener(e -> {
            btnUpdateNow.setEnabled(false);
            warningProgress.setVisible(true);
            lblWarning.setText("Đang cập nhật dữ liệu... Bạn có thể xem chi tiết ở tab Crawl.");
            lblWarning.setForeground(new Color(25, 118, 210)); // Blue

            new Thread(() -> {
                crawlService.crawlAllUsers(true);
                SwingUtilities.invokeLater(() -> {
                    pnlWarning.setVisible(false); // Ẩn thanh cảnh báo sau khi xong
                    int index = mainTabs.getSelectedIndex();
                    switch (index) {
                        case 0:
                            dashboardPanel.refreshStats();
                            break;
                        case 1:
                            userPanel.refreshList();
                            break;
                        case 3:
                            statusPanel.refreshTable();
                            break;
                    }
                    JOptionPane.showMessageDialog(MainFrame.this, "Đã cập nhật dữ liệu mới thành công!", "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        });

        add(mainTabs, BorderLayout.CENTER);

        // Auto refresh khi chuyển tab
        mainTabs.addChangeListener(e -> {
            // Tự động ẩn thanh cảnh báo nếu dữ liệu đã được cập nhật
            if (!crawlService.isDataOutdated()) {
                pnlWarning.setVisible(false);
            }

            int index = mainTabs.getSelectedIndex();
            switch (index) {
                case 0:
                    dashboardPanel.refreshStats();
                    break;
                case 1:
                    userPanel.refreshList();
                    break;
                case 3:
                    statusPanel.refreshTable();
                    break;
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatIntelliJLaf());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("ProgressBar.arc", 12);
            UIManager.put("TextComponent.arc", 12);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("Component.accentColor", new Color(33, 150, 243));
            UIManager.put("TabbedPane.selectedBackground", new Color(225, 240, 255));

            Font modernFont = new Font("Segoe UI", Font.PLAIN, 14);
            java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = UIManager.get(key);
                if (value instanceof javax.swing.plaf.FontUIResource) {
                    UIManager.put(key, new javax.swing.plaf.FontUIResource(modernFont));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
