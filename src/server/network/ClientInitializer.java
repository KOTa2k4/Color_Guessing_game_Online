package server.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import server.Lobby;
import server.service.AuthenticationService;
import server.service.BroadcastService;
import server.view.ServerView;
import shared.model.Message;
import shared.model.User;

/**
 * Lớp này chỉ có một nhiệm vụ:
 * Nhận một Socket mới, xử lý toàn bộ logic Đăng nhập/Đăng ký.
 * Nếu thành công, nó sẽ khởi chạy ClientHandler.
 * Nếu thất bại, nó sẽ đóng Socket.
 */
public class ClientInitializer {

    private final Socket socket;
    private final AuthenticationService authService;
    private final Lobby lobby;
    private final BroadcastService broadcastService;
    private final MessageHandler messageHandler;
    private final ExecutorService threadPool;
    private final ServerView view;

    public ClientInitializer(Socket socket, AuthenticationService authService, Lobby lobby,
            BroadcastService broadcastService, MessageHandler messageHandler,
            ExecutorService threadPool, ServerView view) {
        this.socket = socket;
        this.authService = authService;
        this.lobby = lobby;
        this.broadcastService = broadcastService;
        this.messageHandler = messageHandler;
        this.threadPool = threadPool;
        this.view = view;
    }

    public void processConnection() {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            Message firstMsg = (Message) ois.readObject();
            String username = (String) firstMsg.data.get("username");
            String passHash = (String) firstMsg.data.get("passwordHash");

            User user = null;

            if (firstMsg.type == Message.Type.LOGIN) {
                user = authService.authenticate(username, passHash);
                if (user == null) {
                    Message failMsg = new Message(Message.Type.LOGIN_FAIL);
                    failMsg.data = Map.of("reason", authService.getLastError());
                    oos.writeObject(failMsg);
                    socket.close();
                    return;
                }
            } else if (firstMsg.type == Message.Type.REGISTER_REQ) {
                user = authService.register(username, passHash);
                if (user == null) {
                    Message failMsg = new Message(Message.Type.REGISTER_FAIL);
                    failMsg.data = Map.of("reason", authService.getLastError());
                    oos.writeObject(failMsg);
                    socket.close();
                    return;
                }
            } else {
                oos.writeObject(new Message(Message.Type.ERROR));
                socket.close();
                return;
            }

            if (lobby.isUserOnline(username)) {
                Message failMsg = new Message(Message.Type.LOGIN_FAIL);
                failMsg.data = Map.of("reason", "User already logged in");
                oos.writeObject(failMsg);
                socket.close();
                return;
            }

            // Khởi tạo ClientHandler
            ClientHandler handler = new ClientHandler(socket, ois, oos, user, messageHandler, lobby, broadcastService);
            lobby.addOnline(handler);
            broadcastService.broadcastUserList();

            Message okMsg = new Message(Message.Type.LOGIN_OK);
            okMsg.data = Map.of("isAdmin", user.isAdmin());
            handler.send(okMsg);

            // Chuyển cho ClientHandler trên một luồng mới
            threadPool.execute(handler::listen);
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
}