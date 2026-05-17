package com.codeanalyzer.ui.panel;

import com.codeanalyzer.repository.UserRepository;
import com.codeanalyzer.repository.SubmissionRepository;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class UserManagementPanel extends JPanel {
    private UserRepository userRepo = new UserRepository();
    private SubmissionRepository subRepo = new SubmissionRepository();
    private DefaultTableModel tableModel;
    private JTable userTable;

    public UserManagementPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form nhập liệu
        JPanel pnlForm = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlForm.setBorder(BorderFactory.createTitledBorder("Thêm/Sửa Nickname"));
        
        JTextField txtHandle = new JTextField(15);
        String[] platforms = {"Codeforces"};
        JComboBox<String> cbPlatform = new JComboBox<>(platforms);
        JButton btnAdd = new JButton("Thêm Nick");
        JButton btnDelete = new JButton("Xóa Nick");
        JButton btnCheck = new JButton("Kiểm tra tồn tại");
        JButton btnEvaluation = new JButton("📊 Đánh giá tổng hợp");

        pnlForm.add(new JLabel("Nickname:"));
        pnlForm.add(txtHandle);
        pnlForm.add(new JLabel("Nền tảng:"));
        pnlForm.add(cbPlatform);
        pnlForm.add(btnAdd);
        pnlForm.add(btnDelete);
        pnlForm.add(btnCheck);
        pnlForm.add(btnEvaluation);

        add(pnlForm, BorderLayout.NORTH);

        // Bảng danh sách
        String[] cols = {"Nickname", "Nền tảng", "Trạng thái", "Lần crawl cuối"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        userTable = new JTable(tableModel);
        
        JPanel pnlTable = new JPanel(new BorderLayout());
        pnlTable.setBorder(BorderFactory.createTitledBorder("Danh sách theo dõi"));
        pnlTable.add(new JScrollPane(userTable), BorderLayout.CENTER);
        
        add(pnlTable, BorderLayout.CENTER);

        // Events
        btnAdd.addActionListener(e -> {
            String handle = txtHandle.getText().trim();
            String platform = (String) cbPlatform.getSelectedItem();
            if (!handle.isEmpty()) {
                btnAdd.setEnabled(false);
                new Thread(() -> {
                    com.codeanalyzer.crawl.CodeforcesApiClient apiClient = new com.codeanalyzer.crawl.CodeforcesApiClient();
                    JSONObject userInfo = apiClient.getUserInfo(handle);
                    SwingUtilities.invokeLater(() -> {
                        btnAdd.setEnabled(true);
                        if (userInfo != null) {
                            userRepo.addUser(handle, platform);
                            refreshList();
                            txtHandle.setText("");
                            JOptionPane.showMessageDialog(this, "Đã thêm nick " + handle + " (" + platform + ")");
                        } else {
                            JOptionPane.showMessageDialog(this, "Không thể thêm! Tài khoản '" + handle + "' không tồn tại trên Codeforces.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    });
                }).start();
            }
        });

        btnDelete.addActionListener(e -> {
            int row = userTable.getSelectedRow();
            if (row != -1) {
                String handle = (String) tableModel.getValueAt(row, 0);
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa " + handle + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    userRepo.deleteUser(handle);
                    refreshList();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một nick trong bảng để xóa.");
            }
        });

        btnCheck.addActionListener(e -> {
            String handle = txtHandle.getText().trim();
            if (!handle.isEmpty()) {
                btnCheck.setEnabled(false);
                btnCheck.setText("Đang kiểm tra...");
                new Thread(() -> {
                    com.codeanalyzer.crawl.CodeforcesApiClient apiClient = new com.codeanalyzer.crawl.CodeforcesApiClient();
                    JSONObject userInfo = apiClient.getUserInfo(handle);
                    SwingUtilities.invokeLater(() -> {
                        btnCheck.setEnabled(true);
                        btnCheck.setText("Kiểm tra tồn tại");
                        if (userInfo != null) {
                            String rank = userInfo.optString("rank", "unranked");
                            int rating = userInfo.optInt("rating", 0);
                            JOptionPane.showMessageDialog(this, "Tài khoản '" + handle + "' TỒN TẠI.\nRank: " + rank + "\nRating: " + rating, "Kết quả", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Tài khoản '" + handle + "' KHÔNG TỒN TẠI trên Codeforces.", "Kết quả", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                }).start();
            }
        });

        btnEvaluation.addActionListener(e -> showSelectedUserEvaluation());

        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showSelectedUserEvaluation();
                }
            }
        });
        
        refreshList();
    }

    private void showSelectedUserEvaluation() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một Nickname để xem đánh giá!");
            return;
        }
        String handle = tableModel.getValueAt(row, 0).toString();
        
        // Fetch official info in background
        new Thread(() -> {
            com.codeanalyzer.crawl.CodeforcesApiClient apiClient = new com.codeanalyzer.crawl.CodeforcesApiClient();
            JSONObject officialInfo = apiClient.getUserInfo(handle);
            SwingUtilities.invokeLater(() -> showUserEvaluationDialog(handle, officialInfo));
        }).start();
    }

    private void showUserEvaluationDialog(String handle, JSONObject officialInfo) {
        List<String> analyses = subRepo.getAiAnalysesForUser(handle);
        if (analyses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Hiện tại chưa có dữ liệu phân tích AI cho " + handle + ".\nHãy thực hiện Crawl và Phân tích ở tab AI trước.", "Chưa có dữ liệu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalProb = 0;
        int skilledCount = 0;
        Set<String> skills = new HashSet<>();
        Set<String> algos = new HashSet<>();
        
        for (String jsonStr : analyses) {
            try {
                JSONObject json = new JSONObject(jsonStr);
                double prob = json.optDouble("ai_probability", 0);
                totalProb += prob;
                
                String level = json.optString("skill_level", "");
                if ("Giỏi".equals(level)) skilledCount++;
                
                if (prob < 40) { // Chỉ lấy skill từ các bài tự viết
                    String skill = json.optString("skill_level", "");
                    if (!skill.isEmpty()) skills.add(skill);
                    String algo = json.optString("ds_algo", "");
                    if (!algo.isEmpty()) algos.add(algo);
                }
            } catch (Exception ignored) {}
        }

        int totalSub = analyses.size();
        double avgProb = totalProb / totalSub;
        
        // Logic so sánh với thông tin chính thức
        String offRank = "N/A";
        int offRating = 0;
        if (officialInfo != null) {
            offRank = officialInfo.optString("rank", "unranked");
            offRating = officialInfo.optInt("rating", 0);
        }

        String finalStatus;
        if (avgProb > 70) {
            finalStatus = "🔴 KHẢ NGHI CAO (Lạm dụng AI/Copy-paste)";
        } else if (avgProb > 40) {
            finalStatus = "🟡 CÓ DẤU HIỆU (Sử dụng AI hỗ trợ thường xuyên)";
        } else {
            if (totalSub >= 10 && skilledCount > totalSub * 0.3) {
                finalStatus = "💎 THỰC LỰC (Lập trình viên có kỹ năng tốt)";
            } else if (totalSub >= 5) {
                finalStatus = "🟢 TIỀM NĂNG (Tự làm, đang phát triển)";
            } else {
                finalStatus = "⚪ CHƯA RÕ (Cần thêm dữ liệu mẫu)";
            }
        }

        // Kiểm tra sự mâu thuẫn (Discrepancy Check)
        boolean discrepancy = false;
        if (offRating > 1900 && avgProb > 50) discrepancy = true; // Rank cao nhưng AI nghi ngờ

        StringBuilder msg = new StringBuilder();
        msg.append("📊 BÁO CÁO PHÂN TÍCH CHUYÊN SÂU: ").append(handle).append("\n");
        msg.append("==================================================\n\n");
        
        msg.append("🏛️ THÔNG TIN CHÍNH THỨC (CODEFORCES):\n");
        msg.append(" - Xếp hạng (Rank): ").append(offRank.toUpperCase()).append("\n");
        msg.append(" - Điểm số (Rating): ").append(offRating).append("\n\n");

        msg.append("📈 DỮ LIỆU CRAWL & AI (MẪU PHÂN TÍCH):\n");
        msg.append(" - Số lượng mẫu đã quét: ").append(totalSub).append(" bài nộp\n");
        msg.append(" - Xác suất dùng AI trung bình: ").append(String.format("%.1f%%", avgProb)).append("\n\n");
        
        msg.append("🚩 KẾT LUẬN TỪ HỆ THỐNG:\n");
        msg.append(" >> ").append(finalStatus).append("\n");
        if (discrepancy) {
            msg.append(" ⚠️ CẢNH BÁO: Phát hiện sự mâu thuẫn lớn giữa Rank chính thức và thực tế code!");
        }
        msg.append("\n\n");
        
        msg.append("💡 KỸ NĂNG XÁC THỰC (Dựa trên các bài tự viết):\n");
        if (algos.isEmpty()) msg.append(" - (Không đủ dữ liệu mẫu tin cậy để xác định kỹ năng)\n");
        else {
            msg.append(" - Thuật toán tiêu biểu: ");
            algos.stream().limit(5).forEach(a -> msg.append(a).append(", "));
            msg.append("\n - Cấp độ AI nhận diện: ");
            skills.forEach(s -> msg.append(s).append(" "));
        }
        
        msg.append("\n\n--------------------------------------------------\n");
        msg.append("* Ghi chú: Dữ liệu Codeforces lấy thời gian thực từ API.\n");
        msg.append("Kết luận 'Thực lực' chỉ được đưa ra khi có đủ >10 mẫu sạch.");

        JTextArea area = new JTextArea(22, 60);
        area.setText(msg.toString());
        area.setEditable(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(area);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Hồ sơ năng lực: " + handle, true);
        dialog.add(scroll);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void refreshList() {
        tableModel.setRowCount(0);
        List<com.codeanalyzer.model.User> users = userRepo.getAllUsersFull();
        for (com.codeanalyzer.model.User user : users) {
            tableModel.addRow(new Object[]{user.getHandle(), user.getPlatform(), "Active", user.getLastCrawledAt()});
        }
    }
}

