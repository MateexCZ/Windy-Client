package Windy.ui.clickgui.components.setting;

import Windy.font.CustomFont;
import Windy.setting.impl.ColorSetting;
import Windy.ui.modern.ColorPopup;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;

public final class ColorComp extends Comp<ColorSetting> {
    public ColorComp(ColorSetting setting) { super(setting); height = 24; }

    @Override public void draw(int mouseX, int mouseY, float alpha) {
        CustomFont.drawString(CustomFont.light(16), setting.getName(), x + 8, y + 7, VisualTheme.TEXT, alpha);
        RenderUtil.drawRoundedRect(x + width - 28, y + 5, 20, 14, 4, AnimationUtil.multiplyAlpha(setting.getColor(), alpha));
    }

    @Override public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0 || !RenderUtil.contains(mouseX, mouseY, x, y, width, height)) return false;
        ColorPopup.open(setting);
        return true;
    }

    @Override public void mouseReleased(int x, int y, int state) { }
    @Override public void keyTyped(char character, int key) { }
}
