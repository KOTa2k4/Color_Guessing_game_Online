package server.network;

import server.service.ChatService;
import server.service.LeaderboardService;
import server.service.LobbyService;
import server.service.MatchService;
import shared.model.Message;

public class MessageHandler {
    private final LobbyService lobbyService;
    private final MatchService matchService;
    private final LeaderboardService leaderboardService;
    private final ChatService chatService;

    public MessageHandler(LobbyService lobbyService, MatchService matchService,
            LeaderboardService leaderboardService, ChatService chatService) {
        this.lobbyService = lobbyService;
        this.matchService = matchService;
        this.leaderboardService = leaderboardService;
        this.chatService = chatService;
    }

    /**
     * "Tổng đài" nhận một cuộc gọi (Message) từ một người dùng (sender)
     * và chuyển đến đúng bộ phận (Service) để xử lý.
     * 
     * @param m      Tin nhắn từ client.
     * @param sender ClientHandler của người gửi tin nhắn.
     */
    public void handle(Message m, ClientHandler sender) {
        switch (m.type) {
            // Các yêu cầu liên quan đến sảnh chờ
            case CHALLENGE -> lobbyService.handleChallenge(sender, m);
            case CHALLENGE_RESP -> lobbyService.handleChallengeResponse(sender, m);

            // Các yêu cầu trong trận đấu
            case MOVE -> matchService.submitMove(sender, m);
            case EXIT -> matchService.handleExit(sender);
            case REMATCH_RESP -> matchService.handleRematchResponse(sender, m);
            case NEXT_ROUND -> matchService.handleNextRound(sender);

            // Các yêu cầu khác
            case LEADERBOARD_REQ -> leaderboardService.sendLeaderboardTo(sender);
            case CHAT_MESSAGE -> chatService.handleChatMessage(sender, m);
            case IN_GAME_CHAT -> matchService.handleInGameChat(sender, m);
            default -> {
                System.out.println("Unknown message type received: " + m.type);
            }
        }
    }
}