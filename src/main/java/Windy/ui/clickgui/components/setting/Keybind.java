package Windy.ui.clickgui.components.setting;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.setting.impl.KeybindSetting;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

public class Keybind extends Comp<KeybindSetting> {

    private boolean binding = false;

    public Keybind(KeybindSetting setting) {
        super(setting);
        this.height = 24;
    }

    @Override
    public void draw(int mouseX, int mouseY, float alpha) {
        RenderUtil.drawRoundedRect(x, y, width, 24, 5, AnimationUtil.multiplyAlpha(0xFF1A1C24, alpha));
        CustomFont.LIGHT_18.drawString(setting.getName(), x + 8, y + 7, AnimationUtil.multiplyAlpha(0xFFFFFFFF, alpha));

        String bindVal = binding ? "..." : (setting.getCode() == Keyboard.KEY_NONE ? "None" : Keyboard.getKeyName(setting.getCode()));

        int btnW = (int) CustomFont.getWidth(bindVal) + 12;
        int btnX = x + width - btnW - 6;

        int btnBg = binding ? 0xFFFFFFFF : 0xFF1E212B;
        int txtColor = binding ? 0xF70B0C0E : 0xFFFFFFFF;

        RenderUtil.drawRoundedRect(btnX, y + 4, btnW, 15, 4, AnimationUtil.multiplyAlpha(btnBg, alpha));
        CustomFont.LIGHT_18.drawString(bindVal, btnX + 5, y + 6, AnimationUtil.multiplyAlpha(txtColor, alpha));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            binding = !binding;
            return true;
        } else if (binding) {
            binding = false;
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (binding) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                setting.setCode(Keyboard.KEY_NONE);
            } else {
                setting.setCode(keyCode);
            }
            binding = false;
            Windy.INSTANCE.getConfigManager().saveConfig();
        }
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int state) {}

    public boolean isBinding() {
        return binding;
    }

    public void setBinding(boolean binding) {
        this.binding = binding;
    }
}
