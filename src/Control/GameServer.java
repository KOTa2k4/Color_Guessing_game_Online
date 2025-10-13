package Control;

import dao.UserDAO;
import model.Message;
import model.User;
import view.ServerView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private int port;
    private ServerSocket serverSocket;
    private final Lobby lobby;
    private final ServerView view;
    // TẠO MỘT BỂ LUỒNG CỐ ĐỊNH
    private final ExecutorService pool = Executors.newFixedThreadPool(20); // Ví dụ: 20 luồng

    public GameServer(int port) throws Exception {
        this.port = port;
        this.view = new ServerView();
        this.lobby = new Lobby(this, new UserDAO(), view);
    }

    public ExecutorService getPool() {
        return pool;
    }

    public Lobby getLobby() {
        return lobby;
    }

    public ServerView getView() {
        return view;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        view.showMessage("GameServer started on port " + port);

        while (true) {
            Socket s = serverSocket.accept();
            view.showMessage("New client connected: " + s.getInetAddress());
            pool.execute(() -> handleNewClient(s));
        }
    }

    private void handleNewClient(Socket s) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

            Message m = (Message) ois.readObject();
            if (m.type != Message.Type.LOGIN) {
                oos.writeObject(new Message(Message.Type.ERROR));
                s.close();
                return;
            }

            String username = (String) m.data.get("username");
            String passHash = (String) m.data.get("passwordHash");

            User user = lobby.getUserDAO().findByUsername(username);
            if (user == null) {
                user = lobby.getUserDAO().createUser(username, passHash);
                view.showMessage("New user registered: " + username);
            } else if (!user.getPasswordHash().equals(passHash)) {
                Message failMsg = new Message(Message.Type.LOGIN_FAIL);
                failMsg.data = Map.of("reason", "Wrong password");
                oos.writeObject(failMsg);
                s.close();
                return;
            }

            ClientHandler handler = new ClientHandler(this, s, ois, oos, user);
            lobby.addOnline(handler);

            handler.send(new Message(Message.Type.LOGIN_OK));
            handler.listen();
            view.showMessage("User logged in: " + username);

        } catch (Exception e) {
            view.showError("Client connection error: " + e.getMessage());
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new GameServer(55555).start();
    }
}
