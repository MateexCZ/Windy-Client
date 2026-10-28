package Windy.ui.clickgui.components.setting;

import Windy.setting.Setting;
import lombok.Getter;

public abstract class Comp<T extends Setting> {
    @Getter
    protected final T setting;
    protected int x;
    protected int y;
    protected int width;
    @Getter
    protected int height;

    public Comp(T setting) {
        this.setting = setting;
    }

    public abstract void draw(int mouseX, int mouseY, float alpha);
    public abstract boolean mouseClicked(int mouseX, int mouseY, int mouseButton);
    public abstract void mouseReleased(int mouseX, int mouseY, int state);
    public abstract void keyTyped(char typedChar, int keyCode);

    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
