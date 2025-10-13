package Control;

import model.Message;
import model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastService {
    private final Lobby lobby;

    public BroadcastService(Lobby lobby) {
        this.lobby = lobby;
    }

    public void broadcastUserList() {
        if (lobby == null)
            return;

        List<Map<String, Object>> userListInfo = new ArrayList<>();

        for (ClientHandler handler : lobby.getOnlineClients().values()) {
            User u = handler.getUser();
            Map<String, Object> info = new HashMap<>();
            info.put("username", u.getUsername());
            info.put("points", u.getTotalPoints());
            info.put("wins", u.getTotalWins());
            info.put("status", handler.isInGame() ? "In match" : "Online");
            userListInfo.add(info);
        }

        Message m = new Message(Message.Type.USER_LIST);
        m.data = Map.of("users", userListInfo);

        for (ClientHandler ch : lobby.getOnlineClients().values()) {
            ch.send(m);
        }
    }
}