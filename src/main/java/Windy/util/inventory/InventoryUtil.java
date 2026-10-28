package Windy.util.inventory;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;

public final class InventoryUtil {

    private static final Minecraft MC = Minecraft.getMinecraft();

    private InventoryUtil() {
    }

    public static Item getHeldItem() {
        if (MC.thePlayer == null || MC.thePlayer.getCurrentEquippedItem() == null) return null;
        return MC.thePlayer.getCurrentEquippedItem().getItem();
    }

    public static int getBlockSlot() {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = MC.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                if (isBlockPlaceable(itemBlock.getBlock()))
                    return i;
            }
        }
        return -1;
    }

    public static boolean isBlockPlaceable(final Block block) {
        return (block.isFullBlock() || block == Blocks.glass) &&
                block != Blocks.sand &&
                block != Blocks.gravel &&
                block != Blocks.dispenser &&
                block != Blocks.command_block &&
                block != Blocks.noteblock &&
                block != Blocks.furnace &&
                block != Blocks.crafting_table &&
                block != Blocks.tnt &&
                block != Blocks.dropper &&
                block != Blocks.beacon;
    }

    public static float getSwordStrength(ItemStack stack) {
        if (stack.getItem() instanceof ItemSword) {
            ItemSword sword = (ItemSword) stack.getItem();
            float sharpness = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25F;
            float fireAspect = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 1.5F;
            return sword.getDamageVsEntity() + sharpness + fireAspect;
        }
        return 0;
    }

    public static void click(int slot, int mouseButton, boolean shiftClick) {
        MC.playerController.windowClick(MC.thePlayer.inventoryContainer.windowId, slot, mouseButton, shiftClick ? 1 : 0, MC.thePlayer);
    }

    public static void drop(int slot) {
        MC.playerController.windowClick(0, slot, 1, 4, MC.thePlayer);
    }

    public static void swap(int slot, int hSlot) {
        MC.playerController.windowClick(MC.thePlayer.inventoryContainer.windowId, slot, hSlot, 2, MC.thePlayer);
    }
}
