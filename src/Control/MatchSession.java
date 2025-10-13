package Control;

import java.util.*;
import model.Message;
import java.awt.Color;

public class MatchSession {
    private final PlayerState player1;
    private final PlayerState player2;
    private Timer roundTimer;
    private int currentRound = 0;
    private static final int MAX_ROUNDS = 5;
    private static final Random random = new Random();
    private Color correctColorObject;

    private Map<String, Object> generateSimilarColorPalette(int roundNumber, int numColors) {
        // 1. Xác định độ khó (delta) dựa trên vòng đấu
        int delta;
        if (roundNumber <= 1) {
            delta = 200; // Vòng 1
        } else if (roundNumber <= 3) {
            delta = 100; // Vòng 2-3
        } else {
            delta = 50; // Vòng 4-5
        }

        // 2. Tạo màu "đúng" ngẫu nhiên
        int r_base = random.nextInt(256);
        int g_base = random.nextInt(256);
        int b_base = random.nextInt(256);
        Color correctColor = new Color(r_base, g_base, b_base);

        List<Color> palette = new ArrayList<>();
        palette.add(correctColor);

        // 3. Tạo các màu "mồi" dựa trên màu đúng
        for (int i = 0; i < numColors - 1; i++) {
            // Lấy một offset ngẫu nhiên trong khoảng [-delta, +delta] cho mỗi kênh màu
            int r_offset = random.nextInt(delta * 2) - delta;
            int g_offset = random.nextInt(delta * 2) - delta;
            int b_offset = random.nextInt(delta * 2) - delta;

            // Tính giá trị RGB mới và đảm bảo nó nằm trong khoảng [0, 255]
            int new_r = Math.max(0, Math.min(255, r_base + r_offset));
            int new_g = Math.max(0, Math.min(255, g_base + g_offset));
            int new_b = Math.max(0, Math.min(255, b_base + b_offset));

            palette.add(new Color(new_r, new_g, new_b));
        }

        Collections.shuffle(palette);

        Map<String, Object> result = new HashMap<>();
        result.put("correctColor", correctColor);
        result.put("palette", palette);
        return result;
    }

    public MatchSession(PlayerState p1, PlayerState p2) {
        this.player1 = p1;
        this.player2 = p2;
        player1.setMatchSession(this);
        player2.setMatchSession(this);
    }

    public PlayerState getPlayer1() {
        return player1;
    }

    public PlayerState getPlayer2() {
        return player2;
    }

    public void start() {
        player1.getClient().setInGame(true);
        player2.getClient().setInGame(true);
        player1.setOpponent(player2);
        player2.setOpponent(player1);
        player1.resetMatchScore();
        player2.resetMatchScore();
        currentRound = 0;
        startNextRound();
    }

    private void startNextRound() {
        if (currentRound >= MAX_ROUNDS) {
            endMatch("all_rounds_completed");
            return;
        }
        currentRound++;

        // ✅ GỌI HÀM TẠO BẢNG MÀU MỚI
        int numColors = 6 + (currentRound - 1); // Vòng 1 có 6 màu, vòng 2 có 7, v.v.
        Map<String, Object> colorData = generateSimilarColorPalette(currentRound, numColors);

        this.correctColorObject = (Color) colorData.get("correctColor");
        List<Color> paletteForThisRound = (List<Color>) colorData.get("palette");

        sendColorList(player1, paletteForThisRound);
        sendColorList(player2, paletteForThisRound);

        scheduleRoundTimeout();
    }

    // ✅ Đã sửa lại để dùng cách gọi trực tiếp
    private void sendColorList(PlayerState player, List<Color> palette) {
        Message msg = new Message(Message.Type.START_GAME);
        Map<String, Object> data = new HashMap<>();

        // 1. Chuyển danh sách đối tượng Color thành danh sách các Map RGB
        List<Map<String, Integer>> colorsAsRgb = new ArrayList<>();
        for (Color c : palette) {
            colorsAsRgb.add(Map.of("r", c.getRed(), "g", c.getGreen(), "b", c.getBlue()));
        }
        data.put("colors", colorsAsRgb);

        // 2. Gửi màu đúng cũng dưới dạng Map RGB
        data.put("correctColor", Map.of(
                "r", correctColorObject.getRed(),
                "g", correctColorObject.getGreen(),
                "b", correctColorObject.getBlue()));

        data.put("opponent", player.getOpponent().getClient().getUser().getUsername());
        data.put("yourScore", player.getMatchScore());
        data.put("opponentScore", player.getOpponent().getMatchScore());

        data.put("round", currentRound);
        data.put("maxRounds", MAX_ROUNDS);
        msg.data = data;

        player.getClient().send(msg);
    }

