package view;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerView {

    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public ServerView() {
        showMessage("Server view initialized.");
    }

    public void showMessage(String msg) {
        System.out.println("[" + sdf.format(new Date()) + "] " + msg);
    }

    public void showError(String msg) {
        System.err.println("[" + sdf.format(new Date()) + "] [ERROR] " + msg);
    }
}
