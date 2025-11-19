package client.view;

import client.GameClient;
import shared.model.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class GameView extends JDialog {
    private final GameClient client;
    private final String opponentName;
    private JLabel yourScoreLabel, opponentScoreLabel, countdownLabel, infoLabel;
    private JTextArea inGameChatArea;
    private JTextField chatInput;
    private Timer gameLogicTimer;
    private Timer countdownTimer;

    public GameView(JFrame owner, GameClient client, String opponent, List<Map<String, Integer>> colors,
                    Map<String, Integer> correctColor, double yourScore, double opponentScore) {
        super(owner, "ĐOÁN MÀU NEON - vs " + opponent.toUpperCase(), true);
        this.client = client;
        this.opponentName = opponent;

        setSize(1100, 680);
        setLocationRelativeTo(owner);
        setResizable(false);

        // Background neon + stars
        JPanel bg = new BackgroundPanel();
        bg.setLayout(new BorderLayout());
        setContentPane(bg);

        initNeonUI(colors, correctColor, yourScore, opponentScore);
        setVisible(true);
    }

    private void initNeonUI(List<Map<String, Integer>> colors, Map<String, Integer> correctColor,
                            double yourScore, double opponentScore) {

        // ==================== HEADER ====================
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(25, 40, 15, 40));

        infoLabel = new JLabel("CHỌN MÀU ĐÚNG TRONG 15 GIÂY!", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 28));
        infoLabel.setForeground(new Color(0, 255, 255));

        countdownLabel = new JLabel("15", SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Consolas", Font.BOLD, 72));
        countdownLabel.setForeground(new Color(255, 0, 255));
        countdownLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));

        JPanel scorePanel = new JPanel(new GridLayout(1, 2, 60, 0));
        scorePanel.setOpaque(false);

        yourScoreLabel = new JLabel("YOU: " + String.format("%.0f", yourScore), SwingConstants.CENTER);
        yourScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        yourScoreLabel.setForeground(new Color(0, 255, 150));

        opponentScoreLabel = new JLabel(opponentName.toUpperCase() + ": " + String.format("%.0f", opponentScore), SwingConstants.CENTER);
        opponentScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        opponentScoreLabel.setForeground(new Color(255, 100, 255));

        scorePanel.add(yourScoreLabel);
        scorePanel.add(opponentScoreLabel);

        header.add(infoLabel, BorderLayout.CENTER);
        header.add(countdownLabel, BorderLayout.EAST);
        header.add(scorePanel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

       // ==================== COLOR GRID - DYNAMIC SIZE ====================
JPanel colorWrapper = new JPanel(new GridBagLayout());
colorWrapper.setOpaque(false);
GridBagConstraints gbc = new GridBagConstraints();
gbc.insets = new Insets(20, 20, 20, 20);
gbc.fill = GridBagConstraints.BOTH;
gbc.weightx = 1.0;
gbc.weighty = 1.0;

int numColors = colors.size();
int cols;
if (numColors <= 3) cols = 3;
else if (numColors <= 6) cols = 3;  // Vẫn 3 cột cho 6 màu
else cols = 4;  // 4 cột cho 9 màu

int rows = (int) Math.ceil((double) numColors / cols);
JPanel colorPanel = new JPanel(new GridLayout(rows, cols, 15, 15));
colorPanel.setOpaque(false);

// Tính kích thước ô động theo số màu
int btnSize;
if (numColors <= 4) btnSize = 160;
else if (numColors <= 6) btnSize = 140;
else btnSize = 120;  // Nhỏ hơn tí cho 9 màu nhưng vẫn đẹp

JToggleButton[] buttons = new JToggleButton[numColors];
ButtonGroup group = new ButtonGroup();

for (int i = 0; i < numColors; i++) {
    Map<String, Integer> c = colors.get(i);
    Color color = new Color(c.get("r"), c.get("g"), c.get("b"));
    String rgb = c.get("r") + "," + c.get("g") + "," + c.get("b");

    JToggleButton btn = new JToggleButton();
    btn.setBackground(color);
    btn.setPreferredSize(new Dimension(btnSize, btnSize));
    btn.setMinimumSize(new Dimension(btnSize, btnSize));
    btn.setMaximumSize(new Dimension(btnSize, btnSize));
    btn.setOpaque(true);
    btn.setBorderPainted(false);
    btn.putClientProperty("rgb", rgb);
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // NEON GLOW HOVER
    btn.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (btn.isEnabled()) {
                btn.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 255), 8));
            }
        }
        @Override
        public void mouseExited(MouseEvent e) {
            btn.setBorder(BorderFactory.createEmptyBorder());
        }
    });

    btn.addActionListener(e -> {
        for (JToggleButton b : buttons) b.setSelected(b == btn);
    });

    buttons[i] = btn;
    group.add(btn);
    colorPanel.add(btn);
}

