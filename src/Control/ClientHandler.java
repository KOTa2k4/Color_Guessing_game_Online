package Control;

import model.Message;
import model.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler {
    private final MessageHandler messageHandler;
    private MatchSession match;
    private GameServer server;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private User user;
    public volatile boolean inGame = false;
    private ClientHandler opponent;

    public ClientHandler(GameServer server, Socket socket, ObjectInputStream in, ObjectOutputStream out, User user) {
        this.server = server;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.user = user;
        this.messageHandler = new MessageHandler(this);
    }

    // ---------------- Getters ----------------
    public User getUser() {
        return user;
    }

    public GameServer getServer() {
        return server;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean inGame) {
        this.inGame = inGame;
    }

    public MatchSession getMatch() {
        return match;
    }

    public void setMatch(MatchSession match) {
        this.match = match;
    }

    public ClientHandler getOpponent() {
        return opponent;
    }

    public void setOpponent(ClientHandler opponent) {
        this.opponent = opponent;
    }

    // ---------------- Network ----------------
    public void listen() {
        // Giao việc lắng nghe cho ExecutorService của GameServer
        // Bạn cần truyền ExecutorService vào ClientHandler hoặc lấy nó từ GameServer
        server.getPool().execute(() -> { // Giả sử GameServer có getPool()
            try {
                while (true) {
                    Message m = (Message) in.readObject();
                    server.getLobby().getView().showMessage("[MSG] Received " + m.type + " from " + user.getUsername());
                    messageHandler.handle(m);
                }
            } catch (Exception e) {
                server.getLobby().getView().showMessage("[DISCONNECT] Client disconnected: " + user.getUsername());
                cleanup();
            }
        });
    }

    public synchronized void send(Message m) {
        try {
            out.writeObject(m);
            out.reset();
            server.getLobby().getView().showMessage("[SEND] Sent " + m.type + " to " + user.getUsername());
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }
    }

    // ---------------- Game methods ----------------
    public void startGameWith(ClientHandler ch) {
        // Tạo PlayerState cho cả hai
        PlayerState ps1 = new PlayerState(this);
        PlayerState ps2 = new PlayerState(ch);

        ps1.setOpponent(ps2);
        ps2.setOpponent(ps1);

        MatchSession newMatch = new MatchSession(ps1, ps2);
        this.match = newMatch;
        ch.setMatch(newMatch);

        newMatch.start();

        server.getLobby().getView().showMessage(
                "[MATCH] Started between " + this.user.getUsername() + " and " + ch.getUser().getUsername());
    }

    // ---------------- Internal handlers (MessageHandler gọi) ----------------
    public void handleMoveInternal(Message m) {
        if (!inGame || match == null) {
            send(new Message(Message.Type.ERROR));
            return;
        }
        String colorGuess = (String) m.data.get("move");
        match.submitMove(getPlayerState(), colorGuess);
    }

    public void handleGuessColorInternal(Message m) {
        if (!inGame || match == null) {
            send(new Message(Message.Type.ERROR));
            return;
        }
        String colorGuess = (String) m.data.get("color");
        match.submitMove(getPlayerState(), colorGuess);
    }

    public void handleRematchRespInternal(Message m) {
        if (match == null)
            return;

        boolean accept = (boolean) m.data.get("accept");
        match.submitRematchResponse(getPlayerState(), accept);
    }

    public void handleExitInternal() {
        if (match != null)
            match.handleExit(getPlayerState());
    }

    public void handleMatchEndInternal() {
        if (opponent != null) {
            System.out.println("[REMATCH] Sending rematch request to " + opponent.getUser().getUsername());
            Message rematchReq = new Message(Message.Type.REMATCH_REQ);
            rematchReq.from = user.getUsername();
            opponent.send(rematchReq);
        } else {
            System.out.println("[REMATCH] Opponent is null for " + user.getUsername());
        }
    }

    public void handleLeaderboardReqInternal() {
        server.getLobby().getLeaderboardService().sendLeaderboardTo(this);
    }

    // ---------------- Cleanup ----------------
    public void cleanup() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
        inGame = false;
        match = null;
        opponent = null;
        server.getLobby().removeOnline(user.getUsername());
        server.getLobby().getView().showMessage("[LOBBY] User removed: " + user.getUsername());
    }

    // ---------------- Utilities ----------------
    public Map<String, Object> getUserInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("username", user.getUsername());
        info.put("points", user.getTotalPoints());
        info.put("wins", user.getTotalWins());
        info.put("status", inGame ? "BUSY" : "IDLE");
        return info;
    }

    // Trả về PlayerState tương ứng (dùng trong MatchSession)
    public PlayerState getPlayerState() {
        if (match == null)
            return null;
        // Tìm PlayerState trong match
        if (match.getPlayer1().getClient() == this)
            return match.getPlayer1();
        if (match.getPlayer2().getClient() == this)
            return match.getPlayer2();
        return null;
    }

    public void handleNextRoundInternal() {
        if (match == null)
            return;
        match.handleNextRound(getPlayerState());
    }

}
