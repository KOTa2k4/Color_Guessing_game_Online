package server;

import java.util.HashMap;
import java.util.Map;
import server.dao.UserDAO;
import server.network.MessageHandler;
import server.service.*;
import server.view.ServerView;
import shared.model.Message;

public class ServerApp {

    public static void main(String[] args) throws Exception {
        ServerView view = new ServerView();
        UserDAO userDAO = new UserDAO();
        Lobby lobby = new Lobby(view);

        AuthenticationService authService = new AuthenticationService(userDAO);
        BroadcastService broadcastService = new BroadcastService(lobby);

        // 1. Khởi tạo tất cả các Service sẽ nhận tin nhắn
        MatchService matchService = new MatchService(userDAO, broadcastService);
        LobbyService lobbyService = new LobbyService(lobby, matchService);
        ChatService chatService = new ChatService(lobby);
        LeaderboardService leaderboardService = new LeaderboardService(userDAO);
        MatchHistoryService historyService = new MatchHistoryService(userDAO);
        AdminService adminService = new AdminService(matchService);

        // 2. Tạo "Bản đồ Đăng ký Dịch vụ"
        Map<Message.Type, IMessageService> registry = new HashMap<>();

        // Đăng ký LobbyService
        registry.put(Message.Type.CHALLENGE, lobbyService);
        registry.put(Message.Type.CHALLENGE_RESP, lobbyService);

        // Đăng ký MatchService
        registry.put(Message.Type.MOVE, matchService);
        registry.put(Message.Type.EXIT, matchService);
        registry.put(Message.Type.REMATCH_RESP, matchService);
        registry.put(Message.Type.NEXT_ROUND, matchService);
        registry.put(Message.Type.IN_GAME_CHAT, matchService);

        // Đăng ký các service đơn giản
        registry.put(Message.Type.CHAT_MESSAGE, chatService);
        registry.put(Message.Type.LEADERBOARD_REQ, leaderboardService);
        registry.put(Message.Type.GET_MATCH_HISTORY, historyService);

        // Đăng ký AdminService
        registry.put(Message.Type.GET_MATCH_LIST, adminService);
        registry.put(Message.Type.SPECTATE_REQ, adminService);
        registry.put(Message.Type.STOP_SPECTATING_REQ, adminService);

        // 3. Khởi tạo MessageHandler
        MessageHandler messageHandler = new MessageHandler(registry);

        // 4. Khởi tạo và BẮT ĐẦU GameServer
        GameServer server = new GameServer(
                55555,
                view,
                lobby,
                authService,
                messageHandler,
                broadcastService);

        server.start();
    }
}