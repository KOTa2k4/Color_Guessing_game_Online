package shared.model;

import java.io.Serializable;
import java.util.Date;

public class MatchRecord implements Serializable {
    private static final long serialVersionUID = 1L; // Cần thiết cho Serializable

    private String player1Username;
    private String player2Username;
    private double player1Score;
    private double player2Score;
    private String winnerUsername; // Sẽ là null nếu hòa
    private Date matchDate;

    // Constructor để DAO dễ dàng tạo đối tượng
    public MatchRecord(String player1Username, String player2Username, double player1Score,
            double player2Score, String winnerUsername, Date matchDate) {
        this.player1Username = player1Username;
        this.player2Username = player2Username;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.winnerUsername = winnerUsername;
        this.matchDate = matchDate;
    }

    // Client sẽ cần các getters này để hiển thị
    public String getPlayer1Username() {
        return player1Username;
    }

    public String getPlayer2Username() {
        return player2Username;
    }

    public double getPlayer1Score() {
        return player1Score;
    }

    public double getPlayer2Score() {
        return player2Score;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public Date getMatchDate() {
        return matchDate;
    }

    @Override
    public String toString() {
        // Giúp hiển thị đơn giản trên JList của Client
        String winner = (winnerUsername == null) ? "HÒA" : winnerUsername + " thắng";
        return String.format("[%s] %s (%.1f) - (%.1f) %s. %s",
                matchDate.toString(),
                player1Username,
                player1Score,
                player2Score,
                player2Username,
                winner);
    }
}