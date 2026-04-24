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

        statusPanel = new CrawlStatusPanel(crawlService);
        crawlService.setLogger(statusPanel::log);

        UserManagementPanel userPanel = new UserManagementPanel();
        userPanel.setPreferredSize(new Dimension(250, 0));

        add(statusPanel, BorderLayout.CENTER);
        add(userPanel, BorderLayout.WEST);

        // Sidebar or Topbar can be added here
        JPanel pnlTop = new JPanel();
        JButton btnStartScheduler = new JButton("Bật Tự động Crawl (24h)");
        pnlTop.add(btnStartScheduler);
        add(pnlTop, BorderLayout.NORTH);

        scheduler = new CrawlScheduler(crawlService);

        btnStartScheduler.addActionListener(e -> {
            scheduler.start();
            btnStartScheduler.setEnabled(false);
            btnStartScheduler.setText("Tự động Crawl: ĐANG CHẠY");
            statusPanel.log("⏰ Đã kích hoạt chế độ crawl định kỳ 24h.");
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
