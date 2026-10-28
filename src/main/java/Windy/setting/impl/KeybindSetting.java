package Windy.setting.impl;

import Windy.setting.Setting;
import lombok.Setter;
import org.lwjgl.input.Keyboard;

@Setter
public class KeybindSetting extends Setting {

    private int code;

    public KeybindSetting(int code) {
        this.name = "Keybind";
        this.code = code;
    }

    public int getCode() {
        return code == -1 ? Keyboard.KEY_NONE : code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
