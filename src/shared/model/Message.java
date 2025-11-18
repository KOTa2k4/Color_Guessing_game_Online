package shared.model;

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
        IN_GAME_CHAT,
        GET_MATCH_HISTORY,
        MATCH_HISTORY_RESPONSE,
        REGISTER_REQ,
        REGISTER_FAIL,
        LOGOUT,
        GET_MATCH_LIST,
        MATCH_LIST_RESPONSE,
        SPECTATE_REQ,
        SPECTATE_UPDATE,
        STOP_SPECTATING_REQ
    }

    public Type type;
    public String from;
    public String to;
    public Map<String, Object> data;

    public Message() {
    }

    public Message(Type type) {
        this.type = type;
    }
}