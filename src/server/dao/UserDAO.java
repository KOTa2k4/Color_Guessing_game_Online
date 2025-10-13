package server.dao;

import server.config.DBConfig;
import shared.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private Connection conn;

    public UserDAO() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
        conn = DriverManager.getConnection(DBConfig.getJdbcUrl(), DBConfig.DB_USER, DBConfig.DB_PASS);
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password_hash, total_points, total_wins FROM users WHERE username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setTotalPoints(rs.getDouble("total_points"));
                    u.setTotalWins(rs.getInt("total_wins"));
                    return u;
                }
            }
        }
        return null;
    }

    public User createUser(String username, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt(1));
                    u.setUsername(username);
                    return u;
                }
            }
        }
        return null;
    }

    /** Lưu kết quả một trận đấu vào bảng matches */
    public void saveMatchResult(int playerAId, int playerBId, double scoreA, double scoreB, Integer winnerId) {
        String sql = "INSERT INTO matches (player_a, player_b, score_a, score_b, winner_id, played_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerAId);
            stmt.setInt(2, playerBId);
            stmt.setDouble(3, scoreA);
            stmt.setDouble(4, scoreB);
            if (winnerId != null) {
                stmt.setInt(5, winnerId);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Cập nhật điểm và số trận thắng cho mỗi người chơi
        updateUserStats(playerAId, scoreA, (winnerId != null && winnerId == playerAId));
        updateUserStats(playerBId, scoreB, (winnerId != null && winnerId == playerBId));
    }

    /** Cập nhật điểm & số trận thắng cho user */
    private void updateUserStats(int userId, double points, boolean isWinner) {
        String sql = "UPDATE users SET total_points = total_points + ?, total_wins = total_wins + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, points);
            stmt.setInt(2, isWinner ? 1 : 0);
            stmt.setInt(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Lấy bảng xếp hạng người chơi */
    public List<User> getLeaderboard() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, total_points, total_wins " +
                "FROM users ORDER BY total_points DESC, total_wins DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                // bỏ passwordHash
                u.setTotalPoints(rs.getDouble("total_points"));
                u.setTotalWins(rs.getInt("total_wins"));
                users.add(u);
            }

        }
        return users;
    }
}
