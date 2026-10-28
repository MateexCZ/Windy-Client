package Windy.module.impl.combat;

import Windy.Windy;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "AutoClicker", description = "Repeated attack clicks while the attack key is held", category = Category.Combat)
public final class AutoClicker extends Module {
    private final NumberSetting minCps = new NumberSetting("Min CPS", 8, 1, 20, 1);
    private final NumberSetting maxCps = new NumberSetting("Max CPS", 12, 1, 20, 1);
    private final BooleanSetting weaponsOnly = new BooleanSetting("Weapons Only", false);
    private final BooleanSetting allowTools = new BooleanSetting("Allow Tools", false);
    private final BooleanSetting avoidBlocks = new BooleanSetting("Avoid Blocks", true);
    private long nextClick;

    public AutoClicker() {
        addSettings(minCps, maxCps, weaponsOnly, allowTools, avoidBlocks);
        allowTools.addParent(weaponsOnly, setting -> weaponsOnly.isState());
    }

    @Override public void on2D(ScaledResolution unused) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus
                || Windy.INSTANCE.isToggled(KillAura.class) || mc.thePlayer.isUsingItem()) { reset(); return; }
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        boolean held = key < 0 ? Mouse.isButtonDown(key + 100) : key > 0 && Keyboard.isKeyDown(key);
        if (!held) { reset(); return; }
        if (weaponsOnly.isState() && (mc.thePlayer.getHeldItem() == null
                || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)
                && !(allowTools.isState() && mc.thePlayer.getHeldItem().getItem() instanceof ItemTool))) { reset(); return; }
        if (avoidBlocks.isState() && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) { reset(); return; }
        long now = System.nanoTime();
        if (nextClick == 0) { nextClick = now + delayNanos(); return; }
        if (now >= nextClick) {
            mc.windyClickMouse();
            nextClick += delayNanos();
            if (nextClick < now - 50_000_000L) nextClick = now + delayNanos();
        }
    }

    private long delayNanos() {
        double low = Math.max(1, Math.min(minCps.getVal(), maxCps.getVal()));
        double high = Math.max(low, Math.max(minCps.getVal(), maxCps.getVal()));
        double cps = low == high ? low : ThreadLocalRandom.current().nextDouble(low, high);
        return (long)(1_000_000_000.0 / cps);
    }
    private void reset() { nextClick = 0; }
    @Override public void onDisable() { reset(); }
}