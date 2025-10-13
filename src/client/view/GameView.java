package client.view;

import client.GameClient;
import shared.model.Message;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class GameView extends JDialog {
    private final GameClient client;
    private final String opponentName; // Lưu lại tên đối thủ để cập nhật điểm
    private JLabel yourScoreLabel, opponentScoreLabel;
    private JTextArea inGameChatArea;
    private Timer gameLogicTimer;
    private Timer countdownTimer;

    /**
     * Constructor tạo ra toàn bộ cửa sổ game.
     * 
     * @param owner         Cửa sổ cha (thường là LobbyView).
     * @param client        Đối tượng GameClient để gửi tin nhắn.
     * @param opponent      Tên đối thủ.
     * @param colors        Danh sách các màu RGB cho vòng đấu.
     * @param correctColor  Màu đúng dưới dạng RGB.
     * @param yourScore     Điểm số ban đầu của bạn.
     * @param opponentScore Điểm số ban đầu của đối thủ.
     */
    public GameView(JFrame owner, GameClient client, String opponent, List<Map<String, Integer>> colors,
            Map<String, Integer> correctColor, double yourScore, double opponentScore) {
        super(owner, "Đoán màu vs " + opponent, true);
        this.client = client;
        this.opponentName = opponent;

        setLayout(new BorderLayout());

        // --- BƯỚC 1: TẠO VÀ SẮP XẾP TẤT CẢ CÁC COMPONENT GIAO DIỆN ---
        JPanel topPanel = createTopPanel(yourScore, opponentScore);
        add(topPanel, BorderLayout.NORTH);

        JPanel colorPanel = createColorPanel(colors);
        add(colorPanel, BorderLayout.CENTER);

        JPanel chatPanel = createChatPanel();
        add(chatPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        JButton sendBtn = new JButton("Gửi");
        JButton exitBtn = new JButton("Thoát");
        buttonPanel.add(sendBtn);
        buttonPanel.add(exitBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // --- BƯỚC 2: TẠO CÁC NÚT VÀ THIẾT LẬP LOGIC GAME ---
        JToggleButton[] buttons = createColorButtons(colors);

        // --- BƯỚC 3: KHỞI ĐỘNG CÁC TIMER VÀ ACTIONLISTENER ---
        gameLogicTimer = new Timer();
        countdownTimer = new Timer();

        setupGameLogic(buttons, correctColor, colorPanel, (JLabel) topPanel.getComponent(0),
                (JLabel) topPanel.getComponent(1));

        sendBtn.addActionListener(e -> {
            for (JToggleButton btn : buttons) {
                if (btn.isSelected()) {
                    String selectedColorValue = (String) btn.getClientProperty("colorValue");
                    sendMoveAndClose(selectedColorValue);
                    break;
                }
            }
        });

        exitBtn.addActionListener(e -> {
            try {
                client.send(new Message(Message.Type.EXIT));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            closeTimers();
            dispose();
        });

        // --- BƯỚC 4: HIỂN THỊ CỬA SỔ ---
        setSize(650, 450);
        setLocationRelativeTo(owner);
        // setVisible(true) sẽ được gọi từ ClientController
    }

    // --- CÁC PHƯƠNG THỨC ĐỂ CONTROLLER CÓ THỂ CẬP NHẬT TỪ BÊN NGOÀI ---

    public void updateScores(double yourNewScore, double opponentNewScore) {
        yourScoreLabel.setText(String.format("You: %.0f", yourNewScore));
        opponentScoreLabel.setText(String.format("%s: %.0f", opponentName, opponentNewScore));
    }

    public void appendChatMessage(String sender, String message) {
        inGameChatArea.append(String.format("[%s]: %s\n", sender, message));
        inGameChatArea.setCaretPosition(inGameChatArea.getDocument().getLength());
    }

    // --- CÁC PHƯƠNG THỨC HELPER ĐỂ CODE GỌN HƠN ---

    private JPanel createTopPanel(double yourScore, double opponentScore) {
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel infoLbl = new JLabel("Chọn màu đúng trong 15 giây!", SwingConstants.CENTER);
        infoLbl.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(infoLbl, BorderLayout.CENTER);

        JLabel countdownLbl = new JLabel("15", SwingConstants.CENTER);
        countdownLbl.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(countdownLbl, BorderLayout.EAST);

        JPanel scorePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        yourScoreLabel = new JLabel(String.format("You: %.0f", yourScore), SwingConstants.LEFT);
        opponentScoreLabel = new JLabel(String.format("%s: %.0f", opponentName, opponentScore), SwingConstants.RIGHT);
        yourScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        opponentScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scorePanel.add(yourScoreLabel);
        scorePanel.add(opponentScoreLabel);
        topPanel.add(scorePanel, BorderLayout.SOUTH);

        return topPanel;
    }

    private JPanel createColorPanel(List<Map<String, Integer>> colors) {
        int numColors = colors.size();
        int columns = 3;
        int rows = (int) Math.ceil((double) numColors / columns);
        return new JPanel(new GridLayout(rows, columns, 10, 10));
    }

    private JPanel createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setPreferredSize(new Dimension(200, 0));
        chatPanel.setBorder(BorderFactory.createTitledBorder("Match Chat"));

        inGameChatArea = new JTextArea();
        inGameChatArea.setEditable(false);
        inGameChatArea.setLineWrap(true);
        chatPanel.add(new JScrollPane(inGameChatArea), BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(5, 5));
        JTextField inGameMessageField = new JTextField();
        JButton sendInGameChatButton = new JButton("Send");
        messageInputPanel.add(inGameMessageField, BorderLayout.CENTER);
        messageInputPanel.add(sendInGameChatButton, BorderLayout.EAST);
        chatPanel.add(messageInputPanel, BorderLayout.SOUTH);

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
        return chatPanel;
    }

    private JToggleButton[] createColorButtons(List<Map<String, Integer>> colors) {
        JToggleButton[] buttons = new JToggleButton[colors.size()];
        for (int i = 0; i < colors.size(); i++) {
            Map<String, Integer> colorMap = colors.get(i);
            int r = colorMap.get("r");
            int g = colorMap.get("g");
            int b = colorMap.get("b");
            JToggleButton btn = new JToggleButton("");
            btn.setBackground(new Color(r, g, b));
            String colorValue = String.format("%d,%d,%d", r, g, b);
            btn.putClientProperty("colorValue", colorValue);
            buttons[i] = btn;
        }
        return buttons;
    }

    private void setupGameLogic(JToggleButton[] buttons, Map<String, Integer> correctColor, JPanel colorPanel,
            JLabel infoLbl, JLabel countdownLbl) {
        // Step 1: Hiển thị màu đúng
        gameLogicTimer.schedule(new TimerTask() {
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

        // Step 2: Đen
        gameLogicTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    for (JToggleButton btn : buttons) {
                        btn.setBackground(Color.BLACK);
                    }
                    infoLbl.setText("Đang xáo vị trí...");
                });
            }
        }, 3000);

        // Step 3: Xáo trộn và cho chọn
        gameLogicTimer.schedule(new TimerTask() {
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
                            for (JToggleButton otherBtn : buttons) {
                                otherBtn.setSelected(otherBtn == btn);
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

        // Countdown
        int[] secondsLeft = { 15 };
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
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        }, 6000, 1000);
    }

    private void sendMoveAndClose(String colorValue) {
        try {
            Message msg = new Message(Message.Type.MOVE);
            msg.data = Map.of("move", colorValue);
            client.send(msg);
            closeTimers();
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void closeTimers() {
        if (gameLogicTimer != null)
            gameLogicTimer.cancel();
        if (countdownTimer != null)
            countdownTimer.cancel();
    }
}