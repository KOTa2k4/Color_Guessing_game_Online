package server.service;

import java.sql.SQLException;

import server.dao.UserDAO;
import shared.model.User;

public class AuthenticationService {
    private final UserDAO userDAO;
    private String lastError;

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    // ĐĂNG NHẬP.

    public User authenticate(String username, String passwordHash) {
        try {
            User user = userDAO.findByUsername(username);

            if (user == null) {
                lastError = "User not found.";
                return null;
            }

            if (user.getPasswordHash().equals(passwordHash)) {
                return user;
            } else {
                lastError = "Wrong password.";
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lastError = "Database error.";
            return null;
        }
    }

    // Chỉ xử lý ĐĂNG KÝ.

    public User register(String username, String passwordHash) {
        try {
            User existingUser = userDAO.findByUsername(username);

            if (existingUser != null) {
                lastError = "Username is already taken.";
                return null;
            }

            return userDAO.createUser(username, passwordHash);

        } catch (SQLException e) {
            e.printStackTrace();
            lastError = "Database error.";
            return null;
        }
    }

    public String getLastError() {
        return lastError;
    }
}