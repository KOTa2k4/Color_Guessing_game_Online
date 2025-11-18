package server;

import server.network.ClientInitializer;

import server.network.MessageHandler;
import server.service.AuthenticationService;
import server.service.BroadcastService;
import server.view.ServerView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Vận hành Server, Chấp nhận kết nối và giao phó cho ClientInitializer.

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

                clientProcessingPool.execute(() -> {
                    new ClientInitializer(
                            clientSocket,
                            authService,
                            lobby,
                            broadcastService,
                            messageHandler,
                            clientProcessingPool,
                            view).processConnection();
                });
            }
        }
    }
}