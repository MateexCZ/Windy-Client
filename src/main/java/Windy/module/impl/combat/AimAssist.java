package Windy.module.impl.combat;

import Windy.Windy;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "AimAssist", description = "Smooth head-level aim toward visible players", category = Category.Combat)
public final class AimAssist extends Module {
    private final NumberSetting range = new NumberSetting("Range", 4.5, 2, 6, 0.1);
    private final NumberSetting fov = new NumberSetting("FOV", 90, 10, 180, 5);
    private final NumberSetting strength = new NumberSetting("Strength", 4, 0.5, 15, 0.5);
    private final NumberSetting smoothing = new NumberSetting("Smoothing", 6, 1, 20, 0.5);
    private final BooleanSetting requireClick = new BooleanSetting("Require Click", true);
    private final BooleanSetting swordOnly = new BooleanSetting("Sword Only", false);
    private EntityPlayer target;
    private long lastFrame;
    private EntityPlayer trackedPlayer;
    private Object trackedWorld;

    public AimAssist() { addSettings(range, fov, strength, smoothing, requireClick, swordOnly); }

    @Override public void on3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || !mc.inGameHasFocus
                || Windy.INSTANCE.isToggled(KillAura.class)) { reset(); return; }
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        if (requireClick.isState() && !(key < 0 ? Mouse.isButtonDown(key + 100) : key > 0 && Keyboard.isKeyDown(key))) {
            reset(); return;
        }
        if (swordOnly.isState() && (mc.thePlayer.getHeldItem() == null
                || !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword))) { reset(); return; }

        if (trackedPlayer != mc.thePlayer || trackedWorld != mc.theWorld) {
            reset();
            trackedPlayer = mc.thePlayer;
            trackedWorld = mc.theWorld;
        }
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 1.0 / 60 : Math.max(0, Math.min(0.05, (now - lastFrame) / 1.0e9));
        lastFrame = now;
        partialTicks = MathHelper.clamp_float(partialTicks, 0, 1);
        EntityPlayer best = null;
        float[] desired = null;
        double halfFov = fov.getVal() / 2;
        double bestScore = Double.POSITIVE_INFINITY;
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer || player.isDead || player.getHealth() <= 0 || player.isInvisible()
                    || mc.thePlayer.getDistanceToEntity(player) > range.getVal()
                    || !mc.thePlayer.canEntityBeSeen(player)) continue;
            float[] aim = rotations(player, partialTicks);
            double angle = Math.abs(MathHelper.wrapAngleTo180_float(aim[0] - mc.thePlayer.rotationYaw));
            if (angle > halfFov) continue;
            double score = angle - (player == target ? 12 : 0);
            if (score < bestScore) { best = player; desired = aim; bestScore = score; }
        }
        target = best;
        if (desired == null) return;
        float oldYaw = mc.thePlayer.rotationYaw;
        float oldPitch = mc.thePlayer.rotationPitch;
        float[] next = smoothRotations(desired, oldYaw, oldPitch, dt,
                mc.thePlayer.moveStrafing, mc.thePlayer.motionY);
        mc.thePlayer.rotationYaw = next[0];
        mc.thePlayer.rotationPitch = next[1];
        mc.thePlayer.prevRotationYaw += next[0] - oldYaw;
        mc.thePlayer.prevRotationPitch = MathHelper.clamp_float(mc.thePlayer.prevRotationPitch
                + next[1] - oldPitch, -90, 90);
    }

    private float[] smoothRotations(float[] desired, float yaw, float pitch, double dt,
                                    float strafe, double motionY) {
        float deltaYaw = MathHelper.wrapAngleTo180_float(desired[0] - yaw);
        float deltaPitch = desired[1] - pitch;
        double yawSmoothing = Math.max(1, smoothing.getVal());
        double pitchSmoothing = yawSmoothing;
        if (strafe < 0 && deltaYaw < 0 || strafe > 0 && deltaYaw > 0)
            yawSmoothing = Math.max(1, yawSmoothing / 2);
        if (motionY > 0 && deltaPitch > 0 || motionY < 0 && deltaPitch < 0)
            pitchSmoothing = Math.max(1, pitchSmoothing / 2);
        double ticks = Math.max(0, Math.min(.05, dt)) * 20 * (5 + strength.getVal() * 1.5) / 11;
        if (ticks == 0) return new float[]{yaw, pitch};
        double yawGain = 1 - Math.pow(1 - 1 / yawSmoothing, ticks);
        double pitchGain = 1 - Math.pow(1 - 1 / pitchSmoothing, ticks);
        float limit = (float)((100 + strength.getVal() * 55) * Math.max(0, Math.min(.05, dt)));
        return new float[]{yaw + MathHelper.clamp_float((float)(deltaYaw * yawGain), -limit, limit),
                MathHelper.clamp_float(pitch + MathHelper.clamp_float((float)(deltaPitch * pitchGain), -limit, limit), -90, 90)};
    }

    private float[] rotations(EntityPlayer player, float partialTicks) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(partialTicks);
        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks - eyes.xCoord;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks
                + player.getEyeHeight() - 0.12 - eyes.yCoord;
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks - eyes.zCoord;
        return new float[]{(float)(Math.toDegrees(Math.atan2(z, x)) - 90),
                (float)-Math.toDegrees(Math.atan2(y, Math.hypot(x, z)))};
    }

    private void reset() { target = null; lastFrame = 0; trackedPlayer = null; trackedWorld = null; }
    @Override public void onEnable() { reset(); }
    @Override public void onDisable() { reset(); }
}