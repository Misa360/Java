package com.codeanalyzer.ui.panel;

import com.codeanalyzer.ai.AIService;
import com.codeanalyzer.repository.SubmissionRepository;
import com.codeanalyzer.service.CrawlService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;

public class CrawlStatusPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextArea txtLog;
    private JTextField txtHandle;
    private CrawlService crawlService;
    private SubmissionRepository subRepo = new SubmissionRepository();
    private AIService aiService = new AIService();

    public CrawlStatusPanel(CrawlService crawlService) {
        this.crawlService = crawlService;
        setLayout(new BorderLayout());

        // --- Top: Input ---
        JPanel pnlInput = new JPanel();
        txtHandle = new JTextField(15);
        JButton btnCrawl = new JButton("Crawl Nick");
        JButton btnRefresh = new JButton("Làm mới");
        JButton btnViewCode = new JButton("Xem Code");
        JButton btnAI = new JButton("AI Phân Tích");

        pnlInput.add(new JLabel("Nick:"));
        pnlInput.add(txtHandle);
        pnlInput.add(btnCrawl);
        pnlInput.add(btnRefresh);
        pnlInput.add(btnViewCode);
        pnlInput.add(btnAI);
        add(pnlInput, BorderLayout.NORTH);

        // --- Center: Table ---
        String[] cols = { "ID Submission", "Người nộp", "Contest", "Kết quả AI" };
        tableModel = new DefaultTableModel(cols, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Bottom: Log ---
        txtLog = new JTextArea(10, 0);
        txtLog.setEditable(false);
        txtLog.setBackground(Color.BLACK);
        txtLog.setForeground(new Color(0, 255, 65)); // Matrix green
        add(new JScrollPane(txtLog), BorderLayout.SOUTH);

        // --- Events ---
        btnCrawl.addActionListener(e -> {
            String handle = txtHandle.getText().trim();
            if (!handle.isEmpty()) {
                new Thread(() -> {
                    crawlService.crawlUser(handle);
                    refreshTable();
                }).start();
            }
        });

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

        btnAI.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1)
                return;
            String subId = table.getValueAt(row, 0).toString();
            new Thread(() -> {
                log("🤖 Đang gửi bài " + subId + " cho AI...");
                String code = subRepo.getCodeById(subId);
                String res = aiService.analyzeCode(code);
                subRepo.updateAnalysis(subId, res);
                log("✅ AI đã phân tích xong bài " + subId);
                refreshTable();
                JOptionPane.showMessageDialog(this, res);
            }).start();
        });

        refreshTable();
    }

    public void refreshTable() {
        try {
            tableModel.setRowCount(0);
            ResultSet rs = subRepo.getAllSubmissions();
            while (rs.next()) {
                tableModel.addRow(new Object[] {
                        rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)
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

    private void showCodeDialog(String subId, String code) {
        JTextArea area = new JTextArea(25, 80);
        area.setText(code);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Source Code: " + subId, true);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
