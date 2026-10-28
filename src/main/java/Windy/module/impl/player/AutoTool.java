package Windy.module.impl.player;

import Windy.event.impl.MotionEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.module.impl.combat.KillAura;
import Windy.setting.impl.BooleanSetting;
import Windy.util.inventory.InventoryUtil;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;

@ModuleInfo(
        name = "AutoTool",
        description = "switches to the best tool",
        category = Category.Player
)
public class AutoTool extends Module {

    private final BooleanSetting autoSword = new BooleanSetting("AutoSword", true);

    public AutoTool() {
        addSettings(autoSword);
    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        if (e.isPre()) {
            if (KillAura.target != null || (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null)) {
                if (autoSword.isState()) {
                    switchSword();
                }
                return;
            }

            if (mc.playerController.getIsHittingBlock() && mc.objectMouseOver != null) {
                BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                if (blockPos != null) {
                    Block block = mc.theWorld.getBlockState(blockPos).getBlock();
                    updateItem(block);
                }
            }
        }
    }

    private void updateItem(Block block) {
        float bestSpeed = 1.0F;
        int bestItem = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) {
                continue;
            }
            float speed = itemStack.getStrVsBlock(block);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestItem = i;
            }
        }

        if (bestItem != -1 && mc.thePlayer.inventory.currentItem != bestItem) {
            mc.thePlayer.inventory.currentItem = bestItem;
        }
    }

    private void switchSword() {
        float bestDamage = 0F;
        int bestItem = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack is = mc.thePlayer.inventory.mainInventory[i];
            if (is != null && is.getItem() instanceof ItemSword) {
                float damage = InventoryUtil.getSwordStrength(is);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestItem = i;
                }
            }
        }

        if (bestItem != -1 && mc.thePlayer.inventory.currentItem != bestItem) {
            mc.thePlayer.inventory.currentItem = bestItem;
        }
    }
}