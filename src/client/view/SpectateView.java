package client.view;

import javax.swing.*;

import client.GameClient;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import shared.model.Message;

/**
 * Giao diện XEM TRẬN ĐẤU (chỉ đọc) dành cho Admin.
 */
public class SpectateView extends JDialog {

    private JLabel p1ScoreLabel, p2ScoreLabel;
    private JPanel colorPanel;
    private JLabel infoLabel;
    private String p1Name = "Player 1";
    private String p2Name = "Player 2";

    public SpectateView(JFrame owner, GameClient client, String p1, String p2, Runnable onWindowCloseCallback) {
        super(owner, "Spectating: " + p1 + " vs " + p2, false); // 'false' = non-modal
        this.p1Name = p1;
        this.p2Name = p2;

        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);

        // Panel Điểm số
        JPanel scorePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        p1ScoreLabel = new JLabel(p1Name + ": 0", SwingConstants.LEFT);
        p2ScoreLabel = new JLabel(p2Name + ": 0", SwingConstants.RIGHT);
        p1ScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        p2ScoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scorePanel.add(p1ScoreLabel);
        scorePanel.add(p2ScoreLabel);
        add(scorePanel, BorderLayout.NORTH);

        // Panel Thông tin (Vòng, Màu đúng)
        infoLabel = new JLabel("Đang tải...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(infoLabel, BorderLayout.SOUTH);

        // Panel Màu sắc (nơi hiển thị các ô màu)
        colorPanel = new JPanel(new GridLayout(3, 3, 10, 10)); // 3x3 layout
        colorPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(colorPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onWindowCloseCallback.run(); // Chạy code do Controller truyền vào
            }
        });
        setLocationRelativeTo(owner);
    }

    /**
     * Cập nhật toàn bộ giao diện với dữ liệu mới từ server.
     */
    public void updateState(Message m) {
        Map<String, Object> data = m.data;

        // Cập nhật điểm
        double p1Score = ((Number) data.getOrDefault("yourScore", 0.0)).doubleValue();
        double p2Score = ((Number) data.getOrDefault("opponentScore", 0.0)).doubleValue();
        p1ScoreLabel.setText(String.format("%s: %.1f", p1Name, p1Score));
        p2ScoreLabel.setText(String.format("%s: %.1f", p2Name, p2Score));

        // Cập nhật thông tin vòng
        int round = ((Number) data.getOrDefault("round", 0)).intValue();
        infoLabel.setText("Vòng " + round);

        colorPanel.removeAll(); // Xóa các màu cũ

        // Hiển thị màu ĐÚNG (nếu có)
        if (data.containsKey("correctColor")) {
            Map<String, Integer> colorMap = (Map<String, Integer>) data.get("correctColor");
            int r = colorMap.get("r");
            int g = colorMap.get("g");
            int b = colorMap.get("b");

            JPanel correctColorPanel = new JPanel();
            correctColorPanel.setBackground(new Color(r, g, b));
            correctColorPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            correctColorPanel.add(new JLabel("MÀU ĐÚNG"));
            colorPanel.add(correctColorPanel);
        }

        // Hiển thị các màu trong bảng (nếu có)
        if (data.containsKey("colors")) {
            List<Map<String, Integer>> colors = (List<Map<String, Integer>>) data.get("colors");
            for (Map<String, Integer> cMap : colors) {
                JPanel p = new JPanel();
                p.setBackground(new Color(cMap.get("r"), cMap.get("g"), cMap.get("b")));
                colorPanel.add(p);
            }
        }

        // Cập nhật giao diện
        colorPanel.revalidate();
        colorPanel.repaint();
    }
}