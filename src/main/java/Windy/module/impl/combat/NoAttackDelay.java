package Windy.module.impl.combat;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import net.minecraft.client.Minecraft;


@ModuleInfo(name = "NoAttackDelay", description = "Removes the client cooldown after a missed attack", category = Category.Combat)
public final class NoAttackDelay extends Module {
    private static volatile boolean active;

    @Override public void onEnable() { active = true; }
    @Override public void onDisable() { active = false; }

    public static boolean shouldReset(Minecraft game) {
        return active && game.inGameHasFocus && game.currentScreen == null
                && game.thePlayer != null && game.theWorld != null;
    }
}
