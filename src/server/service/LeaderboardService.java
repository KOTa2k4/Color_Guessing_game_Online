package server.service;

import server.dao.UserDAO;
import server.network.ClientHandler;
import shared.model.Message;
import shared.model.User;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

public class LeaderboardService implements IMessageService {
    private final UserDAO userDAO;

    public LeaderboardService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // Xử lý khi client yêu cầu bảng xếp hạng.
    @Override
    public void handleMessage(Message m, ClientHandler sender) {
        try {
            List<User> top = userDAO.getLeaderboard();
            List<Map<String, Object>> data = new ArrayList<>();
            for (User u : top) {
                data.add(Map.of(
                        "username", u.getUsername(),
                        "points", u.getTotalPoints(),
                        "wins", u.getTotalWins()));
            }

            Message response = new Message(Message.Type.LEADERBOARD_DATA);
            response.data = Map.of("users", (Serializable) data);

            sender.send(response);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}