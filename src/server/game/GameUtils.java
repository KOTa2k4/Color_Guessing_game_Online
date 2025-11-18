package server.game;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameUtils {

    private static final Random random = new Random();

    public static Map<String, Object> generateSimilarColorPalette(int roundNumber, int numColors) {
        int delta;
        if (roundNumber <= 1)
            delta = 100;
        else if (roundNumber <= 3)
            delta = 40;
        else
            delta = 20;

        int r_base = random.nextInt(256);
        int g_base = random.nextInt(256);
        int b_base = random.nextInt(256);
        Color correctColor = new Color(r_base, g_base, b_base);
        List<Color> palette = new ArrayList<>();
        palette.add(correctColor);

        for (int i = 0; i < numColors - 1; i++) {
            int r_offset = random.nextInt(delta * 2 + 1) - delta;
            int g_offset = random.nextInt(delta * 2 + 1) - delta;
            int b_offset = random.nextInt(delta * 2 + 1) - delta;
            int new_r = Math.max(0, Math.min(255, r_base + r_offset));
            int new_g = Math.max(0, Math.min(255, g_base + g_offset));
            int new_b = Math.max(0, Math.min(255, b_base + b_offset));
            palette.add(new Color(new_r, new_g, new_b));
        }
        Collections.shuffle(palette);
        Map<String, Object> result = new HashMap<>();
        result.put("correctColor", correctColor);
        result.put("palette", palette);
        return result;
    }
}