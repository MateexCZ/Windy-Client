package Windy.module.impl.ui;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.ui.clickgui.ClickGUIScreen;
import Windy.ui.clickgui.DropDowns.DropdownScreen;
import Windy.ui.clickgui.rise.RiseScreen;
import net.minecraft.client.gui.GuiScreen;

@ModuleInfo(
        name = "ClickGUI",
        description = "Decide what style you want to use",
        category = Category.UI,
        enabled = true
)

public class ClickGUI extends Module {

    public static ModeSetting modes = new ModeSetting("Modes", "Windy panel", "Dropdown", "Rise");

    public static final NumberSetting dropdownOpacity = new NumberSetting("Dropdown Opacity", .94, .35, 1, .01);

    public static final NumberSetting backgroundOpacity = new NumberSetting("Background Opacity", .35, 0, 1, .01);


    private static DropdownScreen dropdownScreen;


    public static GuiScreen createScreen() {
        String style = modes.getCurrMode();
        if (style != null && style.trim().equalsIgnoreCase("Dropdown")) {
            if (dropdownScreen == null) dropdownScreen = new DropdownScreen();
            return dropdownScreen;
        }
        if (style != null && style.trim().equalsIgnoreCase("Rise")) return new RiseScreen();
        return new ClickGUIScreen();
    }
    public ClickGUI() {
        addSettings(modes, dropdownOpacity, backgroundOpacity);
    }

}
