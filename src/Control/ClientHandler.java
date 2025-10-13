package Control;

import model.Message;
import model.User;

import java.io.*;
import java.net.Socket;

// quản lý kết nối và luồng I/O.
public class ClientHandler {
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    private final User user;
    private final MessageHandler messageHandler;
    private final GameServer server;

    private MatchSession match;
    private volatile boolean inGame = false;

    public ClientHandler(Socket socket, ObjectInputStream in, ObjectOutputStream out, User user,
            MessageHandler messageHandler, GameServer server) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.user = user;
        this.messageHandler = messageHandler;
        this.server = server;
    }

    public void listen() {
        try {
            while (!socket.isClosed()) {
                Message m = (Message) in.readObject();
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
        // Đóng socket
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        // Nếu người chơi đang trong trận, thông báo cho trận đấu biết họ đã thoát
        if (this.match != null) {
            match.handleExit(getPlayerState());
        }

        // Tự xóa mình khỏi sảnh chờ và kích hoạt broadcast
        server.getLobby().removeOnline(this.user.getUsername());
        server.getBroadcastService().broadcastUserList();

        System.out.println("[CLEANUP] Cleaned up client: " + this.user.getUsername());
    }

    public User getUser() {
        return user;
    }

    public MatchSession getMatch() {
        return match;
    }

    public void setMatch(MatchSession match) {
        this.match = match;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public PlayerState getPlayerState() {
        if (match == null)
            return null;
        if (match.getPlayer1().getClient() == this)
            return match.getPlayer1();
        if (match.getPlayer2().getClient() == this)
            return match.getPlayer2();
        return null;
    }
}