package client.view;

import client.control.ClientController;
import client.GameClient;
import shared.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp View (Giao diện) cho sảnh chờ.
 * Chịu trách nhiệm hiển thị form đăng nhập, danh sách người dùng và chat sảnh
 * chờ.
 * Nó không biết gì về logic game.
 */
public class LobbyView extends JFrame {
    private GameClient client;
    private ClientController controller; // ✅ Giữ tham chiếu đến Controller

    // --- Các thành phần UI của sảnh chờ ---
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JButton registerBtn;
    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendChatButton;
    private JButton leaderboardBtn;
    private JPanel loginPanel; // <-- THÊM DÒNG NÀY
    private JSplitPane centerSplit; // <-- THÊM DÒNG NÀY
    private JPanel bottomPanel; // <-- THÊM DÒNG NÀY
    private JButton logoutBtn;
    private JButton btnAdminPanel;

    public LobbyView(String host, int port) throws Exception {
        super("Game Lobby");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        // --- Panel login ---
        loginPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);
        loginBtn = new JButton("Login");
        loginPanel.add(new JLabel());
        loginPanel.add(loginBtn);
        registerBtn = new JButton("Register"); // <-- NÚT MỚI
        loginPanel.add(registerBtn);
        add(loginPanel, BorderLayout.NORTH);

