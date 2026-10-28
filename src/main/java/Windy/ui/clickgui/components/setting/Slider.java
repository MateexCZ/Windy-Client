package Windy.ui.clickgui.components.setting;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.setting.impl.NumberSetting;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import org.lwjgl.input.Mouse;

public class Slider extends Comp<NumberSetting> {

    private boolean dragging = false;

    public Slider(NumberSetting setting) {
        super(setting);
        this.height = 24;
    }

    @Override
    public void draw(int mouseX, int mouseY, float alpha) {
        if (dragging && Mouse.isButtonDown(0)) {
            updateValue(mouseX);
        } else {
            dragging = false;
        }

        RenderUtil.drawRoundedRect(x, y, width, 24, 5, AnimationUtil.multiplyAlpha(0xFF1A1C24, alpha));

        String valStr = setting.getName() + ": " + Math.round(setting.getVal() * 100.0) / 100.0;
        CustomFont.LIGHT_18.drawString(valStr, x + 8, y + 7, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        int sliderW = 60;
        int sliderX = x + width - sliderW - 8;
        int sliderY = y + 10;

        RenderUtil.drawRoundedRect(sliderX, sliderY, sliderW, 3, 1, AnimationUtil.multiplyAlpha(0xFF1E212B, alpha));

        double percent = (setting.getVal() - setting.getMinVal()) / (setting.getMaxVal() - setting.getMinVal());
        int fillW = (int) (sliderW * percent);
        if (fillW > 0) {
            RenderUtil.drawRoundedRect(sliderX, sliderY, fillW, 3, 1, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));
        }
        RenderUtil.drawCircle(sliderX + fillW, sliderY + 1.5f, 3.5f, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            dragging = true;
            updateValue(mouseX);
            Windy.INSTANCE.getConfigManager().saveConfig();
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (dragging) {
            dragging = false;
            Windy.INSTANCE.getConfigManager().saveConfig();
        }
    }

    private void updateValue(int mouseX) {
        int sliderW = 60;
        int sliderX = x + width - sliderW - 8;

        double diff = Math.min(sliderW, Math.max(0, mouseX - sliderX));
        double percent = diff / sliderW;

        double newVal = setting.getMinVal() + percent * (setting.getMaxVal() - setting.getMinVal());

        if (setting.getIncrement() > 0) {
            newVal = Math.round(newVal / setting.getIncrement()) * setting.getIncrement();
        }

        setting.setVal(newVal);
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}
}
