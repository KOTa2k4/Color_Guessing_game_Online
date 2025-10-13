package server.game;

import shared.model.Message;

public interface MatchListener {
    void onSendMessage(PlayerState player, Message message);

    void onMatchDataSave(PlayerState p1, PlayerState p2, double score1, double score2, Integer winnerId);

    void onPlayerStatusUpdate();
}