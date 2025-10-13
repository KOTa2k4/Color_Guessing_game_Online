package server.service;

import server.dao.UserDAO;
import server.network.ClientHandler;
import shared.model.Message;
import shared.model.User;

import java.util.*;

public class LeaderboardService {
    private final UserDAO userDAO;

    public LeaderboardService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void sendLeaderboardTo(ClientHandler ch) {
        try {
            List<User> top = userDAO.getLeaderboard();
            List<Map<String, Object>> data = new ArrayList<>();
            for (User u : top) {
                data.add(Map.of(
                        "username", u.getUsername(),
                        "points", u.getTotalPoints(),
                        "wins", u.getTotalWins()));
            }

            Message m = new Message(Message.Type.LEADERBOARD_DATA);
            m.data = Map.of("users", data);
            ch.send(m);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
