package Windy.setting.impl;

import Windy.setting.Setting;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ModeSetting extends Setting {
    private final List<String> modes;
    private String currMode;
    @Setter
    private int modeIndex;

    public ModeSetting(String name, String... modes) {
        this.name = name;
        this.modes = Arrays.asList(modes);
        this.modeIndex = this.modes.indexOf(modes[0]);
        if(currMode == null) currMode = modes[0];
    }

    public ModeSetting(String name, int initial, String[] modes) {
        this(name, modes);
        if (initial >= 0 && initial < modes.length) {
            this.modeIndex = initial;
            this.currMode = modes[initial];
        }
    }
    public void setCurrMode(String value) {
        if (value == null) return;
        String normalized = value.equalsIgnoreCase("GODBIRGDE") ? "GODBRIDGE" : value;
        for (int index = 0; index < modes.size(); index++) {
            if (modes.get(index).equalsIgnoreCase(normalized)) {
                currMode = modes.get(index);
                modeIndex = index;
                return;
            }
        }
    }
    public void cycleForward() {
        modeIndex++;
        if(modeIndex > modes.size() - 1) modeIndex = 0;
        currMode = modes.get(modeIndex);
    }

    public void cycleBack() {
        modeIndex--;
        if(modeIndex < 0) modeIndex = modes.size() - 1;
        currMode = modes.get(modeIndex);
    }

}
