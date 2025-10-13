package Control;

import dao.UserDAO;
import view.ServerView;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Lobby {
    private final GameServer server;
    private final UserDAO userDAO;
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private final BroadcastService broadcastService;
    private final LeaderboardService leaderboardService;
    private final ServerView view;

    public Lobby(GameServer server, UserDAO userDAO, ServerView view) {
        this.server = server;
        this.userDAO = userDAO;
        this.view = view;
        this.broadcastService = new BroadcastService(server, userDAO);
        this.leaderboardService = new LeaderboardService(userDAO);
    }

    // ----------- Getters -----------
    public UserDAO getUserDAO() {
        return userDAO;
    }

    public BroadcastService getBroadcastService() {
        return broadcastService;
    }

    public LeaderboardService getLeaderboardService() {
        return leaderboardService;
    }

    public ServerView getView() {
        return view;
    }

    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }

    // ----------- Player management -----------
    public void addOnline(ClientHandler handler) {
        onlineClients.put(handler.getUser().getUsername(), handler);
        broadcastUserList();
        view.showMessage("User added to lobby: " + handler.getUser().getUsername());
    }

    public void removeOnline(String username) {
        onlineClients.remove(username);
        broadcastUserList();
        view.showMessage("User removed from lobby: " + username);
    }

    public ClientHandler findHandler(String username) {
        return onlineClients.get(username);
    }

    // ----------- Broadcast -----------
    public void broadcastUserList() {
        broadcastService.broadcastUserList();
    }

}
