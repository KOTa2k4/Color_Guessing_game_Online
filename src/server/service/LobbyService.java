package server.service;

import server.Lobby;
import server.network.ClientHandler;
import shared.model.Message;
import java.util.Map;

public class LobbyService implements IMessageService {
    private final Lobby lobby;
    private final MatchService matchService;

    public LobbyService(Lobby lobby, MatchService matchService) {
        this.lobby = lobby;
        this.matchService = matchService;
    }

    // Nhận tất cả tin nhắn liên quan đến Lobby.

    @Override
    public void handleMessage(Message m, ClientHandler sender) {
        switch (m.type) {
            case CHALLENGE:
                handleChallenge(sender, m);
                break;
            case CHALLENGE_RESP:
                handleChallengeResponse(sender, m);
                break;
            default:
                // Không làm gì nếu tin nhắn không khớp
                break;
        }
    }

    // Xử lý khi một người chơi gửi lời mời thách đấu.
    private void handleChallenge(ClientHandler challenger, Message m) {
        String targetUsername = (String) m.data.get("target");

        // Không cho phép tự thách đấu chính mình
        if (challenger.getUser().getUsername().equals(targetUsername)) {
            return;
        }

        ClientHandler targetHandler = lobby.findHandler(targetUsername);

        // Kiểm tra xem mục tiêu có online và đang rảnh không
        if (targetHandler != null && !targetHandler.isInGame()) {
            Message request = new Message(Message.Type.CHALLENGE_REQ);
            request.from = challenger.getUser().getUsername();
            targetHandler.send(request);
        } else {
            Message errorMsg = new Message(Message.Type.ERROR);
            errorMsg.data = Map.of("reason", "Player is busy or not found.");
            challenger.send(errorMsg);
        }
    }

    // Xử lý khi một người chơi phản hồi lại lời thách đấu.
    private void handleChallengeResponse(ClientHandler responder, Message m) {
        boolean accepted = (boolean) m.data.get("accept");
        String challengerUsername = m.to;
        ClientHandler challengerHandler = lobby.findHandler(challengerUsername);

        if (challengerHandler != null) {
            // Nếu người được mời đồng ý VÀ cả hai vẫn đang rảnh
            if (accepted && !challengerHandler.isInGame() && !responder.isInGame()) {
                matchService.startNewMatch(challengerHandler, responder);
            } else {
                // (Không làm gì nếu từ chối)
            }
        }
    }
}