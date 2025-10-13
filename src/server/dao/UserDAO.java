package server.dao;

import server.config.DBConfig;
import shared.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public UserDAO() {
    }

    public User findByUsername(String username) throws SQLException {

        String sql = "SELECT id, username, password_hash, total_points, total_wins FROM users WHERE username=?";

        try (Connection conn = DataSourceFactory.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

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
        try (Connection conn = DataSourceFactory.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt(1));
                    u.setUsername(username);
                    u.setPasswordHash(passwordHash);
                    return u;
                }
            }
        }
        return null;
    }

    /** Lưu kết quả một trận đấu vào bảng matches */
    public void saveMatchResult(int playerAId, int playerBId, double scoreA, double scoreB, Integer winnerId)
            throws SQLException {
        String insertMatchSql = "INSERT INTO matches (player_a, player_b, score_a, score_b, winner_id, played_at) VALUES (?, ?, ?, ?, ?, NOW())";
        String updateUserSql = "UPDATE users SET total_points = total_points + ?, total_wins = total_wins + ? WHERE id = ?";

        Connection conn = null;
        try {
            conn = DataSourceFactory.getDataSource().getConnection();
            conn.setAutoCommit(false); // Bắt đầu giao dịch

            // Thao tác 1
            try (PreparedStatement stmt = conn.prepareStatement(insertMatchSql)) {
                // ... set các tham số ...
                stmt.executeUpdate();
            }

            try (PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql)) {
                // Thao tác 2: Update người chơi A
                updateUserStmt.setDouble(1, scoreA);
                updateUserStmt.setInt(2, (winnerId != null && winnerId == playerAId) ? 1 : 0);
                updateUserStmt.setInt(3, playerAId);
                updateUserStmt.executeUpdate();

                // Thao tác 3: Update người chơi B (tái sử dụng cùng một statement)
                updateUserStmt.setDouble(1, scoreB);
                updateUserStmt.setInt(2, (winnerId != null && winnerId == playerBId) ? 1 : 0);
                updateUserStmt.setInt(3, playerBId);
                updateUserStmt.executeUpdate();
            }

            conn.commit(); // Xác nhận tất cả thay đổi
        } catch (SQLException e) {
            if (conn != null)
                conn.rollback(); // Hủy bỏ tất cả nếu có lỗi
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close(); // Trả kết nối về bể
            }
        }
    }

    /** Lấy bảng xếp hạng người chơi */
    public List<User> getLeaderboard() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, total_points, total_wins " +
                "FROM users ORDER BY total_points DESC, total_wins DESC";
        try (Connection conn = DataSourceFactory.getDataSource().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
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
