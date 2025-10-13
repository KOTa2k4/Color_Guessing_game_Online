package Control;

import dao.UserDAO;
import model.User;
import model.Message;

import java.util.*;

public class BroadcastService {
    private final GameServer server;
    private final UserDAO userDAO;

    public BroadcastService(GameServer server, UserDAO userDAO) {
        this.server = server;
        this.userDAO = userDAO;
    }

    public void broadcastUserList() {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            // Lặp qua các ClientHandler đang online
            for (ClientHandler handler : server.getLobby().getOnlineClients().values()) {
                // ✅ LẤY USER TRỰC TIẾP TỪ CLIENTHANDLER, KHÔNG QUERY DB
                User u = handler.getUser();

                Map<String, Object> info = new HashMap<>();
                info.put("username", u.getUsername());
                info.put("points", u.getTotalPoints());
                info.put("wins", u.getTotalWins());
                info.put("status", handler.isInGame() ? "In match" : "Online");
                list.add(info);
            }

            Message m = new Message(Message.Type.USER_LIST);
            m.data = Map.of("users", list);

            // Gửi cho tất cả mọi người
            for (ClientHandler ch : server.getLobby().getOnlineClients().values()) {
                ch.send(m);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
