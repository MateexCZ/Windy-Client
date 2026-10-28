package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;

@ModuleInfo(name = "NightMode", description = "Rockstar-inspired coloured world light", category = Category.Render)
public final class NightMode extends Module {
    private static NightMode instance;
    private final ModeSetting palette = new ModeSetting("Palette", "Blue", "Custom");
    private final ColorSetting custom = new ColorSetting("Custom Color", 0xFF4669C0);
    private final NumberSetting strength = new NumberSetting("Strength", 0.35, 0, 0.8, 0.05);
    public NightMode() { instance = this; addSettings(palette, custom, strength); }
    public static int apply(int argb) {
        if (instance == null || !instance.isToggled()) return argb;
        int color = instance.palette.getCurrMode().equals("Pink") ? 0xFFDD75AE
                : instance.palette.getCurrMode().equals("Custom") ? instance.custom.getColor() : 0xFF658DDA;
        double mix = instance.strength.getVal();
        int r = (int)(((argb >> 16) & 255) * (1 - mix + mix * ((color >> 16) & 255) / 255.0));
        int g = (int)(((argb >> 8) & 255) * (1 - mix + mix * ((color >> 8) & 255) / 255.0));
        int b = (int)((argb & 255) * (1 - mix + mix * (color & 255) / 255.0));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}