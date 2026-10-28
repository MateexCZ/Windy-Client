package Windy.ui.clickgui.components.setting;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.setting.impl.ModeSetting;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import lombok.Getter;
import lombok.Setter;

public class Mode extends Comp<ModeSetting> {

    @Setter
    @Getter
    private boolean open = false;
    private final Animation dropdownAnim = new Animation(200, Easing.EASE_OUT_CUBIC, 0f);

    public Mode(ModeSetting setting) {
        super(setting);
        this.height = 24;
    }

    @Override
    public void draw(int mouseX, int mouseY, float alpha) {
        dropdownAnim.animateTo(open ? 1f : 0f);
        float dropVal = dropdownAnim.get();

        RenderUtil.drawRoundedRect(x, y, width, 24, 5, AnimationUtil.multiplyAlpha(0xFF1A1C24, alpha));
        CustomFont.LIGHT_18.drawString(setting.getName(), x + 8, y + 7, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        String modeVal = setting.getCurrMode();
        int modeW = (int) CustomFont.getWidth(modeVal) + 16;
        int btnX = x + width - modeW - 6;

        RenderUtil.drawRoundedRect(btnX, y + 4, modeW, 15, 4, AnimationUtil.multiplyAlpha(0xFF1E212B, alpha));
        CustomFont.LIGHT_18.drawString(modeVal, btnX + 5, y + 6, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));
        RenderUtil.drawChevron(btnX + modeW - 6, y + 11, 2.5f, dropVal * 90f, 1.2f, AnimationUtil.multiplyAlpha(0xFFA0A6B8, alpha));

        if (dropVal > 0.01f && setting.getModes() != null) {
            int dropY = y + 26;
            int panelH = (setting.getModes().size() * 18) + 4;

            RenderUtil.drawRoundedRect(x,
                    dropY,
                    width,
                    (int) (panelH * dropVal),
                    5,
                    AnimationUtil.multiplyAlpha(0xFF12141A, alpha * dropVal));

            int optY = dropY + 2;
            for (String option : setting.getModes()) {
                if (optY + 16 > dropY + (panelH * dropVal)) break;

                boolean optHovered = mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= optY && mouseY < optY + 16;
                boolean isSelected = option.equalsIgnoreCase(setting.getCurrMode());

                int optBg = isSelected ? 0xFF3A3F50 : (optHovered ? 0xFF222632 : 0x00000000);
                int optText = isSelected ? 0xFFFFFFFF : (optHovered ? 0xFFFFFFFF : 0xFFA0A6B8);

                if (optBg != 0) {
                    RenderUtil.drawRoundedRect(x + 3, optY, width - 6, 16, 4, AnimationUtil.multiplyAlpha(optBg, alpha * dropVal));
                }
                if (isSelected) {
                    RenderUtil.drawRoundedRect(x + 5, optY + 3, 2, 10, 1, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha * dropVal));
                }

                CustomFont.LIGHT_18.drawString(option,
                        x + (isSelected ? 12 : 8),
                        optY + 4,
                        AnimationUtil.multiplyAlpha(optText, alpha * dropVal));
                optY += 18;
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 24) {
            open = !open;
            return true;
        }

        if (open && setting.getModes() != null) {
            int dropY = y + 26;
            int optY = dropY + 2;
            for (String option : setting.getModes()) {
                if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= optY && mouseY < optY + 16) {
                    setting.setCurrMode(option);
                    open = false;
                    Windy.INSTANCE.getConfigManager().saveConfig();
                    return true;
                }
                optY += 18;
            }
        }
        return false;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int state) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public int getHeight() {
        int baseH = 24;
        if (setting.getModes() != null) {
            baseH += (int) (((setting.getModes().size() * 18) + 6) * dropdownAnim.get());
        }
        return baseH;
    }

}
