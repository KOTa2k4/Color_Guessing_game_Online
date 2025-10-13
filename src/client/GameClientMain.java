package client;

import view.GameClientUI;

public class GameClientMain {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 55555;
        GameClientUI ui = new GameClientUI(host, port);
        ui.setVisible(true);
    }
}