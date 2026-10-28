package Windy.util.combat;

import Windy.Windy;
import Windy.module.Module;
import Windy.util.chat.ChatUtil;
import Windy.util.movement.ScaffoldSupport;
import Windy.util.render.ModuleGeometry;
import Windy.util.render.WorldVisuals;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import org.lwjgl.input.*;

public final class AuraSupport {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static float serverYaw;
    
    private AuraSupport() {
    }
    
    public static boolean moduleEnabled(String name) {
        Module module = Windy.INSTANCE.getModuleManager().getModule(name);
        return module != null && module.isToggled();
    }
    
    public static boolean digging() {
        return mc.playerController.getIsHittingBlock();
    }
    
    public static boolean placing() {
        return isUsingItem() && mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock;
    }
    
    public static boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }
    
    public static boolean isHoldingTool() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemTool;
    }
    
    public static boolean isEating() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemFood;
    }
    
    public static boolean isUsingBow() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow;
    }
    
    public static boolean isBlock(ItemStack stack) {
        return ScaffoldSupport.isBlock(stack);
    }
    
    public static boolean isHoldingBlock() {
        return isBlock(mc.thePlayer.getHeldItem());
    }
    
    public static boolean isUsingItem() {
        return mc.gameSettings.keyBindUseItem.isKeyDown();
    }
    
    public static boolean isAttacking() {
        return mc.gameSettings.keyBindAttack.isKeyDown();
    }
    
    public static boolean isKeyDown(int key) {
        return key < 0 ? Mouse.isButtonDown(key + 100) : key > 0 && Keyboard.isKeyDown(key);
    }
    
    public static void setKeyBindState(int key, boolean value) {
        KeyBinding.setKeyBindState(key, value);
    }
    
    public static boolean canMove(double x, double z, double y) {
        return ScaffoldSupport.canMove(x, z, y);
    }
    
    public static boolean isAirAbove() {
        return ScaffoldSupport.isAirAbove();
    }
    
    public static boolean isAirBelow() {
        return ScaffoldSupport.isAirBelow();
    }
    
    public static boolean isEntityLoaded(Entity entity) {
        return mc.theWorld.loadedEntityList.contains(entity);
    }
    
    public static boolean isFriend(EntityPlayer player) {
        return false;
    }
    
    public static boolean isTarget(EntityPlayer player) {
        return !isSameTeam(player);
    }
    
    public static boolean isSameTeam(EntityLivingBase player) {
        return mc.thePlayer.isOnSameTeam(player);
    }
    
    public static boolean hasTeamColor(EntityLivingBase player) {
        return player.getTeam() != null;
    }
    
    public static float getHealthScore(EntityLivingBase player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }
    
    public static boolean isBot(EntityPlayer player) {
        return player.getUniqueID() == null || player.getName().isEmpty() || player.getEntityId() < 0;
    }
    
    public static boolean hasRawUnbreakingEnchant(ItemStack stack) {
        return net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.enchantment.Enchantment.unbreaking.effectId, stack) > 0;
    }
    
    public static void attackEntity(Entity entity) {
        mc.thePlayer.attackTargetEntityWithCurrentItem(entity);
    }
    
    public static int getServerSlot() {
        return mc.playerController.windyGetServerSlot();
    }
    
    public static void setServerSlot(int slot) {
        mc.playerController.windySetServerSlot(slot);
    }
    
    public static void logFormatted(String message) {
        ChatUtil.info(message.replace('&', '\u00a7'));
    }
    
    public static void beginRender() {
        WorldVisuals.begin(false, false);
    }
    
    public static void endRender() {
        WorldVisuals.end();
    }
    
    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        ModuleGeometry.box(entity.getEntityBoundingBox(), new Color(red, green, blue).getRGB(), true);
    }
    
    public static void renderBlock(BlockPos position, int color, boolean outline, boolean shade, float width) {
        ModuleGeometry.box(new AxisAlignedBB(position, position.add(1, 1, 1)), color, true);
    }
    
    public static boolean hasRawUnbreakingEnchant() {
        if (mc.thePlayer == null) {
            return false;
        }
        ItemStack itemStack = mc.thePlayer.getHeldItem();
        if (itemStack == null) {
            return false;
        }
        if (itemStack.hasTagCompound()) {
            NBTTagCompound tag = itemStack.getTagCompound();
            if (tag.hasKey("ExtraAttributes")) {
                NBTTagCompound extra = tag.getCompoundTag("ExtraAttributes");
                if (extra.hasKey("UHCid")) {
                    long id = extra.getLong("UHCid");
                    if (id == 50006L || id == 50009L) {
                        return true;
                    }
                }
            }
            if (tag.hasKey("HideFlags") && itemStack.getItem() instanceof ItemSpade && ((ItemSpade)itemStack.getItem()).getToolMaterial() == Item.ToolMaterial.EMERALD) {
                return true;
            }
        }
        if (itemStack.getItem() instanceof ItemEnchantedBook) {
            return false;
        }
        if (EnchantmentHelper.getEnchantments(itemStack).containsKey(19)) {
            return true;
        }
        return itemStack.getItem() instanceof ItemSword;
    }
    
    public static void renderBlock(BlockPos p, int color, boolean outline, boolean shade) {
        renderBlock(p, color, outline, shade, 1.5F);
    }
}
