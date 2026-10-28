package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import net.minecraft.util.Vec3;

@ModuleInfo(name = "SkyColor", description = "Blue or pink night sky tint", category = Category.Render)
public final class SkyColor extends Module {
    private static SkyColor instance;
    private final ModeSetting palette = new ModeSetting("Palette", "Neon Blue", "Neon Pink", "Custom");
    private final ColorSetting custom = new ColorSetting("Custom Color", 0xFF061C56);
    private final NumberSetting strength = new NumberSetting("Strength", 0.8, 0, 1, 0.05);
    public SkyColor() { instance = this; addSettings(palette, custom, strength); }
    public static Vec3 apply(Vec3 vanilla) {
        if (instance == null || !instance.isToggled()) return vanilla;
        int color = instance.palette.getCurrMode().equals("Neon Pink") ? 0xFF682249
                : instance.palette.getCurrMode().equals("Custom") ? instance.custom.getColor() : 0xFF062C74;
        double mix = instance.strength.getVal();
        return new Vec3(vanilla.xCoord * (1 - mix) + ((color >> 16) & 255) / 255.0 * mix,
                vanilla.yCoord * (1 - mix) + ((color >> 8) & 255) / 255.0 * mix,
                vanilla.zCoord * (1 - mix) + (color & 255) / 255.0 * mix);
    }
}