package client.view;

import client.control.ClientController;
import client.GameClient;
import shared.model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendChatButton;
    private JButton leaderboardBtn;

    public LobbyView(String host, int port) throws Exception {
        super("Game Lobby");
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        // --- Panel login ---
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 5, 5));
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

        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(userList), chatPanel);
        centerSplit.setResizeWeight(0.6);
        add(centerSplit, BorderLayout.CENTER);

        // --- Panel dưới cùng ---
        leaderboardBtn = new JButton("Leaderboard");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(leaderboardBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Action Listeners ---
        loginBtn.addActionListener(e -> doLogin());
        sendChatButton.addActionListener(e -> sendChatMessage());
        messageField.addActionListener(e -> sendChatMessage()); // Gửi khi nhấn Enter

        leaderboardBtn.addActionListener(e -> {
            try {
                client.send(new Message(Message.Type.LEADERBOARD_REQ));
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
            passwordField.setEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Cannot connect to server.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
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
}