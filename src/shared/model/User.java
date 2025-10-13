package shared.model;

import java.io.Serializable;

public class User implements Serializable {
    private int id;
    private String username;
    private String passwordHash; // store hashed in DB
    private double totalPoints;
    private int totalWins;

    public User() {
    }

    public void addWin() {
        this.totalWins++;
    }

    public void addPoints(double points) {
        this.totalPoints += points;
    }

    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    // getters/setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public double getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(double totalPoints) {
        this.totalPoints = totalPoints;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

}