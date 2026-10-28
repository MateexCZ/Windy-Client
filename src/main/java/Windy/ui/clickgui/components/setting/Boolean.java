package Windy.ui.clickgui.components.setting;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.setting.impl.BooleanSetting;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;

public class Boolean extends Comp<BooleanSetting> {

    private final Animation toggleAnim;

    public Boolean(BooleanSetting setting) {
        super(setting);
        this.height = 24;
        this.toggleAnim = new Animation(150, Easing.EASE_OUT_CUBIC, setting.isState() ? 1f : 0f);
    }

    @Override
    public void draw(int mouseX, int mouseY, float alpha) {
        RenderUtil.drawRoundedRect(x, y, width, 24, 5, AnimationUtil.multiplyAlpha(0xFF1A1C24, alpha));
        CustomFont.LIGHT_18.drawString(setting.getName(), x + 8, y + 7, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        int boxX = x + width - 26;
        int boxY = y + 6;

        toggleAnim.animateTo(setting.isState() ? 1f : 0f);
        float tVal = toggleAnim.get();

        int trackBg = AnimationUtil.lerpColor(0xFF2A2E3B, 0xFF3A3F50, tVal);
        RenderUtil.drawRoundedRect(boxX, boxY, 20, 11, 5, AnimationUtil.multiplyAlpha(trackBg, alpha));

        float knobX = boxX + 2 + (9 * tVal);
        int knobColor = AnimationUtil.lerpColor(0xFFA0A6B8, 0xFFFFFFFF, tVal);
        RenderUtil.drawCircle(knobX + 3.5f, boxY + 5.5f, 3.5f, AnimationUtil.multiplyAlpha(knobColor, alpha));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            setting.toggle();
            Windy.INSTANCE.getConfigManager().saveConfig();
            return true;
        }
        return false;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int state) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}
}
