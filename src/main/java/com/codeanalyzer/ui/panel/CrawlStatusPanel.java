package com.codeanalyzer.ui.panel;

import com.codeanalyzer.ai.AIService;
import com.codeanalyzer.repository.SubmissionRepository;
import com.codeanalyzer.service.CrawlService;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.ResultSet;

public class CrawlStatusPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea txtLog;
    private CrawlService crawlService;
    private SubmissionRepository subRepo = new SubmissionRepository();
    private AIService aiService = new AIService();

    public CrawlStatusPanel(CrawlService crawlService) {
        this.crawlService = crawlService;
        setLayout(new BorderLayout());

        // --- Top: Input ---
        JPanel pnlInput = new JPanel();
        JButton btnRefresh = new JButton("Làm mới Bảng");
        JButton btnViewCode = new JButton("Xem Code Chi Tiết");
        JButton btnViewAI = new JButton("🔍 Xem kết quả AI");
        JButton btnAI = new JButton("🤖 AI Phân Tích Code");
        JButton btnAnalyzeAll = new JButton("🔄 Phân tích toàn bộ");

        pnlInput.add(btnRefresh);
        pnlInput.add(btnViewCode);
        pnlInput.add(btnViewAI);
        pnlInput.add(btnAI);
        pnlInput.add(btnAnalyzeAll);
        add(pnlInput, BorderLayout.NORTH);

        // --- Center: Table ---
        String[] cols = { "ID", "When", "Who", "Problem", "Lang", "Verdict", "Time (ms)", "Memory (KB)", "AI Analysis", "RAW_JSON" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        
        // Hide RAW_JSON column
        table.getColumnModel().getColumn(9).setMinWidth(0);
        table.getColumnModel().getColumn(9).setMaxWidth(0);
        table.getColumnModel().getColumn(9).setPreferredWidth(0);
        
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom: Log ---
        txtLog = new JTextArea(10, 0);
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(25, 25, 25));
        txtLog.setForeground(new Color(0, 255, 65)); // Matrix green
        add(new JScrollPane(txtLog), BorderLayout.SOUTH);

        // --- Events ---
        btnRefresh.addActionListener(e -> refreshTable());

        btnViewCode.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bài nộp!");
                return;
            }
            String subId = table.getValueAt(row, 0).toString();
            String code = subRepo.getCodeById(subId);
            showCodeDialog(subId, code);
        });

        btnViewAI.addActionListener(e -> viewAiResultAtSelectedRow());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewAiResultAtSelectedRow();
                }
            }
        });

        btnAI.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một bài nộp!");
                return;
            }
            String subId = table.getValueAt(row, 0).toString();
            new Thread(() -> {
                String code = subRepo.getCodeById(subId);
                if (code == null || code.trim().length() < 10) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Không tìm thấy Source Code cho bài " + subId + " trong Database.\nCó thể quá trình Crawl trước đó bị lỗi hoặc mã nguồn bị ẩn.", "Lỗi Dữ Liệu", JOptionPane.WARNING_MESSAGE);
                    });
                    return;
                }
                
                log("🤖 Đang gửi bài " + subId + " cho AI...");
                String res = aiService.analyzeCode(code);
                subRepo.updateAnalysis(subId, res);
                log("✅ AI đã phân tích xong bài " + subId);
                
                SwingUtilities.invokeLater(() -> {
                    refreshTable();
                    showAiResultDialog(subId, res);
                });
            }).start();
        });

        btnAnalyzeAll.addActionListener(e -> {
            new Thread(() -> {
                log("🔄 Bắt đầu phân tích toàn bộ các bài chưa có kết quả (chế độ đa luồng)...");
                try {
                    ResultSet rs = subRepo.getAllSubmissions();
                    java.util.List<String> pendingIds = new java.util.ArrayList<>();
                    while (rs.next()) {
                        String subId = rs.getString("submission_id");
                        String aiRaw = rs.getString("ai_analysis");
                        if (aiRaw == null || !aiRaw.startsWith("{")) {
                            pendingIds.add(subId);
                        }
                    }
                    rs.close();

                    if (pendingIds.isEmpty()) {
                        log("✅ Không có bài nào cần phân tích.");
                        return;
                    }

                    log(">> Tìm thấy " + pendingIds.size() + " bài cần phân tích. Bắt đầu đẩy lên AI...");
                    
                    int threads = Math.min(5, pendingIds.size());
                    java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
                    java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

                    for (String subId : pendingIds) {
                        executor.submit(() -> {
                            String code = subRepo.getCodeById(subId);
                            if (code != null && code.trim().length() >= 10) {
                                String res = aiService.analyzeCode(code);
                                subRepo.updateAnalysis(subId, res);
                                int done = count.incrementAndGet();
                                log(String.format("🤖 Đã phân tích xong bài %s (%d/%d)", subId, done, pendingIds.size()));
                            } else {
                                int done = count.incrementAndGet();
                                log(String.format("⚠️ Bỏ qua bài %s vì không có mã nguồn (%d/%d)", subId, done, pendingIds.size()));
                            }
                            SwingUtilities.invokeLater(this::refreshTable);
                        });
                    }

                    executor.shutdown();
                    executor.awaitTermination(1, java.util.concurrent.TimeUnit.HOURS);
                    
                    log("✅ Đã hoàn tất phân tích toàn bộ " + count.get() + " bài.");
                } catch (Exception ex) {
                    log("❌ Lỗi phân tích hàng loạt: " + ex.getMessage());
                }
            }).start();
        });

        refreshTable();
    }

    private void viewAiResultAtSelectedRow() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bài nộp!");
            return;
        }
        String subId = table.getValueAt(row, 0).toString();
        Object rawJson = table.getValueAt(row, 9);
        if (rawJson == null || rawJson.toString().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bài nộp này chưa được AI phân tích!");
            return;
        }
        showAiResultDialog(subId, rawJson.toString());
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            ResultSet rs = subRepo.getAllSubmissions();
            while (rs.next()) {
                String subId = rs.getString("submission_id");
                long timeSec = rs.getLong("submission_time");
                String dateStr = timeSec > 0 ? new java.text.SimpleDateFormat("MMM/dd/yyyy HH:mm").format(new java.util.Date(timeSec * 1000)) : "";
                String handle = rs.getString("handle");
                String problem = rs.getString("problem_name");
                String lang = rs.getString("programming_language");
                String verdict = rs.getString("verdict");
                String time = rs.getInt("time_consumed_ms") + " ms";
                String mem = (rs.getInt("memory_consumed_bytes") / 1024) + " KB";
                String aiRaw = rs.getString("ai_analysis");
                
                String aiStatus = "Chưa phân tích";
                if (aiRaw != null && aiRaw.startsWith("{")) {
                    try {
                        JSONObject json = new JSONObject(aiRaw);
                        if (json.has("error") && "hidden_code".equals(json.optString("error"))) {
                            aiStatus = "Bị ẩn (Bỏ qua)";
                        } else {
                            aiStatus = String.format("AI: %s%% | %s", json.optString("ai_probability", "?"), json.optString("skill_level", "?"));
                        }
                    } catch (Exception e) { aiStatus = "Lỗi JSON"; }
                }

                tableModel.addRow(new Object[] {
                        subId, dateStr, handle, problem, lang, verdict, time, mem, aiStatus, aiRaw
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private void showAiResultDialog(String subId, String res) {
        try {
            JSONObject json = new JSONObject(res);
            if (json.has("error")) {
                JOptionPane.showMessageDialog(this, "Lỗi từ AI: " + json.getString("error"), "Lỗi Phân Tích", JOptionPane.ERROR_MESSAGE);
            } else {
                StringBuilder msg = new StringBuilder();
                msg.append("🤖 KẾT QUẢ PHÂN TÍCH AI - BÀI NỘP: ").append(subId).append("\n\n");
                msg.append("🔹 Thuật toán/CTDL: ").append(json.optString("ds_algo", "Không rõ")).append("\n");
                msg.append("🔹 Xác suất dùng AI: ").append(json.optString("ai_probability", "?")).append("%\n");
                msg.append("🔹 Trình độ ước tính: ").append(json.optString("skill_level", "Không rõ")).append("\n\n");
                msg.append("📝 CHI TIẾT GIẢI THÍCH:\n");
                msg.append(json.optString("explanation", "Không có giải thích chi tiết."));

                JTextArea area = new JTextArea(15, 60);
                area.setText(msg.toString());
                area.setEditable(false);
                area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                JScrollPane scroll = new JScrollPane(area);
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Kết quả AI - " + subId, true);
                dialog.add(scroll);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Dữ liệu AI không hợp lệ: " + res);
        }
    }

    private void showCodeDialog(String subId, String code) {
        JTextArea area = new JTextArea(30, 90);
        if ("HIDDEN_CODE_ACCESS_DENIED".equals(code)) {
            area.setText("// Rất tiếc, bài nộp này đã bị người dùng cài đặt ẩn (hoặc nằm trong kỳ thi Private).\n" +
                         "// Hệ thống Codeforces không cho phép bạn xem mã nguồn này.");
            area.setForeground(Color.RED);
        } else {
            area.setText(code);
            area.setForeground(new Color(220, 220, 220));
        }
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBackground(new Color(30, 30, 30));
        area.setCaretColor(Color.WHITE);
        area.setLineWrap(false); // Code thường không nên wrap để dễ nhìn cấu trúc
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Source Code ID: " + subId));
        
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Source Code Viewer", true);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}

