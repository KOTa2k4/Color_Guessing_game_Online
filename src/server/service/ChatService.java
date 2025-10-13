package server.service;

import java.util.Map;

import server.Lobby;
import server.network.ClientHandler;
import shared.model.Message;

public class ChatService {
    private final Lobby lobby;

    public ChatService(Lobby lobby) {
        this.lobby = lobby;
    }

    /**
     * Xử lý một tin nhắn chat đến.
     * Nó sẽ thêm tên người gửi vào tin nhắn và phát lại cho mọi người.
     * 
     * @param sender          Người gửi tin.
     * @param originalMessage Tin nhắn gốc từ client.
     */
    public void handleChatMessage(ClientHandler sender, Message originalMessage) {
        String messageContent = (String) originalMessage.data.get("message");
        if (messageContent == null || messageContent.isBlank()) {
            return;
        }

        // Tạo một tin nhắn mới để gửi đi, thêm tên người gửi vào.
        Message broadcastMessage = new Message(Message.Type.CHAT_MESSAGE);
        broadcastMessage.data = Map.of(
                "sender", sender.getUser().getUsername(),
                "message", messageContent);

        // Gửi tin nhắn cho tất cả các client đang online
        for (ClientHandler recipient : lobby.getOnlineClients().values()) {
            recipient.send(broadcastMessage);
        }
    }
}