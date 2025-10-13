package server.service;

import java.sql.SQLException;

import server.dao.UserDAO;
import shared.model.User;

public class AuthenticationService {
    private final UserDAO userDAO;

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User authenticate(String username, String passwordHash) {
        try {
            User user = userDAO.findByUsername(username);

            if (user == null) {
                System.out.println("New user registered: " + username);
                return userDAO.createUser(username, passwordHash);
            } else if (user.getPasswordHash().equals(passwordHash)) {
                return user;
            } else {
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Database error during authentication: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}