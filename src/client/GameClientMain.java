package client;

import client.view.LobbyView;

public class GameClientMain {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 55555;
        LobbyView ui = new LobbyView(host, port);
        ui.setVisible(true);
    }
}