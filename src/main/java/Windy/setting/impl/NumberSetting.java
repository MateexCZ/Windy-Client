package Windy.setting.impl;

import Windy.setting.Setting;
import lombok.Getter;
import lombok.Setter;
import static net.minecraft.realms.RealmsMth.clamp;

@Getter
@Setter
public class NumberSetting extends Setting  {
    private double val, minVal, maxVal, defaultVal, increment;

    public NumberSetting(String name, double defaultVal, double minVal, double maxVal, double increment) {
        this.name = name;
        this.val = defaultVal;
        this.defaultVal = defaultVal;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.increment = increment;
    }

    public float getFloat() {
        return (float) val;
    }

    public int getInt() {
        return (int) val;
    }
    public void setVal(double val) {
        this.val = clamp(val, minVal, maxVal);
    }

    private double clamp(double val, double minVal, double maxVal) {
        return Math.min(maxVal, Math.max(minVal, val));
    }

}
