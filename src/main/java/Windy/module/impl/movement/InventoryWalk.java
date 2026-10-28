package Windy.module.impl.movement;

import Windy.event.impl.MoveInputEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "InventoryWalk", description = "Use movement keys in inventory and containers", category = Category.Movement)
public final class InventoryWalk extends Module {
    private boolean containerOpen;
    private boolean pressed(KeyBinding binding) {
        int key = binding.getKeyCode();
        return key < 0 ? Mouse.isButtonDown(key + 100) : key > 0 && Keyboard.isKeyDown(key);
    }
    private boolean inContainer() {
        return mc.currentScreen instanceof GuiContainer;
    }
    @Override public void onMoveInput(MoveInputEvent event) {
        if (!inContainer()) return;
        event.setForward((pressed(mc.gameSettings.keyBindForward) ? 1 : 0)
                - (pressed(mc.gameSettings.keyBindBack) ? 1 : 0));
        event.setStrafe((pressed(mc.gameSettings.keyBindLeft) ? 1 : 0)
                - (pressed(mc.gameSettings.keyBindRight) ? 1 : 0));
        event.setJump(pressed(mc.gameSettings.keyBindJump));
        event.setSneak(pressed(mc.gameSettings.keyBindSneak));
    }
    @Override public void onUpdate() {
        if (mc.thePlayer == null) return;
        if (inContainer()) {
            containerOpen = true;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), pressed(mc.gameSettings.keyBindSprint));
        } else if (containerOpen) {
            containerOpen = false;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), pressed(mc.gameSettings.keyBindSprint));
        }
    }
    @Override public void onDisable() { containerOpen = false; }
}