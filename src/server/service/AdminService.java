package server.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import server.game.MatchSession;
import server.network.ClientHandler;
import shared.model.Message;

public class AdminService implements IMessageService {
    private final MatchService matchService;

    public AdminService(MatchService matchService) {
        this.matchService = matchService;
    }

    @Override
    public void handleMessage(Message m, ClientHandler sender) {
        if (!sender.getUser().isAdmin()) {
            return;
        }

        switch (m.type) {
            case GET_MATCH_LIST:
                sendActiveMatches(sender);
                break;
            case SPECTATE_REQ:
                handleSpectateRequest(sender, m);
                break;
            case STOP_SPECTATING_REQ:
                handleStopSpectating(sender);
                break;
            default:
                break;
        }
    }

    // Xử lý khi Admin ngừng xem

    private void handleStopSpectating(ClientHandler sender) {
        MatchSession match = sender.getSpectatingMatch();
        if (match != null) {
            match.removeSpectator(sender);
            sender.setSpectatingMatch(null); // Xóa trạng thái
        }
    }

    // Gửi danh sách các trận đang hoạt động cho Admin.

    private void sendActiveMatches(ClientHandler handler) {
        Map<String, MatchSession> matches = matchService.getActiveMatches();
        List<Map<String, String>> matchList = new ArrayList<>();

        for (MatchSession match : matches.values()) {
            matchList.add(Map.of(
                    "id", match.getMatchId(),
                    "p1", match.getPlayer1().getClient().getUser().getUsername(),
                    "p2", match.getPlayer2().getClient().getUser().getUsername()));
        }

        Message response = new Message(Message.Type.MATCH_LIST_RESPONSE);
        response.data = Map.of("matches", (Serializable) matchList);
        handler.send(response);
    }

    // Xử lý khi Admin yêu cầu xem 1 trận cụ thể.

    private void handleSpectateRequest(ClientHandler handler, Message m) {
        String matchId = (String) m.data.get("matchId");
        MatchSession match = matchService.findMatchById(matchId);

        if (match != null) {
            match.addSpectator(handler);
        } else {
            // Gửi tin nhắn lỗi nếu muốn
        }
    }
}