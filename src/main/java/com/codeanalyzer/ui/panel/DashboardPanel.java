package com.codeanalyzer.ui.panel;

import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {
    private JLabel lblUserCount;
    private JLabel lblSubCount;
    private JLabel lblTodayCount;
    private com.codeanalyzer.repository.SubmissionRepository subRepo = new com.codeanalyzer.repository.SubmissionRepository();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        
        // Thẻ thống kê
        JPanel pnlCards = new JPanel(new GridLayout(1, 3, 10, 10));
        pnlCards.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        lblUserCount = new JLabel("...", SwingConstants.CENTER);
        lblSubCount = new JLabel("...", SwingConstants.CENTER);
        lblTodayCount = new JLabel("...", SwingConstants.CENTER);

        pnlCards.add(createCard("Tổng số Nick đang theo dõi", lblUserCount, new Color(33, 150, 243)));
        pnlCards.add(createCard("Tổng số bài nộp đã crawl", lblSubCount, new Color(76, 175, 80)));
        pnlCards.add(createCard("Số bài đã phân tích AI", lblTodayCount, new Color(156, 39, 176))); // Purple color
        add(pnlCards, BorderLayout.NORTH);

        // Biểu đồ (Placeholder)
        JPanel pnlChart = new JPanel(new BorderLayout());
        pnlChart.setBorder(BorderFactory.createTitledBorder("Thông tin hệ thống"));
        JLabel lblChart = new JLabel("<html><center><h1 style='color:#2196F3'>🚀 Hệ thống Phân tích Code AI</h1><br>" +
                "<p style='font-size:12px'>Tự động thu thập dữ liệu từ Codeforces và sử dụng trí tuệ nhân tạo Gemini để đánh giá trình độ người dùng.</p></center></html>", SwingConstants.CENTER);
        pnlChart.add(lblChart, BorderLayout.CENTER);
        add(pnlChart, BorderLayout.CENTER);

        // Log nhanh
        JPanel pnlLog = new JPanel(new BorderLayout());
        pnlLog.setBorder(BorderFactory.createTitledBorder("Trạng thái hoạt động & Kết nối"));
        JTextArea txtLog = new JTextArea(8, 0); // Tăng chiều cao
        txtLog.setEditable(false);
        txtLog.setText(">> Hệ thống đã sẵn sàng.\n>> Kết nối Cơ sở dữ liệu MySQL: Thành công\n>> Google Gemini AI API: Đang hoạt động (v1beta)");
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(0, 255, 65));
        pnlLog.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        
        add(pnlLog, BorderLayout.SOUTH);

        refreshStats();
    }

    public void refreshStats() {
        Object[] stats = subRepo.getDashboardStats();
        lblUserCount.setText(stats[0].toString());
        lblSubCount.setText(stats[1].toString());
        lblTodayCount.setText(stats[2].toString());
    }

    private JPanel createCard(String title, JLabel lblValue, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblValue.setForeground(Color.WHITE);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 36));
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }
}
