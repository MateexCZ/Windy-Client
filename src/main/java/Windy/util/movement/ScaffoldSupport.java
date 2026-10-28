package Windy.util.movement;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;


public class ScaffoldSupport {
    private static final Minecraft mc = Minecraft.getMinecraft();
    protected ScaffoldSupport() { }

    public static boolean isReplaceable(BlockPos position) {
        return mc.theWorld.getBlockState(position).getBlock().isReplaceable(mc.theWorld, position);
    }
    public static boolean isInteractable(BlockPos position) {
        return isInteractable(mc.theWorld.getBlockState(position).getBlock());
    }
    public static boolean isInteractable(Block block) {
        return block instanceof net.minecraft.block.BlockContainer ||
                block instanceof net.minecraft.block.BlockWorkbench ||
                block instanceof net.minecraft.block.BlockDoor ||
                block instanceof net.minecraft.block.BlockBed ||
                block instanceof net.minecraft.block.BlockFenceGate;
    }
    public static boolean isSolid(Block block) { return block.isFullCube(); }
    public static boolean isBlock(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) return false;
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        return isSolid(block) && !isInteractable(block);
    }
    public static boolean isHoldingBlock() { return isBlock(mc.thePlayer.getHeldItem()); }
    public static Vec3 getClickVec(BlockPos position, EnumFacing face) {
        return new Vec3(position).addVector(.5 + face.getFrontOffsetX() * .5,
                .5 + face.getFrontOffsetY() * .5, .5 + face.getFrontOffsetZ() * .5);
    }
    public static Vec3 getHitVec(BlockPos position, EnumFacing face, float yaw, float pitch) {
        MovingObjectPosition hit = rayTrace(yaw, pitch, mc.playerController.getBlockReachDistance(), 1);
        return hit != null && position.equals(hit.getBlockPos()) && hit.sideHit == face
                ? hit.hitVec : getClickVec(position, face);
    }
    public static float wrapAngleDiff(float angle, float reference) {
        return reference + MathHelper.wrapAngleTo180_float(angle - reference);
    }
    public static float clampAngle(float angle, float maximum) {
        return MathHelper.clamp_float(angle, -maximum, maximum);
    }
    public static float quantizeAngle(float angle) {
        float sensitivity = mc.gameSettings.mouseSensitivity * .6f + .2f;
        float quantum = sensitivity * sensitivity * sensitivity * 1.2f;
        return Math.round(angle / quantum) * quantum;
    }
    public static float[] getRotationsTo(double x, double y, double z, float yaw, float pitch) {
        return new float[] {wrapAngleDiff((float) Math.toDegrees(Math.atan2(z, x)) - 90, yaw),
                MathHelper.clamp_float((float) -Math.toDegrees(Math.atan2(y, Math.hypot(x, z))), -90, 90)};
    }
    public static MovingObjectPosition rayTrace(float yaw, float pitch, double reach, float partialTicks) {
        Vec3 eye = mc.thePlayer.getPositionEyes(partialTicks);
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        Vec3 end = eye.addVector(-Math.sin(y) * Math.cos(p) * reach, -Math.sin(p) * reach,
                Math.cos(y) * Math.cos(p) * reach);
        return mc.theWorld.rayTraceBlocks(eye, end, false, false, true);
    }
    public static float getForwardValue() {
        return (mc.gameSettings.keyBindForward.isKeyDown() ? 1 : 0)
                - (mc.gameSettings.keyBindBack.isKeyDown() ? 1 : 0);
    }
    public static float getLeftValue() {
        return (mc.gameSettings.keyBindLeft.isKeyDown() ? 1 : 0)
                - (mc.gameSettings.keyBindRight.isKeyDown() ? 1 : 0);
    }
    public static boolean isForwardPressed() { return getForwardValue() != 0 || getLeftValue() != 0; }
    public static float adjustYaw(float yaw, float forward, float strafe) {
        if (forward < 0) yaw += 180;
        float multiplier = forward < 0 ? -.5f : forward > 0 ? .5f : 1;
        if (strafe > 0) yaw -= 90 * multiplier;
        if (strafe < 0) yaw += 90 * multiplier;
        return yaw;
    }
    public static float getMoveYaw() {
        return adjustYaw(mc.thePlayer.rotationYaw, mc.thePlayer.movementInput.moveForward,
                mc.thePlayer.movementInput.moveStrafe);
    }
    public static double getSpeed() { return Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ); }
    public static int getSpeedLevel() {
        return mc.thePlayer.isPotionActive(Potion.moveSpeed)
                ? mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1 : 0;
    }
    public static void setSpeed(double speed) { setSpeed(speed, getMoveYaw()); }
    public static void setSpeed(double speed, float yaw) {
        mc.thePlayer.motionX = -Math.sin(Math.toRadians(yaw)) * speed;
        mc.thePlayer.motionZ = Math.cos(Math.toRadians(yaw)) * speed;
    }
    public static void fixStrafe(float yaw) { net.minecraft.entity.player.EntityPlayer.movementYaw = yaw; }

    public static boolean hasSupportBelow() {
        return mc.thePlayer != null && mc.theWorld != null && !mc.theWorld.getCollidingBoundingBoxes(
                mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, -1, 0)).isEmpty();
    }

    public static boolean hasHeadroom() {
        return mc.thePlayer != null && mc.theWorld != null && mc.theWorld.getCollidingBoundingBoxes(
                mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0, 1, 0)).isEmpty();
    }


    public static boolean isAirBelow() { return hasSupportBelow(); }
    public static boolean isAirAbove() { return !hasHeadroom(); }
    public static boolean isUsingItem() { return mc.gameSettings.keyBindUseItem.isKeyDown(); }
    public static boolean canMove(double x, double z, double y) {
        return mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer,
                mc.thePlayer.getEntityBoundingBox().offset(x, y, z)).isEmpty();
    }
    public static double nextDouble(double low, double high) { return low + ThreadLocalRandom.current().nextDouble() * (high - low); }
    public static float nextFloat(float low, float high) { return low + ThreadLocalRandom.current().nextFloat() * (high - low); }
    public static void sendPacket(Packet packet) { mc.thePlayer.sendQueue.addToSendQueue(packet); }
    public static long nextLong(long low, long high) {
        return low >= high ? low : java.util.concurrent.ThreadLocalRandom.current().nextLong(low, high + 1);
    }
}
