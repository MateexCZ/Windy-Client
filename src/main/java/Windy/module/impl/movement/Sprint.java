package Windy.module.impl.movement;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;

@ModuleInfo(
        name = "Sprint",
        description = "Automatically sprints for you",
        category = Category.Movement,
        enabled = true
)

public class Sprint extends Module {
    @Override
    public void onDisable() {
        int key = mc.gameSettings.keyBindSprint.getKeyCode();
        boolean pressed = key < 0
                ? org.lwjgl.input.Mouse.isCreated() && org.lwjgl.input.Mouse.isButtonDown(key + 100)
                : key > 0 && org.lwjgl.input.Keyboard.isCreated() && org.lwjgl.input.Keyboard.isKeyDown(key);
        mc.gameSettings.keyBindSprint.setPressed(pressed);
    }

    @Override
    public void onUpdate() {
        mc.gameSettings.keyBindSprint.setPressed(true);
    }
}
