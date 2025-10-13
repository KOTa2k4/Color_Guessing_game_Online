package view;

import client.GameClient;
import model.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GameClientUI extends JFrame {
    private GameClient client;
    private String host;
    private int port;
    private JTextField usernameField;
    private JButton loginBtn;
    private DefaultListModel<String> listModel;
    private JPasswordField passwordField;
    private JList<String> userList;
    private JButton leaderboardBtn;
    private JDialog currentGameDialog;
    private Timer currentTimer;
    private volatile boolean rematchIsPending = false; // <-- THÊM DÒNG NÀY
    private JDialog currentRematchDialog; // Bạn có thể đã có dòng này
    private JLabel yourScoreLabel;
    private JLabel opponentScoreLabel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendChatButton;

    public GameClientUI(String host, int port) throws Exception {
        super("RPS Client");
        this.host = host;
        this.port = port;
        setSize(400, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        // Panel login
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        loginBtn = new JButton("Login");
        loginPanel.add(new JLabel()); // placeholder để giữ cột
        loginPanel.add(loginBtn);

        add(loginPanel, BorderLayout.NORTH);

        // User list
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        add(new JScrollPane(userList), BorderLayout.CENTER);

        // ✅ TẠO PANEL CHAT MỚI
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
        centerSplit.setResizeWeight(0.6); // 60% cho user list, 40% cho chat
        add(centerSplit, BorderLayout.CENTER);

        // Leaderboard button dưới cùng
        leaderboardBtn = new JButton("Leaderboard");
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(leaderboardBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action listeners
        loginBtn.addActionListener(e -> doLogin());
        sendChatButton.addActionListener(e -> sendChatMessage());
        messageField.addActionListener(e -> sendChatMessage());
        leaderboardBtn.addActionListener(e -> {
            try {
                Message m = new Message(Message.Type.LEADERBOARD_REQ);
                client.send(m);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = userList.getSelectedValue();
                if (sel != null) {
                    String[] parts = sel.split(" | ");
                    String name = parts[0].trim();
                    try {
                        Message m = new Message(Message.Type.CHALLENGE);
                        m.data = Map.of("target", name);
                        client.send(m);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    if (client != null)
                        client.close();
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void sendChatMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                Message msg = new Message(Message.Type.CHAT_MESSAGE);
                msg.data = Map.of("message", message);
                client.send(msg);
                messageField.setText(""); // Xóa ô nhập sau khi gửi
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void doLogin() {
        try {
            client = new GameClient(host, port);
            client.setOnMessage(this::handle);

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()); // lấy mật khẩu

            Message login = new Message(Message.Type.LOGIN);
            login.data = Map.of(
                    "username", username,
                    "passwordHash", password);

            client.send(login);
            loginBtn.setEnabled(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm helper để tìm JTextArea
    private JTextArea findChatArea(Container container) {
        for (Component comp : container.getComponents()) {
            if ("inGameChatArea".equals(comp.getName())) {
                return (JTextArea) comp;
            }
            if (comp instanceof Container) {
                JTextArea found = findChatArea((Container) comp);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void handle(Message m) {
        switch (m.type) {
            case LOGIN_OK:
                JOptionPane.showMessageDialog(this, "Login OK");
                break;
            case USER_LIST:
                Map<String, Object> d = m.data;
                List<Map<String, Object>> users = (List<Map<String, Object>>) d.get("users");
                SwingUtilities.invokeLater(() -> {
                    String currentUser = usernameField.getText().trim();
                    listModel.clear();
                    for (Map<String, Object> u : users) {
                        String username = (String) u.get("username");
                        // Chỉ thêm vào danh sách nếu không phải là chính mình
                        if (!username.equals(currentUser)) {
                            String line = String.format("%s | %.1f điểm | %d thắng | %s",
                                    u.get("username"),
                                    ((Number) u.get("points")).doubleValue(),
                                    ((Number) u.get("wins")).intValue(),
                                    u.get("status"));
                            listModel.addElement(line);
                        }
                    }
                });
                break;

            case CHALLENGE_REQ:
                SwingUtilities.invokeLater(() -> { // <-- Bọc trong invokeLater
                    int r = JOptionPane.showConfirmDialog(this, "Challenge from " + m.from + " - accept?", "Challenge",
                            JOptionPane.YES_NO_OPTION);
                    Message resp = new Message(Message.Type.CHALLENGE_RESP);
                    resp.to = m.from;
                    resp.from = usernameField.getText().trim();
                    resp.data = Map.of("accept", r == JOptionPane.YES_OPTION);
                    try {
                        client.send(resp);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case LOGIN_FAIL:
                // Hiển thị thông báo lỗi
                JOptionPane.showMessageDialog(this,
                        "Login failed: " + m.data.getOrDefault("reason", "Unknown"),
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE);

                // Bật lại các trường nhập liệu và nút login
                loginBtn.setEnabled(true);
                usernameField.setEnabled(true);
                passwordField.setEnabled(true);
                passwordField.setText(""); // xóa password cũ
                break;

            case START_GAME:
                String opp = (String) m.data.get("opponent");
                // ✅ Đọc đúng kiểu dữ liệu mới
                List<Map<String, Integer>> colors = (List<Map<String, Integer>>) m.data.get("colors");
                Map<String, Integer> correctColor = (Map<String, Integer>) m.data.get("correctColor");
                // ✅ Đọc điểm số ban đầu
                double yourScore = ((Number) m.data.get("yourScore")).doubleValue();
                double opponentScore = ((Number) m.data.get("opponentScore")).doubleValue();

                // ✅ Truyền điểm số vào hàm hiển thị
                SwingUtilities
                        .invokeLater(() -> showColorGameDialog(opp, colors, correctColor, yourScore, opponentScore));
                break;

            case ROUND_RESULT:
                SwingUtilities.invokeLater(() -> {
                    double sc = ((Number) m.data.get("score")).doubleValue();
                    JOptionPane.showMessageDialog(this, "Round result: score=" + sc
                            + " yourMove=" + m.data.get("yourMove")
                            + " oppMove=" + m.data.get("oppMove"));
                    // ✅ Gửi yêu cầu sang server để bắt đầu round tiếp theo
                    double yourNewScore = ((Number) m.data.get("yourTotalScore")).doubleValue();
                    double opponentNewScore = ((Number) m.data.get("opponentTotalScore")).doubleValue();

                    if (yourScoreLabel != null && opponentScoreLabel != null) {
                        yourScoreLabel.setText(String.format("You: %.0f", yourNewScore));
                        opponentScoreLabel.setText(String.format("Opponent: %.0f", opponentNewScore));
                    }
                    try {
                        Message next = new Message(Message.Type.NEXT_ROUND);
                        client.send(next);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            // Trong phương thức handle(Message m)

            case MATCH_END:
                SwingUtilities.invokeLater(() -> {
                    // 1. Dọn dẹp cửa sổ game như cũ
                    if (currentGameDialog != null) {
                        currentGameDialog.dispose();
                        currentGameDialog = null;
                    }
                    if (currentTimer != null) {
                        currentTimer.cancel();
                        currentTimer = null;
                    }

                    // 2. Hiển thị thông báo kết thúc trận
                    JOptionPane.showMessageDialog(this, "Match ended: " + m.data.get("reason"));

                    // 3. KIỂM TRA VÀ XỬ LÝ YÊU CẦU TÁI ĐẤU ĐANG CHỜ
                    if (rematchIsPending) {
                        rematchIsPending = false; // Reset cờ ngay lập tức

                        // Bây giờ mới hiển thị hộp thoại hỏi tái đấu
                        int r2 = JOptionPane.showConfirmDialog(this,
                                "Opponent wants to play again. Accept?",
                                "Rematch", JOptionPane.YES_NO_OPTION);

                        Message resp2 = new Message(Message.Type.REMATCH_RESP);
                        resp2.data = Map.of("accept", r2 == JOptionPane.YES_OPTION);
                        try {
                            client.send(resp2);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                break;

            // Trong phương thức handle(Message m)
            case CHAT_MESSAGE: {
                String sender = (String) m.data.get("sender");
                String message = (String) m.data.get("message");
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(String.format("[%s]: %s\n", sender, message));
                    // Tự động cuộn xuống cuối
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
                break;
            }

            case IN_GAME_CHAT: {
                // Vì giao diện game (JDialog) có thể phức tạp,
                // chúng ta cần một cách để truy cập vào chat area của nó.
                // Một cách đơn giản là tìm component theo tên.
                if (currentGameDialog != null) {
                    JTextArea inGameChatArea = findChatArea(currentGameDialog);
                    if (inGameChatArea != null) {
                        String sender = (String) m.data.get("sender");
                        String message = (String) m.data.get("message");
                        SwingUtilities.invokeLater(() -> {
                            inGameChatArea.append(String.format("[%s]: %s\n", sender, message));
                            inGameChatArea.setCaretPosition(inGameChatArea.getDocument().getLength());
                        });
                    }
                }
                break;
            }

            case REMATCH_REQ:
                // Chỉ đơn giản là bật cờ lên và không làm gì khác.
                // Giao diện sẽ không thay đổi gì ở bước này.
                rematchIsPending = true;
                break;
            case LEADERBOARD_DATA:
                List<Map<String, Object>> lbUsers = (List<Map<String, Object>>) m.data.get("users");
                StringBuilder sb = new StringBuilder("🏆 Leaderboard:\n");
                int rank = 1;
                for (Map<String, Object> u : lbUsers) {
                    sb.append(rank++).append(". ")
                            .append(u.get("username"))
                            .append(" - ").append(u.get("points")).append(" điểm")
                            .append(" - ").append(u.get("wins")).append(" thắng\n");
                }
                JOptionPane.showMessageDialog(this, sb.toString());
                break;

            default:
                break;
        }
    }

    private void showColorGameDialog(String opponent, List<Map<String, Integer>> colors,
            Map<String, Integer> correctColor, double yourScore, double opponentScore) {
        JDialog dialog = new JDialog(this, "Đoán màu vs " + opponent, true);
        currentGameDialog = dialog;
        dialog.setLayout(new BorderLayout());

        // ✅ TẠO PANEL MỚI ĐỂ CHỨA ĐIỂM SỐ
        JPanel scorePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        yourScoreLabel = new JLabel(String.format("You: %.0f", yourScore), SwingConstants.LEFT);
        opponentScoreLabel = new JLabel(String.format("Opponent: %.0f", opponentScore), SwingConstants.RIGHT);

        yourScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        opponentScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));

        scorePanel.add(yourScoreLabel);
        scorePanel.add(opponentScoreLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel infoLbl = new JLabel("Chọn màu đúng trong 15 giây!", SwingConstants.CENTER);
        infoLbl.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(infoLbl, BorderLayout.CENTER);

        JLabel countdownLbl = new JLabel("15", SwingConstants.CENTER);
        countdownLbl.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(countdownLbl, BorderLayout.EAST);
        dialog.add(topPanel, BorderLayout.NORTH);

        topPanel.add(scorePanel, BorderLayout.SOUTH);

        // ✅ TÍNH TOÁN LAYOUT MỘT CÁCH TỰ ĐỘNG
        int numColors = colors.size();
        int columns = 3; // Giữ cố định 3 cột cho đẹp
        // Tính số hàng cần thiết. Math.ceil đảm bảo làm tròn lên.
        // Ví dụ: 8 màu / 3 cột = 2.66 -> làm tròn thành 3 hàng.
        int rows = (int) Math.ceil((double) numColors / columns);
        // ✅ TẠO LAYOUT ĐỘNG
        JPanel colorPanel = new JPanel(new GridLayout(rows, columns, 10, 10));
        dialog.add(colorPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton sendBtn = new JButton("Gửi");
        JButton exitBtn = new JButton("Thoát");
        btnPanel.add(sendBtn);
        btnPanel.add(exitBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        // --- TẠO GIAO DIỆN CHAT ---
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setPreferredSize(new Dimension(150, 0)); // Đặt chiều rộng cho khung chat
        chatPanel.setBorder(BorderFactory.createTitledBorder("Match Chat"));

        JTextArea inGameChatArea = new JTextArea();
        inGameChatArea.setName("inGameChatArea"); // ✅ Đặt tên để có thể tìm thấy
        inGameChatArea.setEditable(false);
        inGameChatArea.setLineWrap(true);
        chatPanel.add(new JScrollPane(inGameChatArea), BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(5, 5));
        JTextField inGameMessageField = new JTextField();
        JButton sendInGameChatButton = new JButton("Send");
        messageInputPanel.add(inGameMessageField, BorderLayout.CENTER);
        messageInputPanel.add(sendInGameChatButton, BorderLayout.EAST);
        chatPanel.add(messageInputPanel, BorderLayout.SOUTH);

        // Thêm listener để gửi tin nhắn
        ActionListener sendAction = e -> {
            String message = inGameMessageField.getText().trim();
            if (!message.isEmpty()) {
                try {
                    Message msg = new Message(Message.Type.IN_GAME_CHAT);
                    msg.data = Map.of("message", message);
                    client.send(msg);
                    inGameMessageField.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        sendInGameChatButton.addActionListener(sendAction);
        inGameMessageField.addActionListener(sendAction);
        dialog.add(chatPanel, BorderLayout.EAST);
        dialog.setSize(600, 400); // Tăng chiều rộng để có chỗ cho khung chat
        dialog.setLocationRelativeTo(this);

        // tạo nút
        JToggleButton[] buttons = new JToggleButton[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Map<String, Integer> colorMap = colors.get(i);
            int r = colorMap.get("r");
            int g = colorMap.get("g");
            int b = colorMap.get("b");

            JToggleButton btn = new JToggleButton("");

            // ✅ Tạo màu từ giá trị RGB nhận được
            btn.setBackground(new Color(r, g, b));

            // ✅ "Thẻ tên vô hình" giờ sẽ là chuỗi "r,g,b"
            String colorValue = String.format("%d,%d,%d", r, g, b);
            btn.putClientProperty("colorValue", colorValue);

            buttons[i] = btn;
        }

        Timer timer = new Timer();
        currentTimer = timer;

        // Step 1: Hiển thị màu đúng 2s
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    int correctR = correctColor.get("r");
                    int correctG = correctColor.get("g");
                    int correctB = correctColor.get("b");
                    Color correctColorObj = new Color(correctR, correctG, correctB);
                    String correctValue = String.format("%d,%d,%d", correctR, correctG, correctB);

                    for (JToggleButton btn : buttons) {
                        String btnValue = (String) btn.getClientProperty("colorValue");
                        if (btnValue.equals(correctValue)) {
                            btn.setBackground(correctColorObj);
                        } else {
                            btn.setBackground(Color.GRAY);
                        }
                        colorPanel.add(btn);
                    }
                    colorPanel.revalidate();
                    colorPanel.repaint();
                    infoLbl.setText("Nhìn kỹ màu đúng!");
                });
            }
        }, 1000);

        // Step 2: Đen 2s
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    for (JToggleButton btn : buttons) {
                        btn.setBackground(Color.BLACK);
                    }
                    infoLbl.setText("Đang xáo vị trí...");
                });
            }
        }, 2000);

        // Step 3: Xáo vị trí, giữ màu gốc, cho chọn
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    java.util.List<JToggleButton> btnList = new java.util.ArrayList<>(java.util.List.of(buttons));
                    java.util.Collections.shuffle(btnList);
                    colorPanel.removeAll();
                    for (JToggleButton btn : btnList) {
                        String rgbString = (String) btn.getClientProperty("colorValue");
                        String[] rgbParts = rgbString.split(",");
                        int r = Integer.parseInt(rgbParts[0]);
                        int g = Integer.parseInt(rgbParts[1]);
                        int b = Integer.parseInt(rgbParts[2]);
                        btn.setBackground(new Color(r, g, b));
                        btn.addActionListener(e -> {
                            for (JToggleButton bi : buttons) {
                                bi.setSelected(bi == btn);
                            }
                        });
                        colorPanel.add(btn);
                    }
                    colorPanel.revalidate();
                    colorPanel.repaint();
                    infoLbl.setText("Chọn màu đúng!");
                });
            }
        }, 5000);

        // countdown
        int[] secondsLeft = { 15 };
        Timer countdownTimer = new Timer();
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    secondsLeft[0]--;
                    countdownLbl.setText(String.valueOf(secondsLeft[0]));
                    if (secondsLeft[0] <= 0) {
                        countdownTimer.cancel();
                        // tự động gửi move nếu chưa chọn
                        JToggleButton selected = null;
                        for (JToggleButton b : buttons)
                            if (b.isSelected())
                                selected = b;
                        if (selected == null)
                            selected = buttons[new Random().nextInt(buttons.length)];
                        try {
                            Message msg = new Message(Message.Type.MOVE);
                            String selectedColor = (String) selected.getClientProperty("colorValue");
                            msg.data = Map.of("move", selectedColor); // ✅ Sửa ở đây);
                            client.send(msg);
                            timer.cancel();
                            dialog.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        }, 1000, 1000);

        // nút gửi
        sendBtn.addActionListener(e -> {
            for (JToggleButton btn : buttons) {
                if (btn.isSelected()) {
                    try {
                        // ✅ BƯỚC 2: Lấy tên màu từ "thẻ tên vô hình" thay vì getText()
                        String selectedColor = (String) btn.getClientProperty("colorValue");

                        Message msg = new Message(Message.Type.MOVE);
                        msg.data = Map.of("move", selectedColor);
                        client.send(msg);
                        timer.cancel();
                        countdownTimer.cancel();
                        dialog.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    break;
                }
            }
        });

        // nút thoát
        exitBtn.addActionListener(e -> {
            timer.cancel();
            countdownTimer.cancel();
            dialog.dispose();
            try {
                Message exitMsg = new Message(Message.Type.EXIT);
                exitMsg.data = Map.of("reason", "player_left");
                client.send(exitMsg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        dialog.pack();
        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
