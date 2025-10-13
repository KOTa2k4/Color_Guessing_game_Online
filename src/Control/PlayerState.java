package Control;

public class PlayerState {
    private final ClientHandler client;
    private PlayerState opponent;
    private String move;
    private boolean rematchAccepted = false;
    private MatchSession matchSession;
    private boolean readyForNextRound = false;
    private double matchScore = 0;

    public double getMatchScore() {
        return matchScore;
    }

    public void addToMatchScore(double points) {
        this.matchScore += points;
    }

    public void resetMatchScore() {
        this.matchScore = 0;
    }

    public boolean isReadyForNextRound() {
        return readyForNextRound;
    }

    public void setReadyForNextRound(boolean readyForNextRound) {
        this.readyForNextRound = readyForNextRound;
    }

    public PlayerState(ClientHandler client) {
        this.client = client;
    }

    public ClientHandler getClient() {
        return client;
    }

    public PlayerState getOpponent() {
        return opponent;
    }

    public void setOpponent(PlayerState opponent) {
        this.opponent = opponent;
    }

    public String getMove() {
        return move;
    }

    public void setMove(String move) {
        this.move = move;
    }

    public boolean isRematchAccepted() {
        return rematchAccepted;
    }

    public void setRematchAccepted(boolean rematchAccepted) {
        this.rematchAccepted = rematchAccepted;
    }

    public MatchSession getMatchSession() {
        return matchSession;
    }

    public void setMatchSession(MatchSession matchSession) {
        this.matchSession = matchSession;
    }

    public void reset() {
        this.move = null;
        this.rematchAccepted = false;
        this.opponent = null;
        this.readyForNextRound = false;
        this.matchScore = 0;
    }

}
