package server.service;

import server.dao.UserDAO;
import server.network.ClientHandler;
import shared.model.MatchRecord;
import shared.model.Message;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class MatchHistoryService implements IMessageService {

    private final UserDAO userDAO;

    public MatchHistoryService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Lấy lịch sử đấu của người chơi và gửi về cho họ.
    @Override
    public void handleMessage(Message m, ClientHandler sender) {
        try {
            int userId = sender.getUser().getId();

            List<MatchRecord> history = userDAO.getMatchHistory(userId);

            Message response = new Message(Message.Type.MATCH_HISTORY_RESPONSE);
            response.data = Map.of("history", (Serializable) history);

            sender.send(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}