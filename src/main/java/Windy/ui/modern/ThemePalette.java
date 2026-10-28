package Windy.ui.modern;

import Windy.Windy;
import Windy.module.impl.ui.Visuals;


public final class ThemePalette {
    public static final String[] NAMES = {"Windy", "Ocean", "Mint", "Sunset", "Rose", "Silver"};
    public static final int[][] COLORS = {
            {0xFF99B8F5, 0xFFB7A0EC}, {0xFF6AAEFF, 0xFF81D5EA},
            {0xFF6ED5B5, 0xFFA8E5C2}, {0xFFF29E7E, 0xFFEAC58F},
            {0xFFE59BB9, 0xFFBC9AE8}, {0xFFBBC6D3, 0xFFE0E5ED}
    };
    private ThemePalette() { }

    public static void apply(int index) {
        if (index >= 0 && index < COLORS.length) apply(COLORS[index][0], COLORS[index][1]);
    }

    public static void apply(int primary, int secondary) {
        Visuals.accent.setColor(primary);
        Visuals.secondary.setColor(secondary);
        if (Windy.INSTANCE.getConfigManager() != null) Windy.INSTANCE.getConfigManager().saveConfig();
    }

    public static int selected() {
        for (int i = 0; i < COLORS.length; i++) {
            if (Visuals.accent.getColor() == COLORS[i][0] && Visuals.secondary.getColor() == COLORS[i][1]) return i;
        }
        return -1;
    }
}
