package Windy.setting.impl;

import Windy.setting.Setting;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BooleanSetting extends Setting {
    private boolean state;

    public BooleanSetting(String name, boolean state) {
        this.name = name;
        this.state = state;
    }

    private boolean isEnabled() {
        return state;
    }

    public void toggle() {
        setState(!isEnabled());
    }


}
