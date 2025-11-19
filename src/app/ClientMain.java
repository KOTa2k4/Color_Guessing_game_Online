package app;

import client.view.LobbyView;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        String host = "172.11.42.188";
        int port = 55555;
        LobbyView ui = new LobbyView(host, port);
        ui.setVisible(true);
    }
}