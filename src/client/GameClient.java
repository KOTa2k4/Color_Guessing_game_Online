package client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

import shared.model.Message;
import shared.model.User;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> onMessage;

    public GameClient(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        new Thread(() -> {
            try {
                while (true) {
                    Message m = (Message) in.readObject();
                    if (onMessage != null)
                        onMessage.accept(m);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setOnMessage(Consumer<Message> cb) {
        this.onMessage = cb;
    }

    public void send(Message m) throws Exception {
        out.writeObject(m);
        out.reset();
    }

    public void close() throws Exception {
        socket.close();
    }
}