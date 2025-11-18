package server.service;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.dao.UserDAO;
import server.game.MatchListener;
import server.game.MatchSession;
import server.game.PlayerState;
import server.network.ClientHandler;
import shared.model.Message;
import shared.model.User;

public class MatchService implements MatchListener, IMessageService {
    private final UserDAO userDAO;
    private final BroadcastService broadcastService;
    private final Map<String, MatchSession> activeMatches = new ConcurrentHashMap<>();

    public MatchService(UserDAO userDAO, BroadcastService broadcastService) {
        this.userDAO = userDAO;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handleMessage(Message m, ClientHandler sender) {
        // Chỉ xử lý nếu người gửi thực sự đang trong trận
        MatchSession match = sender.getMatch();
        if (match == null) {
            return;
        }

        switch (m.type) {
            case MOVE:
                submitMove(sender, m);
                break;
            case REMATCH_RESP:
                handleRematchResponse(sender, m);
                break;
            case NEXT_ROUND:
                handleNextRound(sender);
                break;
            case EXIT:
                handleExit(sender);
                break;
            case IN_GAME_CHAT:
                handleInGameChat(sender, m);
                break;
            default:
                break;
        }
    }

    private void submitMove(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            String move = (String) m.data.get("move");
            match.submitMove(sender.getPlayerState(), move);
        }
    }

    private void handleRematchResponse(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            boolean accept = (boolean) m.data.get("accept");
            match.submitRematchResponse(sender.getPlayerState(), accept);
        }
    }

    private void handleNextRound(ClientHandler sender) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            match.handleNextRound(sender.getPlayerState());
        }
    }

    private void handleExit(ClientHandler sender) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            match.handleExit(sender.getPlayerState());
        }
    }

    private void handleInGameChat(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            match.handleChatMessage(sender.getPlayerState(), m);
        }
    }

    public void startNewMatch(ClientHandler p1Handler, ClientHandler p2Handler) {
        PlayerState ps1 = new PlayerState(p1Handler);
        PlayerState ps2 = new PlayerState(p2Handler);

        MatchSession newMatch = new MatchSession(ps1, ps2, this);
        activeMatches.put(newMatch.getMatchId(), newMatch);

        p1Handler.setMatch(newMatch);
        p2Handler.setMatch(newMatch);

        newMatch.start();
        onPlayerStatusUpdate();
    }

    public Map<String, MatchSession> getActiveMatches() {
        return activeMatches;
    }

    public MatchSession findMatchById(String matchId) {
        return activeMatches.get(matchId);
    }

    @Override
    public void onSendMessage(PlayerState player, Message message) {
        if (player != null && player.getClient() != null) {
            player.getClient().send(message);
        }
    }

    @Override
    public void onMatchDataSave(PlayerState p1, PlayerState p2, double score1, double score2, Integer winnerId) {
        // ... (code đã sửa của bạn) ...
        User user1 = p1.getClient().getUser();
        User user2 = p2.getClient().getUser();
        try {
            userDAO.saveMatchResult(user1.getId(), user2.getId(), score1, score2, winnerId);
            user1.setTotalPoints(userDAO.getTotalPoints(user1.getId()));
            user1.setTotalWins(userDAO.getTotalWins(user1.getId()));
            user2.setTotalPoints(userDAO.getTotalPoints(user2.getId()));
            user2.setTotalWins(userDAO.getTotalWins(user2.getId()));
        } catch (SQLException e) {
            System.err.println("!!! LỖI NGHIÊM TRỌNG: Không thể lưu kết quả trận đấu vào CSDL !!!");
            e.printStackTrace();
        }
    }

    @Override
    public void onPlayerStatusUpdate() {
        broadcastService.broadcastUserList();
    }

    @Override
    public void onMatchFinished(MatchSession session) {
        if (session != null) {
            activeMatches.remove(session.getMatchId());
            System.out.println("Match finished and removed: " + session.getMatchId());
        }
    }
}