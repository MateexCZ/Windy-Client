package Windy.module.impl.player;

import Windy.event.impl.MotionEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;

@ModuleInfo(
        name = "FastPlace",
        description = "Places blocks faster",
        category = Category.Player
)

public final class FastPlace extends Module {

    private final NumberSetting ticks = new NumberSetting("Ticks", 0, 0, 4, 1);
    private final BooleanSetting blocks = new BooleanSetting("Blocks", true);
    private final BooleanSetting projectiles = new BooleanSetting("Projectiles", true);

    public FastPlace() {
        addSettings(ticks, blocks, projectiles);
    }
    @Override
    public void onMotionEvent(MotionEvent event) {
        if (canFastPlace()) {
            mc.rightClickDelayTimer = (int) Math.min(mc.rightClickDelayTimer, ticks.getVal());
        }
    }

    @Override
    public void onDisable() {
        mc.rightClickDelayTimer = 4;
        super.onDisable();
    }

    private boolean canFastPlace() {
        if (mc.thePlayer == null || mc.thePlayer.getCurrentEquippedItem() == null || mc.thePlayer.getCurrentEquippedItem().getItem() == null)
            return false;
        Item heldItem = mc.thePlayer.getCurrentEquippedItem().getItem();
        return (blocks.isState() && heldItem instanceof ItemBlock) || (projectiles.isState() && (heldItem instanceof ItemSnowball || heldItem instanceof ItemEgg));
    }


}
