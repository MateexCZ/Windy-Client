package Windy.module.impl.player;

import Windy.event.impl.SafeWalkEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;

@ModuleInfo(
        name = "SafeWalk",
        description = "Prevents walking off blocks",
        category = Category.Player
)

public final class SafeWalk extends Module {
    @Override
    public void onSafeWalk(SafeWalkEvent e) {
        if (!mc.isCallingFromMinecraftThread() || mc.thePlayer == null || mc.theWorld == null) return;
        e.setSafeWalk(true);
    }

}
