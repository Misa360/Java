package com.codeanalyzer.ui.panel;

import com.codeanalyzer.crawl.CrawlScheduler;
import com.codeanalyzer.service.CrawlService;

import javax.swing.*;
import java.awt.*;

public class CrawlSchedulerPanel extends JPanel {
    private CrawlScheduler scheduler;
    private boolean schedulerRunning = false;

    public CrawlSchedulerPanel(CrawlService crawlService, CrawlScheduler sharedScheduler) {
        this.scheduler = sharedScheduler;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel pnlConfig = new JPanel(new GridLayout(2, 2, 10, 10));
        pnlConfig.setBorder(BorderFactory.createTitledBorder("Cấu hình & Crawl Thủ Công"));

        // Cột 1: Crawl Định kỳ
        pnlConfig.add(new JLabel("Tần suất định kỳ (Giờ):"));
        JComboBox<String> cbFreq = new JComboBox<>(new String[]{"24"});
        cbFreq.setSelectedItem("24");
        pnlConfig.add(cbFreq);

        JButton btnStartSched = new JButton("▶ Bật Lịch Tự Động (24h)");
        btnStartSched.setEnabled(true);
        JButton btnCrawlAll = new JButton("Crawl toàn bộ DB");
        JButton btnLogin = new JButton("Mở Chrome Đăng nhập Codeforces");
        btnLogin.setBackground(new Color(255, 152, 0)); // Orange
        btnLogin.setForeground(Color.WHITE);
        
        pnlConfig.add(btnStartSched);
        pnlConfig.add(btnCrawlAll);
        pnlConfig.add(btnLogin);

        // Cột 2: Crawl 1 người
        JPanel pnlSingle = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlSingle.add(new JLabel("Crawl Nick mới:"));
        JTextField txtHandle = new JTextField(12);
        JButton btnCrawlSingle = new JButton("Crawl Nick Này");
        pnlSingle.add(txtHandle);
        pnlSingle.add(btnCrawlSingle);

        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.add(pnlConfig, BorderLayout.CENTER);
        pnlTop.add(pnlSingle, BorderLayout.SOUTH);

        add(pnlTop, BorderLayout.NORTH);

        // Progress bar
        JPanel pnlProgress = new JPanel(new BorderLayout());
        pnlProgress.setBorder(BorderFactory.createTitledBorder("Tiến trình & Log"));
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(false);
        pnlProgress.add(progressBar, BorderLayout.NORTH);

        JTextArea txtConsole = new JTextArea(15, 0);
        txtConsole.setEditable(false);
        txtConsole.setBackground(new Color(25, 25, 25));
        txtConsole.setForeground(new Color(0, 255, 65)); // Matrix green

        pnlProgress.add(new JScrollPane(txtConsole), BorderLayout.CENTER);
        add(pnlProgress, BorderLayout.CENTER);

        // Log helper
        Runnable appendLog = () -> {
            // Bắt đầu cập nhật progress khi crawl
        };

        // Events
        crawlService.setLogger(msg ->
            SwingUtilities.invokeLater(() -> {
                txtConsole.append(msg + "\n");
                txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
            })
        );

        btnStartSched.addActionListener(e -> {
            if (schedulerRunning) {
                scheduler.stop();
                schedulerRunning = false;
                btnStartSched.setText("▶ Bật Lịch Tự Động (24h)");
                txtConsole.append(">> Đã tắt lịch tự động.\n");
            } else {
                scheduler.start(24);
                schedulerRunning = true;
                btnStartSched.setText("⏸ Tắt Lịch Tự Động (24h)");
                txtConsole.append(">> Đã bật lịch tự động (24h/lần).\n");
            }
        });

        btnCrawlSingle.addActionListener(e -> {
            String handle = txtHandle.getText().trim();
            if (!handle.isEmpty()) {
                txtConsole.append(">> Đang bắt đầu quét nick: " + handle + "\n");
                progressBar.setIndeterminate(true);
                new Thread(() -> {
                    crawlService.crawlUser(handle);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        JOptionPane.showMessageDialog(CrawlSchedulerPanel.this, "Đã hoàn tất quá trình quét nick: " + handle, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    });
                }).start();
            }
        });

        btnCrawlAll.addActionListener(e -> {
            txtConsole.append(">> Đang bắt đầu quét toàn bộ DB...\n");
            progressBar.setIndeterminate(true);
            new Thread(() -> {
                crawlService.crawlAllUsers(true);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    JOptionPane.showMessageDialog(CrawlSchedulerPanel.this, "Đã hoàn tất quá trình quét toàn bộ DB", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                });
            }).start();
        });

        btnLogin.addActionListener(e -> {
            try {
                String profilePath = System.getProperty("user.dir") + "/chrome_profile";
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start chrome.exe --user-data-dir=\"" + profilePath + "\" \"https://codeforces.com/enter\""});
                JOptionPane.showMessageDialog(this, "Đã mở Chrome! Bạn hãy đăng nhập và sau đó tắt Chrome đi nhé.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi mở Chrome: " + ex.getMessage());
            }
        });

        txtConsole.append(">> Hệ thống đã khởi động. Lịch crawl tự động hiện đang tắt.\n");
    }
}
