package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import server.network.ClientHandler;
import server.view.ServerView;

public class Lobby {
    private final ServerView view;
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();

    public Lobby(ServerView view) {
        this.view = view;
    }

    public void addOnline(ClientHandler handler) {
        onlineClients.put(handler.getUser().getUsername(), handler);
        view.showMessage("User added to lobby: " + handler.getUser().getUsername());
    }

    public void removeOnline(String username) {
        if (onlineClients.remove(username) != null) {
            view.showMessage("User removed from lobby: " + username);
        }
    }

    public boolean isUserOnline(String username) {
        return onlineClients.containsKey(username);
    }

    public ClientHandler findHandler(String username) {
        return onlineClients.get(username);
    }

    public Map<String, ClientHandler> getOnlineClients() {
        return onlineClients;
    }
}