colorWrapper.add(colorPanel, gbc);
add(colorWrapper, BorderLayout.CENTER);

        // ==================== CHAT PANEL ====================
        JPanel chatPanel = new NeonCard(new Color(255, 80, 255), new Color(40, 0, 60));
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(340, 0));
        chatPanel.setBorder(new EmptyBorder(20, 20, 20, 30));

        JLabel chatTitle = new JLabel("MATCH CHAT");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        chatTitle.setForeground(Color.WHITE);
        chatTitle.setBorder(new EmptyBorder(0, 0, 10, 0));
        chatPanel.add(chatTitle, BorderLayout.NORTH);

        inGameChatArea = new JTextArea();
        inGameChatArea.setEditable(false);
        inGameChatArea.setBackground(new Color(10, 0, 20));
        inGameChatArea.setForeground(Color.CYAN);
        inGameChatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        inGameChatArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(inGameChatArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 255), 2));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        chatPanel.add(scroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        chatInput = new JTextField();
        chatInput.setBackground(new Color(12, 0, 25));
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.CYAN);
        chatInput.setBorder(BorderFactory.createLineBorder(new Color(0, 200, 255), 2));

        JButton sendBtn = new NeonButton("SEND", new Color(255, 0, 200), new Color(120, 0, 100));
        sendBtn.setPreferredSize(new Dimension(100, 40));

        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        ActionListener sendAction = e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty()) {
                try {
                    Message m = new Message(Message.Type.IN_GAME_CHAT);
                    m.data = Map.of("message", msg);
                    client.send(m);
                    appendChatMessage("Bạn", msg);
                    chatInput.setText("");
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        sendBtn.addActionListener(sendAction);
        chatInput.addActionListener(sendAction);

        add(chatPanel, BorderLayout.EAST);

        // ==================== BOTTOM BUTTONS ====================
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        bottom.setOpaque(false);

        JButton submitBtn = new NeonButton("GỬI LỰA CHỌN", new Color(0, 255, 150), new Color(0, 120, 60));
        submitBtn.setPreferredSize(new Dimension(220, 55));
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton exitBtn = new NeonButton("THOÁT", new Color(255, 80, 80), new Color(140, 0, 0));
        exitBtn.setPreferredSize(new Dimension(160, 55));

        bottom.add(submitBtn);
        bottom.add(exitBtn);
        add(bottom, BorderLayout.SOUTH);

        submitBtn.addActionListener(e -> {
            for (JToggleButton btn : buttons) {
                if (btn.isSelected()) {
                    String rgb = (String) btn.getClientProperty("rgb");
                    sendMoveAndClose(rgb);
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một màu!", "Chưa chọn", JOptionPane.WARNING_MESSAGE);
        });

        exitBtn.addActionListener(e -> {
            try { client.send(new Message(Message.Type.EXIT)); } catch (Exception ex) { ex.printStackTrace(); }
            closeTimers();
            dispose();
        });

        // ==================== GAME LOGIC ====================
        gameLogicTimer = new Timer();
        countdownTimer = new Timer();
        setupNeonGameLogic(buttons, correctColor, colorPanel);
    }

    private void setupNeonGameLogic(JToggleButton[] buttons, Map<String, Integer> correctColor, JPanel colorPanel) {
        int correctR = correctColor.get("r"), correctG = correctColor.get("g"), correctB = correctColor.get("b");
        String correctRGB = correctR + "," + correctG + "," + correctB;

        // 1. Hiện màu đúng
        gameLogicTimer.schedule(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    for (JToggleButton b : buttons) {
                        String rgb = (String) b.getClientProperty("rgb");
                        b.setBackground(rgb.equals(correctRGB) ? new Color(correctR, correctG, correctB) : Color.GRAY.darker());
                    }
                    infoLabel.setText("NHÌN KỸ MÀU ĐÚNG!");
                    infoLabel.setForeground(new Color(255, 255, 0));
                });
            }
        }, 1000);

        // 2. Đen màn hình
        gameLogicTimer.schedule(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    for (JToggleButton b : buttons) b.setBackground(new Color(8, 0, 20));
                    infoLabel.setText("ĐANG XÁO TRỘN...");
                    infoLabel.setForeground(Color.MAGENTA);
                });
            }
        }, 3000);

        // 3. Xáo + cho chọn
        gameLogicTimer.schedule(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    List<JToggleButton> list = new ArrayList<>(Arrays.asList(buttons));
                    Collections.shuffle(list);
                    colorPanel.removeAll();
                    for (JToggleButton b : list) {
                        String[] p = ((String)b.getClientProperty("rgb")).split(",");
                        b.setBackground(new Color(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                        colorPanel.add(b);
                    }
                    colorPanel.revalidate();
                    colorPanel.repaint();
                    infoLabel.setText("CHỌN NGAY!");
                    infoLabel.setForeground(new Color(0, 255, 255));
                });
            }
        }, 5000);

        // Countdown 15s
        int[] time = {15};
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                SwingUtilities.invokeLater(() -> {
                    time[0]--;
                    countdownLabel.setText(String.valueOf(time[0]));
                    if (time[0] <= 5) {
                        countdownLabel.setForeground(time[0] % 2 == 0 ? Color.RED : new Color(255, 0, 255));
                    }
                    if (time[0] <= 0) {
                        countdownTimer.cancel();
                        JToggleButton selected = null;
                        for (JToggleButton b : buttons) if (b.isSelected()) selected = b;
                        if (selected == null) selected = buttons[new Random().nextInt(buttons.length)];
                        String rgb = (String) selected.getClientProperty("rgb");
                        sendMoveAndClose(rgb);
                    }
                });
            }
        }, 6000, 1000);
    }

    public void updateScores(double yourNewScore, double opponentNewScore) {
        yourScoreLabel.setText("YOU: " + String.format("%.0f", yourNewScore));
        opponentScoreLabel.setText(opponentName.toUpperCase() + ": " + String.format("%.0f", opponentNewScore));
    }

    public void appendChatMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            inGameChatArea.append("[" + sender + "]: " + message + "\n");
            inGameChatArea.setCaretPosition(inGameChatArea.getDocument().getLength());

            // Nhấp nháy khi có tin nhắn mới
            if (!sender.equals("Bạn")) {
                Timer flash = new Timer();
                final int[] count = {0};
                flash.scheduleAtFixedRate(new TimerTask() {
                    @Override public void run() {
                        if (count[0] >= 8) {
                            inGameChatArea.setForeground(Color.CYAN);
                            flash.cancel();
                            return;
                        }
                        inGameChatArea.setForeground(count[0]++ % 2 == 0 ? Color.MAGENTA : Color.CYAN);
                    }
                }, 0, 200);
            }
        });
    }

    private void sendMoveAndClose(String rgb) {
        try {
            Message msg = new Message(Message.Type.MOVE);
            msg.data = Map.of("move", rgb);
            client.send(msg);
            closeTimers();
            dispose();
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void closeTimers() {
        if (gameLogicTimer != null) gameLogicTimer.cancel();
        if (countdownTimer != null) countdownTimer.cancel();
    }

    // ==================== NEON COMPONENTS (giống LobbyView) ====================
    private static class BackgroundPanel extends JPanel {
        private final BufferedImage stars = createStarField(1200, 800, 200);
        BackgroundPanel() { setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            GradientPaint gp = new GradientPaint(0, 0, new Color(8, 0, 20), 0, getHeight(), new Color(25, 0, 50));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (stars != null) g2.drawImage(stars, 0, 0, null);
            g2.dispose();
        }
        private static BufferedImage createStarField(int w, int h, int count) {
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            Random r = new Random();
            for (int i = 0; i < count; i++) {
                int x = r.nextInt(w), y = r.nextInt(h);
                int a = r.nextInt(150) + 100;
                g.setColor(new Color(255, 255, 255, a));
                g.fillOval(x, y, 2, 2);
            }
            g.dispose();
            return img;
        }
    }

    private static class NeonCard extends JPanel {
        private final Color glow, inner;
        NeonCard(Color glow, Color inner) {
            this.glow = glow; this.inner = inner;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 20;
            for (int i = 16; i >= 4; i -= 4) {
                float a = 0.1f * (i / 16f);
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), (int)(a*255)));
                g2.fillRoundRect(-i/2, -i/2, w+i, h+i, arc+i, arc+i);
            }
            GradientPaint gp = new GradientPaint(0, 0, inner.brighter(), 0, h, inner.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(glow);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(2, 2, w-5, h-5, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class NeonButton extends JButton {
        private final Color glow, base;
        NeonButton(String text, Color glow, Color base) {
            super(text);
            this.glow = glow; this.base = base;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 20;
            for (int i = 10; i >= 2; i -= 2) {
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 30 + i*15));
                g2.fillRoundRect(-i/2, -i/2, w+i, h+i, arc+i, arc+i);
            }
            GradientPaint gp = new GradientPaint(0, 0, base.brighter(), 0, h, base.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.setColor(new Color(255,255,255,80));
            g2.fillRoundRect(0, 0, w, h/2, arc, arc);
            g2.setColor(glow);
            g2.setStroke(new BasicStroke(3));
            g2.drawRoundRect(2, 2, w-5, h-5, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}