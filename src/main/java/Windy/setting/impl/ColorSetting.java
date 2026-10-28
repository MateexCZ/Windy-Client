package Windy.setting.impl;

import Windy.setting.Setting;
import java.awt.Color;

public final class ColorSetting extends Setting {
    private int argb;
    private final int defaultColor;

    public ColorSetting(String name, int color) {
        this.name = name;
        this.argb = color;
        this.defaultColor = color;
    }

    public int getColor() { return argb; }
    public void setColor(int color) { argb = color; }
    public Color asColor() { return new Color(argb, true); }
    public void reset() { argb = defaultColor; }
    public String toHex() { return String.format("#%08X", argb); }

    public void fromHex(String text) {
        String value = text.trim().replace("#", "");
        if (!value.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) {
            throw new IllegalArgumentException("Use RRGGBB or AARRGGBB");
        }
        argb = (int) Long.parseLong(value, 16);
        if (value.length() == 6) argb |= 0xFF000000;
    }
}
