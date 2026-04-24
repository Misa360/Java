package com.codeanalyzer.ui.panel;

import com.codeanalyzer.repository.UserRepository;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private UserRepository userRepo = new UserRepository();
    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(listModel);

    public UserManagementPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Danh sách Nick theo dõi"));

        add(new JScrollPane(userList), BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel();
        JButton btnAdd = new JButton("Thêm Nick");
        JButton btnRefresh = new JButton("Làm mới");
        pnlBottom.add(btnAdd);
        pnlBottom.add(btnRefresh);
        add(pnlBottom, BorderLayout.SOUTH);

        btnAdd.addActionListener(e -> {
            String handle = JOptionPane.showInputDialog(this, "Nhập handle Codeforces:");
            if (handle != null && !handle.trim().isEmpty()) {
                userRepo.addUser(handle.trim());
                refreshList();
            }
        });

        btnRefresh.addActionListener(e -> refreshList());

        refreshList();
    }

    public void refreshList() {
        listModel.clear();
        List<String> users = userRepo.getAllUsers();
        for (String user : users) {
            listModel.addElement(user);
        }
    }
}
