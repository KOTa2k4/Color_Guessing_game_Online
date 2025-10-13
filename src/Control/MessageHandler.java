package Control;

import model.Message;

public class MessageHandler {
    private final ClientHandler client;

    public MessageHandler(ClientHandler client) {
        this.client = client;
    }

    public void handle(Message m) {
        switch (m.type) {
            case CHALLENGE -> handleChallenge(m);
            case CHALLENGE_RESP -> handleChallengeResp(m);
            case MOVE -> handleMove(m);
            case EXIT -> handleExit();
            case MATCH_END -> handleMatchEnd();
            case REMATCH_RESP -> handleRematchResp(m);
            case LEADERBOARD_REQ -> handleLeaderboardReq();
            case GUESS_COLOR -> client.handleGuessColorInternal(m);
            case NEXT_ROUND -> handleNextRound();

            default -> {
                // unknown type
            }
        }
    }

    private void handleChallenge(Message m) {
        String target = (String) m.data.get("target");
        ClientHandler targetH = client.getServer().getLobby().findHandler(target);
        if (targetH != null && !targetH.isInGame()) {
            Message req = new Message(Message.Type.CHALLENGE_REQ);
            req.from = client.getUser().getUsername();
            req.to = target;
            req.data = java.util.Map.of();
            targetH.send(req);
        } else {
            Message err = new Message(Message.Type.ERROR);
            err.data = java.util.Map.of("msg", "Target busy or not found");
            client.send(err);
        }
    }

    private void handleChallengeResp(Message m) {
        boolean ok = Boolean.TRUE.equals(m.data.get("accept"));
        String challenger = m.to;
        ClientHandler ch = client.getServer().getLobby().findHandler(challenger);
        if (ch != null) {
            Message resp = new Message(Message.Type.CHALLENGE_RESP);
            resp.from = client.getUser().getUsername();
            resp.to = challenger;
            resp.data = java.util.Map.of("accept", ok);
            ch.send(resp);
            if (ok)
                client.startGameWith(ch);
        }
    }

    private void handleMove(Message m) {
        client.handleMoveInternal(m);
    }

    private void handleExit() {
        client.handleExitInternal();
    }

    private void handleMatchEnd() {
        client.handleMatchEndInternal();
    }

    private void handleRematchResp(Message m) {
        client.handleRematchRespInternal(m);
    }

    private void handleLeaderboardReq() {
        client.handleLeaderboardReqInternal();
    }

    private void handleNextRound() {
        client.handleNextRoundInternal();
    }

}
