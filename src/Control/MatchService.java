package Control;

import dao.UserDAO;
import model.Message;
import model.User;

public class MatchService implements MatchListener {
    private final UserDAO userDAO;
    private final BroadcastService broadcastService;

    public MatchService(UserDAO userDAO, BroadcastService broadcastService) {
        this.userDAO = userDAO;
        this.broadcastService = broadcastService;
    }

    public void startNewMatch(ClientHandler p1Handler, ClientHandler p2Handler) {
        PlayerState ps1 = new PlayerState(p1Handler);
        PlayerState ps2 = new PlayerState(p2Handler);

        MatchSession newMatch = new MatchSession(ps1, ps2, this);

        p1Handler.setMatch(newMatch);
        p2Handler.setMatch(newMatch);

        newMatch.start();
        onPlayerStatusUpdate();
    }

    public void submitMove(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            String move = (String) m.data.get("move");
            match.submitMove(sender.getPlayerState(), move);
        }
    }

    public void handleRematchResponse(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            boolean accept = (boolean) m.data.get("accept");
            match.submitRematchResponse(sender.getPlayerState(), accept);
        }
    }

    public void handleNextRound(ClientHandler sender) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            match.handleNextRound(sender.getPlayerState());
        }
    }

    public void handleExit(ClientHandler sender) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            match.handleExit(sender.getPlayerState());
        }
    }

    public void handleInGameChat(ClientHandler sender, Message m) {
        MatchSession match = sender.getMatch();
        if (match != null) {
            // Ủy quyền cho chính trận đấu đó xử lý tin nhắn
            match.handleChatMessage(sender.getPlayerState(), m);
        }
    }

    @Override
    public void onSendMessage(PlayerState player, Message message) {
        // gửi tin nhắn ra mạng
        if (player != null && player.getClient() != null) {
            player.getClient().send(message);
        }
    }

    @Override
    public void onMatchDataSave(PlayerState p1, PlayerState p2, double score1, double score2, Integer winnerId) {
        // cập nhật trạng thái trong bộ nhớ VÀ lưu vào CSDL
        User user1 = p1.getClient().getUser();
        User user2 = p2.getClient().getUser();

        // Cập nhật điểm gốc trong bộ nhớ
        if (score1 > score2) {
            user1.addWin();
            user1.addPoints(1);
        } else if (score2 > score1) {
            user2.addWin();
            user2.addPoints(1);
        }

        // Lưu kết quả vào CSDL
        userDAO.saveMatchResult(user1.getId(), user2.getId(), score1, score2, winnerId);
    }

    @Override
    public void onPlayerStatusUpdate() {
        // broadcast lại danh sách người dùng
        broadcastService.broadcastUserList();
    }
}