        // --- Panel trung tâm (User List và Chat) ---
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);

        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Lobby Chat"));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(5, 5));
        messageField = new JTextField();
        sendChatButton = new JButton("Send");
        messageInputPanel.add(messageField, BorderLayout.CENTER);
        messageInputPanel.add(sendChatButton, BorderLayout.EAST);
        chatPanel.add(messageInputPanel, BorderLayout.SOUTH);

        centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(userList), chatPanel);
        centerSplit.setResizeWeight(0.6);
        add(centerSplit, BorderLayout.CENTER);

        // --- Panel dưới cùng ---
        // --- Panel dưới cùng ---
        leaderboardBtn = new JButton("Leaderboard");
        JButton btnHistory = new JButton("Lịch sử đấu");
        logoutBtn = new JButton("Đăng xuất");
        btnAdminPanel = new JButton("Admin Panel");
        btnAdminPanel.setVisible(false); // Ẩn ban đầu

        bottomPanel = new JPanel(); // <-- Khởi tạo JPanel TRƯỚC
        bottomPanel.add(leaderboardBtn);
        bottomPanel.add(btnHistory);
        bottomPanel.add(logoutBtn);
        bottomPanel.add(btnAdminPanel); // <-- Thêm nút Admin vào đây
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        sendChatButton.addActionListener(e -> sendChatMessage());
        logoutBtn.addActionListener(e -> resetToLoginUI());
        messageField.addActionListener(e -> sendChatMessage()); // Gửi khi nhấn Enter

        leaderboardBtn.addActionListener(e -> {
            try {
                client.send(new Message(Message.Type.LEADERBOARD_REQ));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnAdminPanel.addActionListener(e -> {
            try {
                client.send(new Message(Message.Type.GET_MATCH_LIST));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        btnHistory.addActionListener(e -> {
            try {
                client.send(new Message(Message.Type.GET_MATCH_HISTORY));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    String targetUsername = selected.split(" \\| ")[0].trim();
                    try {
                        Message m = new Message(Message.Type.CHALLENGE);
                        m.data = Map.of("target", targetUsername);
                        client.send(m);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (client != null)
                    try {
                        client.close();
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
            }
        });
        // ... (code addWindowListener)

        // --- Trạng thái ban đầu ---
        // Ẩn sảnh chờ, chỉ hiện màn hình đăng nhập
        loginPanel.setVisible(true);
        centerSplit.setVisible(false);
        bottomPanel.setVisible(false);
    }

    private void doLogin() {
        try {
            client = new GameClient("localhost", 55555); // Thay đổi host/port nếu cần
            controller = new ClientController(client);
            controller.setLobbyView(this);

            // ✅ Khi nhận được tin nhắn, GameClient sẽ gọi controller để xử lý
            client.setOnMessage(controller::handle);

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            Message loginMsg = new Message(Message.Type.LOGIN);
            loginMsg.data = Map.of("username", username, "passwordHash", password);
            client.send(loginMsg);

            loginBtn.setEnabled(false);
            registerBtn.setEnabled(false);
            passwordField.setEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cannot connect to server.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    // THÊM PHƯƠNG THỨC NÀY VÀO LOBBYVIEW.JAVA (bên dưới doLogin())

    private void doRegister() {
        try {
            client = new GameClient("localhost", 55555); // Thay đổi host/port nếu cần
            controller = new ClientController(client);
            controller.setLobbyView(this);

            // ✅ Khi nhận được tin nhắn, GameClient sẽ gọi controller để xử lý
            client.setOnMessage(controller::handle);

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            // SỬA CHỖ NÀY:
            Message registerMsg = new Message(Message.Type.REGISTER_REQ);
            registerMsg.data = Map.of("username", username, "passwordHash", password);
            client.send(registerMsg);

            loginBtn.setEnabled(false); // Vẫn vô hiệu hóa
            registerBtn.setEnabled(false);
            passwordField.setEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cannot connect to server.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    // THÊM PHƯƠNG THỨC NÀY VÀO LOBBYVIEW.JAVA

    public void showRegisterError(Message m) {
        JOptionPane.showMessageDialog(this, "Register failed: " + m.data.getOrDefault("reason", "Unknown"),
                "Register Failed", JOptionPane.ERROR_MESSAGE);
        loginBtn.setEnabled(true);
        registerBtn.setEnabled(true);
        passwordField.setEnabled(true);
    }

    private void sendChatMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                Message msg = new Message(Message.Type.CHAT_MESSAGE);
                msg.data = Map.of("message", message);
                client.send(msg);
                messageField.setText("");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // ❌ TOÀN BỘ PHƯƠNG THỨC handle() VÀ showColorGameDialog() ĐÃ BỊ XÓA BỎ

    // --- CÁC PHƯƠNG THỨC CÔNG KHAI ĐỂ CONTROLLER CẬP NHẬT GIAO DIỆN ---

    public void updateUserList(Message m) {
        List<Map<String, Object>> users = (List<Map<String, Object>>) m.data.get("users");
        String currentUser = usernameField.getText().trim();
        listModel.clear();
        for (Map<String, Object> u : users) {
            String username = (String) u.get("username");
            if (!username.equals(currentUser)) {
                String line = String.format("%s | %.1f điểm | %d thắng | %s",
                        u.get("username"), ((Number) u.get("points")).doubleValue(),
                        ((Number) u.get("wins")).intValue(), u.get("status"));
                listModel.addElement(line);
            }
        }
    }

    public void appendChatMessage(Message m) {
        String sender = (String) m.data.get("sender");
        String message = (String) m.data.get("message");
        chatArea.append(String.format("[%s]: %s\n", sender, message));
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    public void showLoginError(Message m) {
        JOptionPane.showMessageDialog(this, "Login failed: " + m.data.getOrDefault("reason", "Unknown"),
                "Login Failed", JOptionPane.ERROR_MESSAGE);
        loginBtn.setEnabled(true);
        registerBtn.setEnabled(true);
        passwordField.setEnabled(true);
    }

    public void showChallengeRequest(Message m) {
        int choice = JOptionPane.showConfirmDialog(this, "Challenge from " + m.from + ". Accept?",
                "Challenge", JOptionPane.YES_NO_OPTION);
        try {
            Message response = new Message(Message.Type.CHALLENGE_RESP);
            response.to = m.from;
            response.data = Map.of("accept", choice == JOptionPane.YES_OPTION);
            client.send(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLeaderboard(Message m) {
        List<Map<String, Object>> lbUsers = (List<Map<String, Object>>) m.data.get("users");
        StringBuilder sb = new StringBuilder("🏆 Leaderboard:\n");
        int rank = 1;
        for (Map<String, Object> u : lbUsers) {
            sb.append(String.format("%d. %s - %.1f điểm - %d thắng\n",
                    rank++, u.get("username"), ((Number) u.get("points")).doubleValue(),
                    ((Number) u.get("wins")).intValue()));
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
    }

    // THÊM PHƯƠNG THỨC MỚI NÀY VÀO CUỐI TỆP LobbyView.java

    /**
     * Tạo và hiển thị một JDialog mới chứa lịch sử đấu.
     * 
     * @param history Danh sách lịch sử
     */
    public void showMatchHistoryDialog(List<shared.model.MatchRecord> history) {
        // Tạo cửa sổ dialog mới, đặt 'this' (LobbyView) làm cửa sổ cha
        JDialog historyDialog = new JDialog(this, "Lịch sử đấu", true);
        historyDialog.setLayout(new BorderLayout());
        historyDialog.setSize(700, 400); // Kích thước cửa sổ

        if (history == null || history.isEmpty()) {
            historyDialog.add(new JLabel("Không tìm thấy trận đấu nào.", SwingConstants.CENTER));
        } else {
            // Dùng DefaultListModel để đưa danh sách vào JList
            DefaultListModel<shared.model.MatchRecord> listModel = new DefaultListModel<>();
            for (shared.model.MatchRecord record : history) {
                // Tận dụng hàm .toString() bạn đã viết rất tốt trong MatchRecord
                listModel.addElement(record);
            }

            JList<shared.model.MatchRecord> historyList = new JList<>(listModel);

            // Dùng font Monospaced giúp các cột hiển thị thẳng hàng
            historyList.setFont(new Font("Monospaced", Font.PLAIN, 12));

            // Thêm thanh cuộn
            JScrollPane scrollPane = new JScrollPane(historyList);
            historyDialog.add(scrollPane, BorderLayout.CENTER);
        }

        // Nút Đóng
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> historyDialog.dispose());

        JPanel southPanel = new JPanel();
        southPanel.add(btnClose);
        historyDialog.add(southPanel, BorderLayout.SOUTH);

        historyDialog.setLocationRelativeTo(this); // Hiển thị ở giữa lobby
        historyDialog.setVisible(true);
    }
    // THÊM 2 PHƯƠNG THỨC NÀY VÀO CUỐI LobbyView.java

    /**
     * Được gọi khi đăng nhập thành công.
     * Ẩn panel login, hiện panel sảnh chờ.
     */
    public void showLobbyUI(boolean isAdmin) {
        loginPanel.setVisible(false);
        centerSplit.setVisible(true);
        bottomPanel.setVisible(true);
        btnAdminPanel.setVisible(isAdmin);
        // Vô hiệu hóa các trường không cần nữa
        usernameField.setEnabled(false);
        passwordField.setEnabled(false);
        loginBtn.setEnabled(false);
        registerBtn.setEnabled(false);
    }

    /**
     * Được gọi khi nhấn nút Đăng xuất.
     * Gửi tin nhắn LOGOUT, đóng client, và reset UI về màn hình đăng nhập.
     */
    private void resetToLoginUI() {
        // 1. Gửi tin nhắn LOGOUT và đóng client
        try {
            if (client != null) {
                client.send(new Message(Message.Type.LOGOUT));
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        client = null;
        controller = null;

        // 2. Reset UI về trạng thái ban đầu
        loginPanel.setVisible(true);
        centerSplit.setVisible(false);
        bottomPanel.setVisible(false);

        // Kích hoạt lại các trường
        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        passwordField.setText(""); // Xóa pass cũ
        loginBtn.setEnabled(true);
        registerBtn.setEnabled(true);

        // Xóa dữ liệu cũ
        listModel.clear();
        chatArea.setText("");
    }

    // THÊM HÀM MỚI NÀY VÀO LobbyView.java
    public void showMatchListDialog(List<Map<String, String>> matches) {
        JDialog dialog = new JDialog(this, "Active Matches", true);
        DefaultListModel<String> listModel = new DefaultListModel<>();

        Map<String, String> matchIdMap = new HashMap<>();

        for (Map<String, String> match : matches) {
            String id = match.get("id");
            String p1 = match.get("p1");
            String p2 = match.get("p2");
            String displayText = String.format("%s vs %s", p1, p2);
            listModel.addElement(displayText);
            matchIdMap.put(displayText, id); // Lưu ID
        }

        JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = list.getSelectedValue();
                if (selected == null)
                    return;

                String matchId = matchIdMap.get(selected);
                try {
                    Message m = new Message(Message.Type.SPECTATE_REQ);
                    m.data = Map.of("matchId", matchId);
                    client.send(m);
                    dialog.dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        dialog.add(new JScrollPane(list));
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}