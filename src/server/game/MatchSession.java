package server.game;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import server.network.ClientHandler;
import shared.model.Message;
import java.awt.Color;

public class MatchSession {
    private final PlayerState player1;
    private final PlayerState player2;
    private final MatchListener listener;
    private final String matchId;
    private final List<ClientHandler> spectators = new CopyOnWriteArrayList<>();

    private Timer roundTimer;
    private int currentRound = 0;
    public static final int MAX_ROUNDS = 5;
    private Color correctColorObject;

    public MatchSession(PlayerState p1, PlayerState p2, MatchListener listener) {
        this.player1 = p1;
        this.player2 = p2;
        this.listener = listener;
        this.matchId = java.util.UUID.randomUUID().toString();
        player1.setMatchSession(this);
        player2.setMatchSession(this);
    }

    // --- CÁC HÀM GETTER CÔNG KHAI (CHO FACTORY) ---
    public String getMatchId() {
        return this.matchId;
    }

    public PlayerState getPlayer1() {
        return player1;
    }

    public PlayerState getPlayer2() {
        return player2;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    // --- LUỒNG GAME CHÍNH (ĐÃ TỐI ƯU) ---

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
            processFinalMatchResult();
            endMatch("all_rounds_completed");
            return;
        }
        currentRound++;

        int numColors = 6 + (currentRound - 1);

        // 1. Gọi Utils để lấy màu
        Map<String, Object> colorData = GameUtils.generateSimilarColorPalette(currentRound, numColors);
        this.correctColorObject = (Color) colorData.get("correctColor");
        List<Color> paletteForThisRound = (List<Color>) colorData.get("palette");

        // 2. Gọi Factory để đóng gói tin nhắn
        Map<String, Object> data = GamePacketFactory.createStartRoundPacket(this, paletteForThisRound,
                correctColorObject);

        // 3. Gửi cho người chơi
        sendColorList(player1, data);
        sendColorList(player2, data);

        // 4. Gửi cho khán giả
        Message spectateMsg = new Message(Message.Type.SPECTATE_UPDATE);
        spectateMsg.data = data; // Dùng 'data' chung (đã chứa điểm P1, P2)
        broadcastToSpectators(spectateMsg);

        scheduleRoundTimeout();
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

        // 1. Gọi Factory để đóng gói tin nhắn
        Map<String, Object> spectateData = GamePacketFactory.createRoundResultPacket(this, correctColorString, score1,
                score2);

        // 2. Gửi cho người chơi
        sendRoundResult(player1, score1, spectateData);
        sendRoundResult(player2, score2, spectateData);

        // 3. Gửi cho khán giả
        Message spectateMsg = new Message(Message.Type.SPECTATE_UPDATE);
        spectateMsg.data = spectateData; // Dùng 'spectateData' (đã chứa move P1, P2)
        broadcastToSpectators(spectateMsg);

