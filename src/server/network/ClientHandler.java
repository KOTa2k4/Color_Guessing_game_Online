package server.network;

import server.Lobby;
import server.service.BroadcastService;
import server.game.MatchSession;
import server.game.PlayerState;
import shared.model.Message;
import shared.model.User;

import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final User user;
    private final MessageHandler messageHandler;

    private final Lobby lobby;
    private final BroadcastService broadcastService;

    private MatchSession playingMatch;
    private MatchSession spectatingMatch;
    private volatile boolean inGame = false;

    public ClientHandler(Socket socket, ObjectInputStream in, ObjectOutputStream out, User user,
            MessageHandler messageHandler, Lobby lobby, BroadcastService broadcastService) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.user = user;
        this.messageHandler = messageHandler;
        this.lobby = lobby;
        this.broadcastService = broadcastService;
    }

    public void listen() {
        try {
            while (!socket.isClosed()) {
                Message m = (Message) in.readObject();
                if (m.type == Message.Type.LOGOUT) {
                    System.out.println("User " + user.getUsername() + " requested logout.");
                    cleanup();
                    break;
                }
                messageHandler.handle(m, this);
            }
        } catch (IOException | ClassNotFoundException e) {
            cleanup();
        }
    }

    public synchronized void send(Message m) {
        try {
            if (!socket.isClosed()) {
                out.writeObject(m);
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("Error sending message to " + user.getUsername() + ": " + e.getMessage());
            cleanup();
        }
    }

    private void cleanup() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        // Nếu người chơi đang TRONG TRẬN, báo cho trận đấu
        if (this.playingMatch != null) {
            playingMatch.handleExit(getPlayerState());
        }

        // Nếu người chơi đang XEM TRẬN, báo cho trận đấu
        if (this.spectatingMatch != null) {
            spectatingMatch.removeSpectator(this);
        }

        // Tự xóa mình khỏi sảnh chờ và kích hoạt broadcast
        lobby.removeOnline(this.user.getUsername());
        broadcastService.broadcastUserList();

        System.out.println("[CLEANUP] Cleaned up client: " + this.user.getUsername());
    }

    public User getUser() {
        return user;
    }

    public MatchSession getMatch() {
        return playingMatch;
    }

    public void setMatch(MatchSession match) {
        this.playingMatch = match;
        if (match != null) {
            this.spectatingMatch = null;
        }
    }

    public MatchSession getSpectatingMatch() {
        return spectatingMatch;
    }

    public void setSpectatingMatch(MatchSession match) {
        this.spectatingMatch = match;
        if (match != null) {
            this.playingMatch = null;
        }
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public PlayerState getPlayerState() {
        if (playingMatch == null)
            return null;
        if (playingMatch.getPlayer1().getClient() == this)
            return playingMatch.getPlayer1();
        if (playingMatch.getPlayer2().getClient() == this)
            return playingMatch.getPlayer2();
        return null;
    }
}