package app;

import client.view.LobbyView;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 55555;
        LobbyView ui = new LobbyView(host, port);
        ui.setVisible(true);
    }
}