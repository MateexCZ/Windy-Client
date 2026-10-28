package Windy.ui.modern;

import Windy.font.CustomFont;
import Windy.module.impl.ui.Visuals;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;


public final class ThemesPanel {
    private ThemesPanel() { }

    public static void draw(float x, float y, float width, float height, float mx, float my, float alpha) {
        CustomFont.drawString(CustomFont.light(20), "Client theme", x, y + 2, VisualTheme.TEXT, alpha);
        CustomFont.drawString(CustomFont.light(16), "One palette for menus, HUD and linked effects", x, y + 20, VisualTheme.MUTED, alpha);
        float cardWidth = (width - 10) / 2;
        for (int i = 0; i < ThemePalette.NAMES.length; i++) {
            float cx = x + i % 2 * (cardWidth + 10), cy = y + 43 + i / 2 * 63;
            boolean selected = ThemePalette.selected() == i;
            RenderUtil.drawRect(cx, cy, cardWidth, 54, AnimationUtil.multiplyAlpha(0xFF191F29, alpha));
            RenderUtil.drawBorder(cx, cy, cardWidth, 54, AnimationUtil.multiplyAlpha(selected ? VisualTheme.accent() : 0xFF343D4A, alpha));
            int primary = AnimationUtil.multiplyAlpha(VisualTheme.preview(ThemePalette.COLORS[i][0], ThemePalette.COLORS[i][1], 0), alpha);
            int secondary = AnimationUtil.multiplyAlpha(VisualTheme.preview(ThemePalette.COLORS[i][0], ThemePalette.COLORS[i][1], .5f),
                    alpha);
            RenderUtil.drawGradient(cx + 7, cy + 7, cardWidth - 14, 19, primary, secondary, primary, secondary);
            CustomFont.drawString(CustomFont.light(18), ThemePalette.NAMES[i], cx + 8, cy + 35, VisualTheme.TEXT, alpha);
            if (selected) CustomFont.drawString(CustomFont.light(16), "Active", cx + cardWidth - 40, cy + 36, VisualTheme.MUTED, alpha);
        }
        float bottom = y + 240;
        CustomFont.drawString(CustomFont.light(18), "Custom colours", x, bottom, VisualTheme.MUTED, alpha);
        swatch("Primary", x, bottom + 18, cardWidth, Visuals.accent.getColor(), alpha);
        swatch("Secondary", x + cardWidth + 10, bottom + 18, cardWidth, Visuals.secondary.getColor(), alpha);
    }

    private static void swatch(String label, float x, float y, float width, int color, float alpha) {
        RenderUtil.drawRect(x, y, width, 24, AnimationUtil.multiplyAlpha(0xFF1C222C, alpha));
        CustomFont.drawString(CustomFont.light(16), label, x + 8, y + 7, VisualTheme.TEXT, alpha);
        RenderUtil.drawRect(x + width - 30, y + 6, 22, 12, AnimationUtil.multiplyAlpha(color, alpha));
    }

    public static boolean click(float x, float y, float width, float height, int mx, int my, int button) {
        if (button != 0) return false;
        float cardWidth = (width - 10) / 2;
        for (int i = 0; i < ThemePalette.NAMES.length; i++) {
            if (RenderUtil.contains(mx, my, x + i % 2 * (cardWidth + 10), y + 43 + i / 2 * 63, cardWidth, 54)) {
                ThemePalette.apply(i);
                return true;
            }
        }
        if (RenderUtil.contains(mx, my, x, y + 258, cardWidth, 24)) { ColorPopup.open(Visuals.accent); return true; }
        if (RenderUtil.contains(mx, my, x + cardWidth + 10, y + 258, cardWidth, 24)) { ColorPopup.open(Visuals.secondary); return true; }
        return false;
    }
}