        player1.setMove(null);
        player2.setMove(null);
    }

    private void processFinalMatchResult() {
        double tempScore1 = player1.getMatchScore();
        double tempScore2 = player2.getMatchScore();
        double finalPoints1, finalPoints2;
        Integer winnerId = null;

        if (tempScore1 > tempScore2) {
            finalPoints1 = 1.0;
            finalPoints2 = 0.0;
            winnerId = player1.getClient().getUser().getId();
        } else if (tempScore2 > tempScore1) {
            finalPoints1 = 0.0;
            finalPoints2 = 1.0;
            winnerId = player2.getClient().getUser().getId();
        } else {
            finalPoints1 = 0.5;
            finalPoints2 = 0.5;
        }
        listener.onMatchDataSave(player1, player2, finalPoints1, finalPoints2, winnerId);
    }

    // --- HÀM GỬI TIN (ĐÃ TỐI ƯU) ---

    /**
     * Gửi packet BẮT ĐẦU VÒNG (đã được tùy chỉnh) cho 1 người chơi.
     */
    private void sendColorList(PlayerState player, Map<String, Object> data) {
        Message msg = new Message(Message.Type.START_GAME);

        // Tạo bản sao và tùy chỉnh cho người chơi này
        Map<String, Object> dataForPlayer = new HashMap<>(data);
        dataForPlayer.put("opponent", player.getOpponent().getClient().getUser().getUsername());
        dataForPlayer.put("yourScore", player.getMatchScore());
        dataForPlayer.put("opponentScore", player.getOpponent().getMatchScore());

        msg.data = dataForPlayer;
        listener.onSendMessage(player, msg);
    }

    /**
     * Gửi packet KẾT THÚC VÒNG (đã được tùy chỉnh) cho 1 người chơi.
     */
    private void sendRoundResult(PlayerState player, double roundScore, Map<String, Object> data) {
        Message msg = new Message(Message.Type.ROUND_RESULT);

        // Tạo bản sao và tùy chỉnh cho người chơi này
        Map<String, Object> dataForPlayer = new HashMap<>(data);
        dataForPlayer.put("yourMove", player.getMove());
        dataForPlayer.put("oppMove", player.getOpponent().getMove());
        dataForPlayer.put("score", roundScore);
        dataForPlayer.put("yourTotalScore", player.getMatchScore()); // Client dùng key này
        dataForPlayer.put("opponentTotalScore", player.getOpponent().getMatchScore()); // Client dùng key này

        msg.data = dataForPlayer;
        listener.onSendMessage(player, msg);
    }

    // --- HÀM CHO KHÁN GIẢ (ĐÃ TỐI ƯU) ---

    public void addSpectator(ClientHandler handler) {
        spectators.add(handler);
        handler.setSpectatingMatch(this);

        // 1. Tạo packet BẮT ĐẦU VÒNG (dùng trạng thái hiện tại)
        // (Lưu ý: chúng ta không có 'palette' ở đây, nên chỉ gửi màu đúng)
        Map<String, Object> colorData = new HashMap<>();
        colorData.put("correctColor", correctColorObject);
        colorData.put("palette", new ArrayList<>()); // Gửi list rỗng

        Map<String, Object> data = GamePacketFactory.createStartRoundPacket(this,
                (List<Color>) colorData.get("palette"), correctColorObject);

        // 2. Gửi cho chỉ spectator mới này
        Message msg = new Message(Message.Type.SPECTATE_UPDATE);
        msg.data = data;
        listener.onSendMessage(new PlayerState(handler), msg);
    }

    public void removeSpectator(ClientHandler handler) {
        spectators.remove(handler);
        System.out.println("Spectator " + handler.getUser().getUsername() + " removed from match " + matchId);
    }

    private void broadcastToSpectators(Message message) {
        for (ClientHandler spectator : spectators) {
            listener.onSendMessage(new PlayerState(spectator), message);
        }
    }

    // --- CÁC HÀM XỬ LÝ SỰ KIỆN (KHÔNG THAY ĐỔI) ---

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

    public void handleChatMessage(PlayerState senderState, Message originalMessage) {
        PlayerState recipientState = senderState.getOpponent();
        if (recipientState == null)
            return;
        String messageContent = (String) originalMessage.data.get("message");
        if (messageContent == null || messageContent.isBlank())
            return;

        Message forwardMessage = new Message(Message.Type.IN_GAME_CHAT);
        forwardMessage.data = Map.of(
                "sender", senderState.getClient().getUser().getUsername(),
                "message", messageContent);
        listener.onSendMessage(recipientState, forwardMessage);
    }

    private void endMatch(String reason) {
        Message endMsg = new Message(Message.Type.MATCH_END);
        endMsg.data = Map.of("reason", reason);
        listener.onSendMessage(player1, endMsg);
        listener.onSendMessage(player2, endMsg);
        broadcastToSpectators(endMsg);

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
        listener.onMatchFinished(this);
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
}