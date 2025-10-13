package Control;

import dao.UserDAO;
import model.Message;
import model.User;
import view.ServerView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private final int port;
    private final Lobby lobby;
    private final ServerView view;
    private final AuthenticationService authService;
    private final MessageHandler messageHandler;
    private final BroadcastService broadcastService;
    private final ExecutorService clientProcessingPool = Executors.newCachedThreadPool();

    public GameServer(int port, ServerView view, Lobby lobby, AuthenticationService authService,
            MessageHandler messageHandler, BroadcastService broadcastService) {
        this.port = port;
        this.view = view;
        this.lobby = lobby;
        this.authService = authService;
        this.messageHandler = messageHandler;
        this.broadcastService = broadcastService;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            view.showMessage("GameServer started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                view.showMessage("New client connected: " + clientSocket.getInetAddress());
                clientProcessingPool.execute(() -> handleNewClient(clientSocket));
            }
        }
    }

    private void handleNewClient(Socket socket) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            Message loginMsg = (Message) ois.readObject();
            if (loginMsg.type != Message.Type.LOGIN) {
                oos.writeObject(new Message(Message.Type.ERROR));
                socket.close();
                return;
            }

            String username = (String) loginMsg.data.get("username");
            String passHash = (String) loginMsg.data.get("passwordHash");

            User user = authService.authenticate(username, passHash);

            if (user == null || lobby.isUserOnline(username)) {
                String reason = (user == null) ? "Wrong password" : "User already logged in";
                Message failMsg = new Message(Message.Type.LOGIN_FAIL);
                failMsg.data = Map.of("reason", reason);
                oos.writeObject(failMsg);
                socket.close();
                return;
            }

            ClientHandler handler = new ClientHandler(socket, ois, oos, user, messageHandler, this);
            lobby.addOnline(handler);
            broadcastService.broadcastUserList();

            handler.send(new Message(Message.Type.LOGIN_OK));
            clientProcessingPool.execute(handler::listen);
            view.showMessage("User logged in: " + username);

        } catch (Exception e) {
            view.showError("Client connection error: " + e.getMessage());
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public ExecutorService getPool() {
        return clientProcessingPool;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public BroadcastService getBroadcastService() {
        return broadcastService;
    }

    public static void main(String[] args) throws Exception {
        ServerView view = new ServerView();
        UserDAO userDAO = new UserDAO();

        Lobby lobby = new Lobby(view);

        AuthenticationService authService = new AuthenticationService(userDAO);
        BroadcastService broadcastService = new BroadcastService(lobby);
        LeaderboardService leaderboardService = new LeaderboardService(userDAO);

        MatchService matchService = new MatchService(userDAO, broadcastService);

        LobbyService lobbyService = new LobbyService(lobby, matchService);

        ChatService chatService = new ChatService(lobby);

        MessageHandler messageHandler = new MessageHandler(lobbyService, matchService, leaderboardService, chatService);

        GameServer server = new GameServer(55555, view, lobby, authService, messageHandler, broadcastService);

        server.start();
    }
}