    public synchronized void submitMove(PlayerState player, String colorGuess) {
        player.setMove(colorGuess);
        if (player1.getMove() != null && player2.getMove() != null) {
            evaluateRound();
        }
    }

    public synchronized void submitRematchResponse(PlayerState player, boolean accepted) {
        if (!accepted) {
            cleanupAndEndSession();
            return;
        }
        player.setRematchAccepted(true);
        if (player1.isRematchAccepted() && player2.isRematchAccepted()) {
            player1.setRematchAccepted(false);
            player2.setRematchAccepted(false);

            // Tạm thời comment lại dòng log này vì nó phụ thuộc sâu vào cấu trúc
            // player1.getClient().getServer().getLobby().getView().showMessage(...);

            start(); // Bắt đầu lại trận mới
        }
    }

    private void scheduleRoundTimeout() {
        roundTimer = new Timer();
        roundTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                evaluateRound();
            }
        }, 15000);
    }

    private void evaluateRound() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }

        String correctColorString = String.format("%d,%d,%d",
                correctColorObject.getRed(), correctColorObject.getGreen(), correctColorObject.getBlue());

        double score1 = correctColorString.equals(player1.getMove()) ? 1 : 0;
        double score2 = correctColorString.equals(player2.getMove()) ? 1 : 0;

        player1.addToMatchScore(score1);
        player2.addToMatchScore(score2);

        sendRoundResult(player1, score1, correctColorString);
        sendRoundResult(player2, score2, correctColorString);

        Integer winnerId = null;
        if (score1 > score2)
            winnerId = player1.getClient().getUser().getId();
        else if (score2 > score1)
            winnerId = player2.getClient().getUser().getId();

        // Chú ý: Các dòng dưới đây là một điểm yếu kiến trúc (phụ thuộc sâu).
        // Tạm giữ lại theo yêu cầu của bạn.
        // Cần đảm bảo các phương thức get này tồn tại và có thể truy cập được.
        player1.getClient().getServer().getLobby().getUserDAO().saveMatchResult(
                player1.getClient().getUser().getId(),
                player2.getClient().getUser().getId(),
                score1, score2, winnerId);

        player1.getClient().getServer().getLobby().getBroadcastService().broadcastUserList();

        player1.setMove(null);
        player2.setMove(null);
    }

    private void sendRoundResult(PlayerState player, double roundScore, String correctColorString) {
        Message msg = new Message(Message.Type.ROUND_RESULT);
        Map<String, Object> data = new HashMap<>();
        data.put("yourMove", player.getMove());
        data.put("oppMove", player.getOpponent().getMove());

        data.put("correctColor", correctColorString); // Gửi chuỗi "r,g,b"

        data.put("score", roundScore);

        // ✅ GỬI TỔNG ĐIỂM MỚI NHẤT
        data.put("yourTotalScore", player.getMatchScore());
        data.put("opponentTotalScore", player.getOpponent().getMatchScore());
        msg.data = data;

        player.getClient().send(msg);
    }

    public void handleExit(PlayerState leaver) {
        PlayerState opponent = leaver.getOpponent();
        if (opponent != null) {
            Message endMsg = new Message(Message.Type.MATCH_END);
            endMsg.data = Map.of("reason", "opponent_exited");
            opponent.getClient().send(endMsg);
        }
        cleanupAndEndSession();
    }

    public void handleNextRound(PlayerState player) {
        player.setReadyForNextRound(true);
        if (player1.isReadyForNextRound() && player2.isReadyForNextRound()) {
            player1.setReadyForNextRound(false);
            player2.setReadyForNextRound(false);
            startNextRound();
        }
    }

    private void endMatch(String reason) {
        Message endMsg = new Message(Message.Type.MATCH_END);
        endMsg.data = Map.of("reason", reason);
        player1.getClient().send(endMsg);
        player2.getClient().send(endMsg);

        Message rematchReq = new Message(Message.Type.REMATCH_REQ);
        player1.getClient().send(rematchReq);
        player2.getClient().send(rematchReq);
    }

    private void cleanupAndEndSession() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }

        player1.getClient().setMatch(null);
        player2.getClient().setMatch(null);
        player1.reset();
        player2.reset();
        player1.getClient().setInGame(false);
        player2.getClient().setInGame(false);

        // Chú ý: Dòng dưới đây là một điểm yếu kiến trúc.
        // Tạm giữ lại theo yêu cầu của bạn.
        player1.getClient().getServer().getLobby().getBroadcastService().broadcastUserList();
    }
}