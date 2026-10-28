package Windy.module.impl.combat;

import Windy.Windy;
import Windy.event.impl.MotionEvent;
import Windy.event.impl.MoveEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.animation.AnimationUtil;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil.Easing;
import Windy.util.movement.MovementUtils;
import Windy.util.render.RenderUtil;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

import static org.lwjgl.opengl.GL11.*;

@ModuleInfo(name = "TargetStrafe", description = "Orbits the KillAura target while moving", category = Category.Combat)
public final class TargetStrafe extends Module {
    public static final BooleanSetting space = new BooleanSetting("Require Space", false);
    public static final BooleanSetting auto3rdPerson = new BooleanSetting("Auto 3rd Person", false);
    public static final NumberSetting radius = new NumberSetting("Radius", 1.5, 0.5, 8.0, 0.5);
    public static final BooleanSetting edges = new BooleanSetting("Avoid Edges", true);
    public static final BooleanSetting behind = new BooleanSetting("Stay Behind", false);
    public static final BooleanSetting liquids = new BooleanSetting("Avoid Liquids", true);
    public static final BooleanSetting Controllable = new BooleanSetting("Controllable", true);
    private final BooleanSetting circle = new BooleanSetting("Draw Circle", true);

    private static int direction = 1;
    private final Animation circleAnim = new Animation(250, Easing.EASE_OUT_CUBIC, 0f);
    private boolean wasStrafing;
    private boolean wasBlocked;
    private int previousPerspective;

    public TargetStrafe() {
        addSettings(space, auto3rdPerson, radius, circle, edges, behind, liquids, Controllable);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;
        boolean active = canStrafe();
        boolean thirdPerson = active && auto3rdPerson.isState();
        if (thirdPerson && !wasStrafing) {
            previousPerspective = mc.gameSettings.thirdPersonView;
            if (previousPerspective == 0) mc.gameSettings.thirdPersonView = 1;
        } else if (!thirdPerson && wasStrafing && mc.gameSettings.thirdPersonView == 1) {
            mc.gameSettings.thirdPersonView = previousPerspective;
        }
        wasStrafing = thirdPerson;

        if (!active) {
            wasBlocked = false;
            return;
        }

        if (Controllable.isState()) {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) direction = 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) direction = -1;
        }
        boolean blocked = mc.thePlayer.isCollidedHorizontally || isUnsafe(direction);
        if (blocked && !wasBlocked) direction = -direction;
        wasBlocked = blocked;
    }

    @Override
    public void onMoveEvent(MoveEvent event) {
        strafe(event);
    }

    @Override
    public void onDisable() {
        if (wasStrafing && mc.gameSettings.thirdPersonView == 1) {
            mc.gameSettings.thirdPersonView = previousPerspective;
        }
        wasStrafing = false;
        wasBlocked = false;
    }

    public static boolean canStrafe() {
        Minecraft game = Minecraft.getMinecraft();
        if (game.thePlayer == null || game.theWorld == null || !Windy.INSTANCE.isToggled(TargetStrafe.class)
                || !Windy.INSTANCE.isToggled(KillAura.class) || !MovementUtils.isMoving()) return false;
        if (space.isState() && !Keyboard.isKeyDown(Keyboard.KEY_SPACE)) return false;
        EntityLivingBase target = KillAura.target;
        return target != null && !target.isDead && target.getHealth() > 0
                && game.thePlayer.getDistanceToEntity(target) <= KillAura.range.getVal();
    }

    public static boolean strafe(MoveEvent event) {
        return strafe(event, Math.max(0.22, MovementUtils.getSpeed()));
    }

    public static boolean strafe(MoveEvent event, double speed) {
        if (!canStrafe()) return false;
        EntityLivingBase target = KillAura.target;
        double targetX = target.posX;
        double targetZ = target.posZ;
        if (behind.isState()) {
            double yaw = Math.toRadians(target.rotationYaw);
            targetX += Math.sin(yaw) * radius.getVal();
            targetZ -= Math.cos(yaw) * radius.getVal();
        }
        double dx = targetX - mc.thePlayer.posX;
        double dz = targetZ - mc.thePlayer.posZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 0.001) return false;
        double radial = behind.isState() ? Math.min(1, distance) : Math.max(-0.6, Math.min(0.9, (distance - radius.getVal()) * 0.9));
        double tangent = behind.isState() ? 0 : 1;
        double x = dx / distance * radial - dz / distance * direction * tangent;
        double z = dz / distance * radial + dx / distance * direction * tangent;
        double length = Math.hypot(x, z);
        if (length < 0.001) return false;
        event.setX(x / length * speed);
        event.setZ(z / length * speed);
        return true;
    }

    private boolean isUnsafe(int proposedDirection) {
        if (!edges.isState() && !liquids.isState()) return false;
        EntityLivingBase target = KillAura.target;
        if (target == null) return false;
        double dx = target.posX - mc.thePlayer.posX;
        double dz = target.posZ - mc.thePlayer.posZ;
        double distance = Math.hypot(dx, dz);
        if (distance < 0.001) return false;
        double step = Math.max(0.7, MovementUtils.getSpeed() * 4);
        double probeX = mc.thePlayer.posX - dz / distance * proposedDirection * step;
        double probeZ = mc.thePlayer.posZ + dx / distance * proposedDirection * step;
        if (liquids.isState() && mc.theWorld.getBlockState(new BlockPos(probeX, mc.thePlayer.posY - 1, probeZ)).getBlock() instanceof BlockLiquid) {
            return true;
        }
        AxisAlignedBB next = mc.thePlayer.getEntityBoundingBox().offset(probeX - mc.thePlayer.posX, -1.1, probeZ - mc.thePlayer.posZ);
        return edges.isState() && mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, next).isEmpty();
    }

    @Override
    public void on3D(float ticks) {
        if (!circle.isState() || mc.theWorld == null) return;
        circleAnim.animateTo(canStrafe() ? 1f : 0f);
        float alpha = circleAnim.get();
        EntityLivingBase target = KillAura.target;
        if (target == null || alpha <= 0.01f) return;
        double x = target.lastTickPosX + (target.posX - target.lastTickPosX) * ticks - mc.getRenderManager().viewerPosX;
        double y = target.lastTickPosY + (target.posY - target.lastTickPosY) * ticks - mc.getRenderManager().viewerPosY + 0.05;
        double z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * ticks - mc.getRenderManager().viewerPosZ;
        glPushMatrix();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glLineWidth(2f);
        glBegin(GL_LINE_LOOP);
        for (int i = 0; i < 72; i++) {
            double angle = i * Math.PI / 36;
            int color = AnimationUtil.lerpColor(0xFF00C8FF, 0xFFFF33B5, i / 72f);
            RenderUtil.color(AnimationUtil.multiplyAlpha(color, alpha));
            glVertex3d(x + radius.getVal() * Math.cos(angle), y, z + radius.getVal() * Math.sin(angle));
        }
        glEnd();
        glDepthMask(true);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glColor4f(1, 1, 1, 1);
        glPopMatrix();
    }
}
