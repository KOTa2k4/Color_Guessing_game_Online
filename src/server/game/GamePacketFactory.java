package server.game;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lớp này chịu trách nhiệm "đóng gói" dữ liệu game thành các Map
 * để chuẩn bị gửi đi (thành Message).
 */
public class GamePacketFactory {

    /**
     * Tạo packet (gói tin) chung cho tin nhắn BẮT ĐẦU VÒNG
     * (dùng cho START_GAME và SPECTATE_UPDATE).
     */
    public static Map<String, Object> createStartRoundPacket(MatchSession match, List<Color> palette,
            Color correctColor) {
        Map<String, Object> data = new HashMap<>();

        // 1. Gói bảng màu
        List<Map<String, Integer>> colorsAsRgb = new ArrayList<>();
        for (Color c : palette) {
            colorsAsRgb.add(Map.of("r", c.getRed(), "g", c.getGreen(), "b", c.getBlue()));
        }
        data.put("colors", (Serializable) colorsAsRgb);

        // 2. Gói màu đúng
        data.put("correctColor", Map.of("r", correctColor.getRed(), "g", correctColor.getGreen(), "b",
                correctColor.getBlue()));

        // 3. Gói thông tin chung
        data.put("round", match.getCurrentRound());
        data.put("maxRounds", MatchSession.MAX_ROUNDS); // Cần MAX_ROUNDS là 'public static'
        data.put("p1Name", match.getPlayer1().getClient().getUser().getUsername());
        data.put("opponent", match.getPlayer2().getClient().getUser().getUsername());
        data.put("matchId", match.getMatchId());

        // 4. Gói điểm số (dùng P1 làm "you" cho khán giả)
        data.put("yourScore", match.getPlayer1().getMatchScore());
        data.put("opponentScore", match.getPlayer2().getMatchScore());

        return data;
    }

    /**
     * Tạo packet (gói tin) chung cho tin nhắn KẾT THÚC VÒNG
     * (dùng cho ROUND_RESULT và SPECTATE_UPDATE).
     */
    public static Map<String, Object> createRoundResultPacket(MatchSession match, String correctColorString,
            double score1, double score2) {

        Map<String, Object> data = new HashMap<>();

        data.put("correctColor", correctColorString);
        data.put("matchId", match.getMatchId());

        // Gói thông tin cho khán giả (dùng P1 làm "you")
        data.put("yourMove", match.getPlayer1().getMove());
        data.put("oppMove", match.getPlayer2().getMove());
        data.put("score", score1); // Điểm của P1
        data.put("yourScore", match.getPlayer1().getMatchScore());
        data.put("opponentScore", match.getPlayer2().getMatchScore());
        data.put("p1Name", match.getPlayer1().getClient().getUser().getUsername());
        data.put("opponent", match.getPlayer2().getClient().getUser().getUsername());

        return data;
    }
}