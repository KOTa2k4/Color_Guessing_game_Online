package server.service;

import java.util.Map;
import server.Lobby;
import server.network.ClientHandler;
import shared.model.Message;

public class ChatService implements IMessageService {
    private final Lobby lobby;

    public ChatService(Lobby lobby) {
        this.lobby = lobby;
    }

    // Xử lý một tin nhắn chat đến.

    @Override
    public void handleMessage(Message m, ClientHandler sender) { // <-- 1. Sửa tên và thứ tự
        // 2. Sửa 'originalMessage' thành 'm'
        String messageContent = (String) m.data.get("message");
        if (messageContent == null || messageContent.isBlank()) {
            return;
        }

        // Tạo một tin nhắn mới để gửi đi, thêm tên người gửi vào.
        Message broadcastMessage = new Message(Message.Type.CHAT_MESSAGE);
        broadcastMessage.data = Map.of(
                "sender", sender.getUser().getUsername(), // <-- 3. Dùng 'sender'
                "message", messageContent);

        // Gửi tin nhắn cho tất cả các client đang online
        for (ClientHandler recipient : lobby.getOnlineClients().values()) {
            recipient.send(broadcastMessage);
        }
    }
}