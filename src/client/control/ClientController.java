package client.control;

import shared.model.Message;
import client.GameClient;
import client.view.GameView;
import client.view.LobbyView;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Lớp Controller (bộ não) của client.
 * Chịu trách nhiệm nhận tin nhắn từ server và điều phối các View.
 */
public class ClientController {
    private LobbyView lobbyView;
    private GameView gameView; // Chỉ có một game view tại một thời điểm
    private final GameClient client;
    private volatile boolean rematchIsPending = false;

    public ClientController(GameClient client) {
        this.client = client;
    }

    public void setLobbyView(LobbyView lobbyView) {
        this.lobbyView = lobbyView;
    }

    /**
     * "Tổng đài" xử lý tất cả tin nhắn từ server.
     * Nó quyết định hành động nào cần thực hiện trên giao diện.
     * 
     * @param m Tin nhắn nhận được từ GameClient.
     */
    public void handle(Message m) {
        // Đảm bảo mọi thay đổi trên UI đều được thực hiện trên Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            switch (m.type) {
                case LOGIN_OK:
                    JOptionPane.showMessageDialog(lobbyView, "Login OK");
                    break;

                case LOGIN_FAIL:
                    if (lobbyView != null) {
                        lobbyView.showLoginError(m);
                    }
                    break;

                case USER_LIST:
                    if (lobbyView != null) {
                        lobbyView.updateUserList(m);
                    }
                    break;

                case CHAT_MESSAGE:
                    if (lobbyView != null) {
                        lobbyView.appendChatMessage(m);
                    }
                    break;

                case CHALLENGE_REQ:
                    if (lobbyView != null) {
                        lobbyView.showChallengeRequest(m);
                    }
                    break;

                case START_GAME:
                    // Đóng game cũ (nếu có) trước khi bắt đầu game mới
                    if (gameView != null) {
                        gameView.dispose();
                    }

                    String opp = (String) m.data.get("opponent");
                    List<Map<String, Integer>> colors = (List<Map<String, Integer>>) m.data.get("colors");
                    Map<String, Integer> correctColor = (Map<String, Integer>) m.data.get("correctColor");
                    double yourScore = ((Number) m.data.get("yourScore")).doubleValue();
                    double opponentScore = ((Number) m.data.get("opponentScore")).doubleValue();

                    gameView = new GameView(lobbyView, client, opp, colors, correctColor, yourScore, opponentScore);
                    gameView.setVisible(true); // Hiển thị cửa sổ game
                    break;

                case ROUND_RESULT:
                    if (gameView != null) {
                        double yourNewScore = ((Number) m.data.get("yourTotalScore")).doubleValue();
                        double opponentNewScore = ((Number) m.data.get("opponentTotalScore")).doubleValue();
                        gameView.updateScores(yourNewScore, opponentNewScore);
                    }

                    // BƯỚC 1: Đọc thêm điểm của vòng này
                    double sc = ((Number) m.data.get("score")).doubleValue();

                    // BƯỚC 2: Tạo chuỗi hiển thị đầy đủ thông tin như cũ
                    String roundResultText = String.format(
                            "Score this round: %.0f\n\nYour move: %s\nOpponent's move: %s\nCorrect color was: %s",
                            sc,
                            m.data.get("yourMove"),
                            m.data.get("oppMove"),
                            m.data.get("correctColor"));

                    JOptionPane.showMessageDialog(gameView, roundResultText, "Round Result",
                            JOptionPane.INFORMATION_MESSAGE);

                    try {
                        client.send(new Message(Message.Type.NEXT_ROUND));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case IN_GAME_CHAT:
                    if (gameView != null) {
                        String sender = (String) m.data.get("sender");
                        String message = (String) m.data.get("message");
                        gameView.appendChatMessage(sender, message);
                    }
                    break;

                case MATCH_END:
                    if (gameView != null) {
                        gameView.dispose();
                        gameView = null;
                    }
                    JOptionPane.showMessageDialog(lobbyView, "Match ended: " + m.data.get("reason"));

                    if (rematchIsPending) {
                        rematchIsPending = false; // Reset cờ ngay
                        int choice = JOptionPane.showConfirmDialog(lobbyView,
                                "Opponent wants to play again. Accept?",
                                "Rematch", JOptionPane.YES_NO_OPTION);
                        try {
                            Message response = new Message(Message.Type.REMATCH_RESP);
                            response.data = Map.of("accept", choice == JOptionPane.YES_OPTION);
                            client.send(response);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    break;

                case REMATCH_REQ:
                    rematchIsPending = true;
                    break;

                case LEADERBOARD_DATA:
                    if (lobbyView != null) {
                        lobbyView.showLeaderboard(m);
                    }
                    break;

                default:
                    System.out.println("Unhandled message type received by Controller: " + m.type);
                    break;
            }
        });
    }
}