package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;

@ModuleInfo(
        name = "Fullbright",
        description = "See in a dark",
        category = Category.Render,
        enabled = true
)

public final class Fullbright extends Module {


    @Override
    public void onEnable() {
        mc.gameSettings.gammaSetting = 1000000;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.gameSettings.gammaSetting = 1;
        super.onDisable();
    }
}
