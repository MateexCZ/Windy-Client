package Windy.module.impl.ui;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.render.VisualTheme;

@ModuleInfo(name = "Client Colors", description = "Shared colour palette for linked visual effects", category = Category.UI)
public final class Visuals extends Module {
    public static final ColorSetting accent = new ColorSetting("Accent", VisualTheme.DEFAULT_ACCENT);
    public static final ColorSetting secondary = new ColorSetting("Secondary", 0xFFB7A0EC);
    public static final BooleanSetting themeFade = new BooleanSetting("Theme Fade", true);
    public static final NumberSetting fadePeriod = new NumberSetting("Fade Period", 6, 2, 20, .5);

    public Visuals() {
        fadePeriod.addParent(themeFade, setting -> themeFade.isState());
        addSettings(accent, secondary, themeFade, fadePeriod);
    }
}
