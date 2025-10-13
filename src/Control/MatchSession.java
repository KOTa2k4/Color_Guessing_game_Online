package Control;

import java.util.*;
import model.Message;
import java.awt.Color;

public class MatchSession {
    private final PlayerState player1;
    private final PlayerState player2;
    private final MatchListener listener;

    private Timer roundTimer;
    private int currentRound = 0;
    private static final int MAX_ROUNDS = 5;
    private static final Random random = new Random();
    private Color correctColorObject;

    public MatchSession(PlayerState p1, PlayerState p2, MatchListener listener) {
        this.player1 = p1;
        this.player2 = p2;
        this.listener = listener;
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
            start();
        }
    }

    public void handleNextRound(PlayerState player) {
        player.setReadyForNextRound(true);
        if (player1.isReadyForNextRound() && player2.isReadyForNextRound()) {
            player1.setReadyForNextRound(false);
            player2.setReadyForNextRound(false);
            startNextRound();
        }
    }

    public void handleExit(PlayerState leaver) {
        PlayerState opponent = leaver.getOpponent();
        if (opponent != null) {
            Message endMsg = new Message(Message.Type.MATCH_END);
            endMsg.data = Map.of("reason", "opponent_exited");
            listener.onSendMessage(opponent, endMsg);
        }
        cleanupAndEndSession();
    }

    private void startNextRound() {
        if (currentRound >= MAX_ROUNDS) {
            endMatch("all_rounds_completed");
            return;
        }
        currentRound++;

        int numColors = 6 + (currentRound - 1);
        Map<String, Object> colorData = generateSimilarColorPalette(currentRound, numColors);
        this.correctColorObject = (Color) colorData.get("correctColor");
        List<Color> paletteForThisRound = (List<Color>) colorData.get("palette");

        sendColorList(player1, paletteForThisRound);
        sendColorList(player2, paletteForThisRound);
        scheduleRoundTimeout();
    }

    private Map<String, Object> generateSimilarColorPalette(int roundNumber, int numColors) {
        // Thiết lập độ khó
        int delta;
        if (roundNumber <= 1)
            delta = 70;
        else if (roundNumber <= 3)
            delta = 40;
        else
            delta = 20;

        int r_base = random.nextInt(256);
        int g_base = random.nextInt(256);
        int b_base = random.nextInt(256);
        Color correctColor = new Color(r_base, g_base, b_base);
        List<Color> palette = new ArrayList<>();
        palette.add(correctColor);

        for (int i = 0; i < numColors - 1; i++) {
            int r_offset = random.nextInt(delta * 2 + 1) - delta;
            int g_offset = random.nextInt(delta * 2 + 1) - delta;
            int b_offset = random.nextInt(delta * 2 + 1) - delta;
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

    private void sendColorList(PlayerState player, List<Color> palette) {
        Message msg = new Message(Message.Type.START_GAME);
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Integer>> colorsAsRgb = new ArrayList<>();
        for (Color c : palette) {
            colorsAsRgb.add(Map.of("r", c.getRed(), "g", c.getGreen(), "b", c.getBlue()));
        }
        data.put("colors", colorsAsRgb);
        data.put("correctColor", Map.of("r", correctColorObject.getRed(), "g", correctColorObject.getGreen(), "b",
                correctColorObject.getBlue()));
        data.put("opponent", player.getOpponent().getClient().getUser().getUsername());
        data.put("yourScore", player.getMatchScore());
        data.put("opponentScore", player.getOpponent().getMatchScore());
        data.put("round", currentRound);
        data.put("maxRounds", MAX_ROUNDS);
        msg.data = data;

        listener.onSendMessage(player, msg);
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

        Integer winnerId = null;
        if (score1 > score2)
            winnerId = player1.getClient().getUser().getId();
        else if (score2 > score1)
            winnerId = player2.getClient().getUser().getId();

        listener.onMatchDataSave(player1, player2, score1, score2, winnerId);

        sendRoundResult(player1, score1, correctColorString);
        sendRoundResult(player2, score2, correctColorString);

        player1.setMove(null);
        player2.setMove(null);
    }

    private void sendRoundResult(PlayerState player, double roundScore, String correctColorString) {
        Message msg = new Message(Message.Type.ROUND_RESULT);
        Map<String, Object> data = new HashMap<>();
        data.put("yourMove", player.getMove());
        data.put("oppMove", player.getOpponent().getMove());
        data.put("correctColor", correctColorString);
        data.put("score", roundScore);
        data.put("yourTotalScore", player.getMatchScore());
        data.put("opponentTotalScore", player.getOpponent().getMatchScore());
        msg.data = data;

        listener.onSendMessage(player, msg);
    }

    /**
     * Xử lý tin nhắn chat trong trận và chỉ gửi cho đối thủ.
     * 
     * @param senderState     PlayerState của người gửi.
     * @param originalMessage Tin nhắn gốc từ client.
     */
    public void handleChatMessage(PlayerState senderState, Message originalMessage) {
        PlayerState recipientState = senderState.getOpponent();
        if (recipientState == null)
            return;

        String messageContent = (String) originalMessage.data.get("message");
        if (messageContent == null || messageContent.isBlank())
            return;

        // Tạo một tin nhắn mới để gửi đi, thêm tên người gửi vào
        Message forwardMessage = new Message(Message.Type.IN_GAME_CHAT);
        forwardMessage.data = Map.of(
                "sender", senderState.getClient().getUser().getUsername(),
                "message", messageContent);

        // ✅ Chỉ gửi cho đối thủ, không phải cho mọi người
        listener.onSendMessage(recipientState, forwardMessage);
    }

    private void endMatch(String reason) {
        Message endMsg = new Message(Message.Type.MATCH_END);
        endMsg.data = Map.of("reason", reason);
        listener.onSendMessage(player1, endMsg);
        listener.onSendMessage(player2, endMsg);

        Message rematchReq = new Message(Message.Type.REMATCH_REQ);
        listener.onSendMessage(player1, rematchReq);
        listener.onSendMessage(player2, rematchReq);
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

        listener.onPlayerStatusUpdate();
    }
}