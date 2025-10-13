package model;

import java.io.Serializable;
import java.util.Map;

public class Message implements Serializable {
    public enum Type {
        LOGIN,
        LOGIN_OK,
        LOGIN_FAIL,
        USER_LIST,
        CHALLENGE,
        CHALLENGE_REQ,
        CHALLENGE_RESP,
        START_GAME,
        NEXT_ROUND,
        MOVE, MOVE_ACK,
        ROUND_RESULT,
        MATCH_END,
        EXIT, PING,
        PONG, ERROR,
        REMATCH_REQ,
        REMATCH_RESP,
        LEADERBOARD_REQ,
        LEADERBOARD_DATA,
        GUESS_COLOR,
        CHAT_MESSAGE,
        IN_GAME_CHAT
    }

    public Type type;
    public String from; // username
    public String to; // username
    public Map<String, Object> data; // payload

    public Message() {
    }

    public Message(Type type) {
        this.type = type;
    }
}