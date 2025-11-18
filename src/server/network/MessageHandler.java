package server.network;

import java.util.Map;
import server.service.IMessageService;
import shared.model.Message;

public class MessageHandler {
    private final Map<Message.Type, IMessageService> serviceRegistry;

    public MessageHandler(Map<Message.Type, IMessageService> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void handle(Message m, ClientHandler sender) {
        IMessageService service = serviceRegistry.get(m.type);
        if (service != null) {
            service.handleMessage(m, sender);
        } else {
            handleInternalMessage(m, sender);
        }
    }

    private void handleInternalMessage(Message m, ClientHandler sender) {
        System.out.println("Unknown or un-registered message type: " + m.type);
    }
}