package Windy.module.impl.ui;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.NumberSetting;


@ModuleInfo(name = "SmoothChat", description = "Gentle incoming message animations", category = Category.UI, enabled = true)
public final class SmoothChat extends Module {
    private static SmoothChat instance;
    private final NumberSetting duration = new NumberSetting("Duration", 150, 60, 350, 10);

    public SmoothChat() {
        instance = this;
        addSettings(duration);
    }

    public static boolean isActive() {
        return instance != null && instance.isToggled();
    }

    public static float durationMillis() {
        return instance == null ? 150 : (float) instance.duration.getVal();
    }
}
