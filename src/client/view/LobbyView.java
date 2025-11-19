package client.view;

import client.control.ClientController;
import client.GameClient;
import shared.model.MatchRecord;
import shared.model.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyView extends JFrame {
    private GameClient client;
    private ClientController controller;

    // UI Components
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginBtn;
    private JButton registerBtn;
    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JTextArea chatArea;
    private JTextArea lbPreview; // ← THÊM DÒNG NÀY
    private JTextField messageField;
    private JButton sendChatButton;
    private JButton leaderboardBtn;
    private JButton logoutBtn;
    private JButton btnHistory;
    private JButton btnAdminPanel;

    // Panels để ẩn/hiện
    private NeonCard loginCard;
    private JPanel bottomPanel;

    public LobbyView(String host, int port) throws Exception {
        super("GAME LOBBY - NEON EDITION");
        setSize(1150, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Background neon + stars
        setContentPane(new BackgroundPanel());
        getContentPane().setLayout(new BorderLayout());

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // ===================== HEADER =====================
        TitlePanel titlePanel = new TitlePanel("GAME LOBBY");
        titlePanel.setPreferredSize(new Dimension(0, 100));
        getContentPane().add(titlePanel, BorderLayout.NORTH);

        // ===================== MAIN SPLIT =====================
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerSize(8);
        mainSplit.setContinuousLayout(true);
        mainSplit.setOpaque(false);
        mainSplit.setBorder(null);

        // ===================== LEFT PANEL (Login + Leaderboard) =====================
        JPanel leftWrapper = new JPanel(new BorderLayout(12, 12));
        leftWrapper.setOpaque(false);
        leftWrapper.setBorder(new EmptyBorder(18, 18, 18, 10));

        // Login Card
        loginCard = new NeonCard(new Color(255, 0, 180), new Color(55, 0, 90));
        loginCard.setPreferredSize(new Dimension(380, 340));
        loginCard.setLayout(null);

        JLabel signLabel = new JLabel("SIGN IN");
        signLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        signLabel.setForeground(Color.WHITE);
        signLabel.setBounds(24, 18, 300, 34);
        loginCard.add(signLabel);

        usernameField = new JTextField();
        usernameField.setBounds(24, 76, 332, 50);
        styleInput(usernameField);
        loginCard.add(usernameField);

        passwordField = new JPasswordField();
        passwordField.setBounds(24, 140, 332, 50);
        styleInput(passwordField);
        loginCard.add(passwordField);

        loginBtn = new NeonButton("LOGIN", new Color(0, 220, 255), new Color(0, 120, 180));
        loginBtn.setBounds(40, 220, 140, 50);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        loginCard.add(loginBtn);

        registerBtn = new NeonButton("REGISTER", new Color(255, 0, 180), new Color(120, 0, 80));
        registerBtn.setBounds(200, 220, 156, 50);
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginCard.add(registerBtn);

        leftWrapper.add(loginCard, BorderLayout.NORTH);

        // Leaderboard preview card
        NeonCard leaderboardCard = new NeonCard(new Color(0, 220, 255), new Color(0, 50, 90));
        leaderboardCard.setLayout(new BorderLayout());
        leaderboardCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        leaderboardCard.setPreferredSize(new Dimension(380, 220));

        JLabel lbTitle = new JLabel("LEADERBOARD PREVIEW");
        lbTitle.setForeground(Color.WHITE);
        lbTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        leaderboardCard.add(lbTitle, BorderLayout.NORTH);

        this.lbPreview = new JTextArea("Đang tải bảng xếp hạng...");
        this.lbPreview.setEditable(false);
        this.lbPreview.setBackground(new Color(10, 0, 20));
        this.lbPreview.setForeground(Color.CYAN);
        this.lbPreview.setFont(new Font("Consolas", Font.PLAIN, 14));
        this.lbPreview.setLineWrap(true);
        this.lbPreview.setWrapStyleWord(true);
        leaderboardCard.add(new JScrollPane(this.lbPreview), BorderLayout.CENTER);

        leaderboardBtn = new NeonButton("LEADERBOARD", new Color(255, 0, 180), new Color(160, 0, 100));
        JPanel btnWrap = new JPanel();
        btnWrap.setOpaque(false);
        btnWrap.add(leaderboardBtn);
        leaderboardCard.add(btnWrap, BorderLayout.SOUTH);

        leftWrapper.add(leaderboardCard, BorderLayout.CENTER);
        mainSplit.setLeftComponent(leftWrapper);

        // ===================== RIGHT PANEL (Users + Chat) =====================
        JPanel rightWrapper = new JPanel(new BorderLayout(12, 12));
        rightWrapper.setOpaque(false);
        rightWrapper.setBorder(new EmptyBorder(18, 10, 18, 18));

        // Users Card
        NeonCard usersCard = new NeonCard(new Color(0, 210, 255), new Color(0, 40, 80));
        usersCard.setLayout(new BorderLayout(8, 8));
        usersCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel usersLabel = new JLabel("ONLINE PLAYERS");
        usersLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        usersLabel.setForeground(Color.WHITE);
        usersCard.add(usersLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setCellRenderer(new AvatarListCellRenderer());
        userList.setBackground(new Color(8, 0, 18));
        userList.setForeground(Color.WHITE);

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setOpaque(false);
        userScroll.getViewport().setOpaque(false);
        usersCard.add(userScroll, BorderLayout.CENTER);
        rightWrapper.add(usersCard, BorderLayout.NORTH);

        // Chat Card
        NeonCard chatCard = new NeonCard(new Color(255, 80, 255), new Color(30, 0, 30));
        chatCard.setLayout(new BorderLayout(8, 8));
        chatCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel chatLabel = new JLabel("LOBBY CHAT");
        chatLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        chatLabel.setForeground(Color.WHITE);
        chatCard.add(chatLabel, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(new Color(6, 0, 12));
        chatArea.setForeground(Color.WHITE);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        chatCard.add(chatScroll, BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(8, 8));
        inputRow.setOpaque(false);

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        styleInput(messageField);

        sendChatButton = new NeonButton("SEND", new Color(255, 0, 150), new Color(120, 0, 80));
        sendChatButton.setPreferredSize(new Dimension(120, 46));

        inputRow.add(messageField, BorderLayout.CENTER);
        inputRow.add(sendChatButton, BorderLayout.EAST);
        chatCard.add(inputRow, BorderLayout.SOUTH);

        rightWrapper.add(chatCard, BorderLayout.CENTER);
        mainSplit.setRightComponent(rightWrapper);
        mainSplit.setDividerLocation(400);

        getContentPane().add(mainSplit, BorderLayout.CENTER);

        // ===================== BOTTOM PANEL (sau khi login) =====================
        bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 10));

        btnHistory = new NeonButton("LỊCH SỬ ĐẤU", new Color(180, 255, 100), new Color(80, 120, 0));
        logoutBtn = new NeonButton("ĐĂNG XUẤT", new Color(255, 80, 80), new Color(140, 0, 0));
        btnAdminPanel = new NeonButton("ADMIN PANEL", new Color(255, 215, 0), new Color(180, 120, 0));
        btnAdminPanel.setVisible(false);

        bottomPanel.add(leaderboardBtn);
        bottomPanel.add(btnHistory);
        bottomPanel.add(logoutBtn);
        bottomPanel.add(btnAdminPanel);

        getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.setVisible(false); // ẩn lúc đầu

        // ===================== LISTENERS =====================
        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());
        sendChatButton.addActionListener(e -> sendChatMessage());
        messageField.addActionListener(e -> sendChatMessage());

        leaderboardBtn.addActionListener(e -> sendToServer(Message.Type.LEADERBOARD_REQ));
        btnHistory.addActionListener(e -> sendToServer(Message.Type.GET_MATCH_HISTORY));
        logoutBtn.addActionListener(e -> resetToLoginUI());
        btnAdminPanel.addActionListener(e -> sendToServer(Message.Type.GET_MATCH_LIST));

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    String target = selected.split(" \\| ")[0].trim();
                    sendToServer(Message.Type.CHALLENGE, Map.of("target", target));
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) try { client.close(); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    private void sendToServer(Message.Type type) {
        sendToServer(type, null);
    }

    private void sendToServer(Message.Type type, Map<String, Object> data) {
        if (client == null) return;
        try {
            Message m = new Message(type);
            if (data != null) m.data = data;
            client.send(m);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void doLogin() {
        connectAndSend(Message.Type.LOGIN);
    }

    private void doRegister() {
        connectAndSend(Message.Type.REGISTER_REQ);
    }

    private void connectAndSend(Message.Type type) {
        try {
            client = new GameClient("172.11.42.188", 55555); // thay host nếu cần
            controller = new ClientController(client);
            controller.setLobbyView(this);
            client.setOnMessage(controller::handle);

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            Message msg = new Message(type);
            msg.data = Map.of("username", username, "passwordHash", password);
            client.send(msg);

            loginBtn.setEnabled(false);
            registerBtn.setEnabled(false);
            passwordField.setEnabled(false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể kết nối server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== UI CONTROL =====================
    public void showLobbyUI(boolean isAdmin) {
        loginCard.setVisible(false);
        bottomPanel.setVisible(true);
        btnAdminPanel.setVisible(isAdmin);
    }

    private void resetToLoginUI() {
        try {
            if (client != null) {
                client.send(new Message(Message.Type.LOGOUT));
                client.close();
            }
        } catch (Exception ignored) {}
        client = null;
        controller = null;

        loginCard.setVisible(true);
        bottomPanel.setVisible(false);
        btnAdminPanel.setVisible(false);

        usernameField.setEnabled(true);
        passwordField.setEnabled(true);
        passwordField.setText("");
        loginBtn.setEnabled(true);
        registerBtn.setEnabled(true);

        listModel.clear();
        chatArea.setText("");
    }

    private void sendChatMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty() && client != null) {
            sendToServer(Message.Type.CHAT_MESSAGE, Map.of("message", text));
            messageField.setText("");
        }
    }

    // ===================== CÁC HÀM PUBLIC CHO CONTROLLER =====================
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

    public void showRegisterError(Message m) {
        JOptionPane.showMessageDialog(this, "Register failed: " + m.data.getOrDefault("reason", "Unknown"),
                "Register Failed", JOptionPane.ERROR_MESSAGE);
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void showLeaderboard(Message m) {
        List<Map<String, Object>> lbUsers = (List<Map<String, Object>>) m.data.get("users");
        StringBuilder sb = new StringBuilder("LEADERBOARD:\n");
        int rank = 1;
        for (Map<String, Object> u : lbUsers) {
            sb.append(String.format("%d. %s - %.1f điểm - %d thắng\n",
                    rank++, u.get("username"), ((Number) u.get("points")).doubleValue(),
                    ((Number) u.get("wins")).intValue()));
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showMatchHistoryDialog(List<MatchRecord> history) {
        JDialog d = new JDialog(this, "Lịch sử đấu", true);
        d.setSize(700, 450);
        d.setLayout(new BorderLayout());

        if (history == null || history.isEmpty()) {
            d.add(new JLabel("Không có lịch sử đấu nào.", SwingConstants.CENTER));
        } else {
            DefaultListModel<MatchRecord> lm = new DefaultListModel<>();
            history.forEach(lm::addElement);
            JList<MatchRecord> list = new JList<>(lm);
            list.setFont(new Font("Monospaced", Font.PLAIN, 13));
            d.add(new JScrollPane(list), BorderLayout.CENTER);
        }

        JButton close = new JButton("Đóng");
        close.addActionListener(e -> d.dispose());
        JPanel p = new JPanel();
        p.add(close);
        d.add(p, BorderLayout.SOUTH);

        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    public void showMatchListDialog(List<Map<String, String>> matches) {
        JDialog d = new JDialog(this, "Active Matches", true);
        DefaultListModel<String> lm = new DefaultListModel<>();
        Map<String, String> idMap = new HashMap<>();

        for (Map<String, String> m : matches) {
            String txt = m.get("p1") + " vs " + m.get("p2");
            lm.addElement(txt);
            idMap.put(txt, m.get("id"));
        }

        JList<String> list = new JList<>(lm);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = list.getSelectedValue();
                if (sel != null) {
                    try {
                        Message msg = new Message(Message.Type.SPECTATE_REQ);
                        msg.data = Map.of("matchId", idMap.get(sel));
                        client.send(msg);
                        d.dispose();
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        });

        d.add(new JScrollPane(list));
        d.setSize(320, 400);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // ===================== HELPER STYLE =====================
    private void styleInput(JTextField tf) {
        tf.setBackground(new Color(12, 0, 20));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 220), 2),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private void styleInput(JPasswordField pf) {
        styleInput((JTextField) pf);
    }

    // ===================== NEON CUSTOM COMPONENTS =====================
    private static class BackgroundPanel extends JPanel {
        private final BufferedImage stars;

        BackgroundPanel() {
            setOpaque(true);
            stars = createStarField(1200, 800, 150);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, new Color(8, 0, 20), 0, getHeight(), new Color(18, 0, 40));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (stars != null) g2.drawImage(stars, 0, 0, null);
            g2.dispose();
        }

        private static BufferedImage createStarField(int w, int h, int count) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            for (int i = 0; i < count; i++) {
                int x = (int) (Math.random() * w);
                int y = (int) (Math.random() * h);
                int alpha = (int) (Math.random() * 180 + 70);
                g.setColor(new Color(255, 255, 255, alpha));
                g.fillOval(x, y, 2, 2);
            }
            g.dispose();
            return img;
        }
    }

    private static class NeonCard extends JPanel {
        private final Color glowColor;
        private final Color innerColor;

        NeonCard(Color glow, Color inner) {
            this.glowColor = glow;
            this.innerColor = inner;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), arc = 20;

            for (int i = 18; i >= 4; i -= 4) {
                float alpha = 0.08f * (1.0f - (i / 22f));
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int)(alpha*255)));
                g2.fillRoundRect(-i/2, -i/2, w + i, h + i, arc + i, arc + i);
            }

            GradientPaint gp = new GradientPaint(0, 0, innerColor.brighter(), 0, h, innerColor.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(glowColor);
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NeonButton extends JButton {
        private final Color glow;
        private final Color base;

        NeonButton(String text, Color glow, Color base) {
            super(text);
            this.glow = glow;
            this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), arc = 18;

            for (int i = 8; i >= 2; i -= 2) {
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 20 + i*15));
                g2.fillRoundRect(-i/2, -i/2, w + i, h + i, arc + i, arc + i);
            }

            GradientPaint gp = new GradientPaint(0, 0, base.brighter(), 0, h, base.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(new Color(255,255,255,70));
            g2.fillRoundRect(0, 0, w, h/2, arc, arc);

            g2.setColor(glow);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(1, 1, w-3, h-3, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class TitlePanel extends JComponent {
        private final String text;
        private float pulse = 0f;
        private final Timer timer;

        TitlePanel(String text) {
            this.text = text;
            setOpaque(false);
            timer = new Timer(40, e -> {
                pulse += 0.05f;
                if (pulse > Math.PI*2) pulse -= (float)(Math.PI*2);
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int size = Math.max(32, h / 3);
            Font font = new Font("Segoe UI Black", Font.BOLD, size);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int x = (w - fm.stringWidth(text)) / 2;
            int y = (h + fm.getAscent()) / 2 - 8;

            float alpha = 0.6f + 0.4f * (float)((Math.sin(pulse) + 1) / 2.0);
            Color base = new Color(0, 245, 255);

            for (int i = 10; i >= 1; i--) {
                float a = alpha * (1.0f - i/12f) * 0.2f;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), (int)(a*255)));
                g2.setFont(font.deriveFont(size + i * 1.8f));
                g2.drawString(text, x - i, y - i/2);
            }

            GradientPaint gp = new GradientPaint(x, y - fm.getAscent(), new Color(180,255,255), x + fm.stringWidth(text), y, new Color(0,120,255));
            g2.setPaint(gp);
            g2.setFont(font);
            g2.drawString(text, x, y);

            g2.setColor(new Color(220,255,255,200));
            g2.drawString(text, x, y);

            g2.dispose();
        }

        @Override public void addNotify() { super.addNotify(); timer.start(); }
        @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    }

    private static class AvatarListCellRenderer extends JPanel implements ListCellRenderer<String> {
        private String text;
        private boolean selected;

        AvatarListCellRenderer() {
            setOpaque(false);
            setLayout(null);
            setBorder(new EmptyBorder(6, 6, 6, 6));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            this.text = value;
            this.selected = isSelected;
            setPreferredSize(new Dimension(list.getWidth(), 60));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();

            if (selected) {
                g2.setColor(new Color(0, 220, 255, 40));
                g2.fillRoundRect(6, 6, w - 12, h - 12, 14, 14);
            }

            int avSize = Math.min(44, h - 12);
            int ax = 12, ay = (h - avSize) / 2;
            Ellipse2D avatar = new Ellipse2D.Double(ax, ay, avSize, avSize);
            g2.setColor(new Color(40, 0, 80));
            g2.fill(avatar);
            g2.setColor(new Color(255,255,255,40));
            g2.fill(new Ellipse2D.Double(ax + 4, ay + 4, avSize - 8, avSize - 8));

            String name = extractName(text);
            String initials = initialsOf(name);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            int tx = ax + (avSize - fm.stringWidth(initials)) / 2;
            int ty = ay + (avSize + fm.getAscent()) / 2 - 3;
            g2.drawString(initials, tx, ty);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString(name, ax + avSize + 14, ay + 20);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(190, 210, 255));
            g2.drawString(extractStats(text), ax + avSize + 14, ay + 38);

            g2.dispose();
        }

        private String extractName(String raw) { return raw != null && raw.contains(" | ") ? raw.substring(0, raw.indexOf(" | ")) : raw; }
        private String extractStats(String raw) { return raw != null && raw.contains(" | ") ? raw.substring(raw.indexOf(" | ") + 3) : ""; }
        private String initialsOf(String name) {
            if (name == null || name.isBlank()) return "";
            String[] p = name.trim().split("\\s+");
            return p.length > 1 ? (p[0].substring(0,1) + p[p.length-1].substring(0,1)).toUpperCase()
                    : p[0].substring(0,1).toUpperCase();
        }
    }
}