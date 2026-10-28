package Windy.module.impl.player;

import Windy.Windy;
import Windy.event.impl.MotionEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import java.util.Locale;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

@ModuleInfo(name = "ChestStealer", description = "Steals content of the chest", category = Category.Player)
public final class ChestStealer extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 100, 5, 1000, 5);
    private final BooleanSetting ignoreTrash = new BooleanSetting("Ignore Trash", true);
    private final BooleanSetting guiDetection = new BooleanSetting("Gui Detection", true);
    private ContainerChest activeChest;
    private long nextClickNanos;
    private boolean finished;

    public ChestStealer() { addSettings(delay, ignoreTrash, guiDetection); }

    @Override public void onMotionEvent(MotionEvent event) {
        if (!event.isPre()) return;
        if (mc.thePlayer == null || mc.theWorld == null || mc.playerController == null
                || !(mc.currentScreen instanceof GuiChest)
                || !(mc.thePlayer.openContainer instanceof ContainerChest)) { reset(); return; }
        ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
        IInventory inventory = chest.getLowerChestInventory();
        if (guiDetection.isState() && !looksLikeChest(inventory)) { reset(); return; }
        long now = System.nanoTime();
        if (activeChest != chest) {
            activeChest = chest;
            finished = false;
            nextClickNanos = now + delayNanos();
            return;
        }
        if (now < nextClickNanos) return;
        InvManager manager = Windy.INSTANCE.getModuleManager() == null ? null
                : (InvManager) Windy.INSTANCE.getModuleManager().getModule(InvManager.class);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.stackSize <= 0 || !canFit(stack)) continue;
            if (ignoreTrash.isState() && manager != null && manager.isBadItem(stack, slot, true)) continue;
            mc.playerController.windowClick(chest.windowId, slot, 0, 1, mc.thePlayer);
            nextClickNanos = now + delayNanos();
            return;
        }
        finished = true;
        reset();
        mc.thePlayer.closeScreen();
    }

    private boolean looksLikeChest(IInventory inventory) {
        if (!inventory.hasCustomName()) return true;
        String title = inventory.getDisplayName().getUnformattedText().toLowerCase(Locale.ROOT);
        return title.contains("chest") || title.contains("truh")
                || title.equals(I18n.format("container.chest").toLowerCase(Locale.ROOT))
                || title.equals(I18n.format("container.chestDouble").toLowerCase(Locale.ROOT));
    }
    private boolean canFit(ItemStack stack) {
        for (ItemStack current : mc.thePlayer.inventory.mainInventory) {
            if (current == null) return true;
            if (ItemStack.areItemsEqual(current, stack) && ItemStack.areItemStackTagsEqual(current, stack)
                    && current.stackSize < Math.min(current.getMaxStackSize(), mc.thePlayer.inventory.getInventoryStackLimit())) return true;
        }
        return false;
    }
    private long delayNanos() { return (long) (delay.getVal() * 1_000_000L); }
    private void reset() { activeChest = null; nextClickNanos = 0; }
    @Override public void onEnable() { reset(); finished = false; }
    @Override public void onDisable() { reset(); finished = false; }
    public boolean isFinished() { return finished; }
}
