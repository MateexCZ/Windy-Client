package Windy.util.movement;

import Windy.event.impl.MoveInputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

public final class MovementUtils {

    public static final double HEAD_HITTER_MOTION = -0.0784000015258789;
    private static final Minecraft MC = Minecraft.getMinecraft();

    private MovementUtils() {
    }

    public static boolean isMoving() {
        return (MC.thePlayer.movementInput.moveForward != 0F || MC.thePlayer.movementInput.moveStrafe != 0F);
    }

    public static float getMoveYaw() {
        final Vector2f from = new Vector2f((float) MC.thePlayer.lastTickPosX, (float) MC.thePlayer.lastTickPosZ),
                to = new Vector2f((float) MC.thePlayer.posX, (float) MC.thePlayer.posZ),
                diff = new Vector2f(to.getX() - from.getX(), to.getY() - from.getY());

        return (float) Math.toDegrees((Math.atan2(-diff.getX(), diff.getY()) + MathHelper.PI2) % MathHelper.PI2);
    }

    public static double getDirection() {
        float rotationYaw = MC.thePlayer.rotationYaw;

        if (MC.thePlayer.moveForward < 0)
            rotationYaw += 180;

        float forward = 1;

        if (MC.thePlayer.moveForward < 0)
            forward = -0.5F;
        else if (MC.thePlayer.moveForward > 0)
            forward = 0.5F;

        if (MC.thePlayer.moveStrafing > 0)
            rotationYaw -= 90 * forward;
        else if(MC.thePlayer.moveStrafing < 0)
            rotationYaw += 90 * forward;

        return Math.toRadians(rotationYaw);
    }

    public double direction(MoveInputEvent moveInputEvent) {
        float rotationYaw = MC.thePlayer.movementYaw;

        if (moveInputEvent.getForward() < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (moveInputEvent.getForward() < 0) {
            forward = -0.5F;
        } else if (moveInputEvent.getForward() > 0) {
            forward = 0.5F;
        }

        if (moveInputEvent.getStrafe() > 0) {
            rotationYaw -= 70 * forward;
        }

        if (moveInputEvent.getStrafe() < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public double direction(float inputForward, float inputStrafe) {
        float rotationYaw = MC.thePlayer.movementYaw;

        if (inputForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (inputForward < 0) {
            forward = -0.5F;
        } else if (inputForward > 0) {
            forward = 0.5F;
        }

        if (inputStrafe > 0) {
            rotationYaw -= 70 * forward;
        }

        if (inputStrafe < 0) {
            rotationYaw += 70 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;

        float forward = 1F;

        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;

        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;

        return Math.toRadians(rotationYaw);
    }


    public static float getSpeed() {
        return (float) Math.sqrt(MC.thePlayer.motionX * MC.thePlayer.motionX + MC.thePlayer.motionZ * MC.thePlayer.motionZ);
    }

    public static double getAllowedHDistNCP() {
        double hDist = MC.thePlayer.capabilities.getWalkSpeed();
        if (MC.thePlayer.isPotionActive(Potion.moveSlowdown))
            hDist /= 1.0 + 0.05 * (MC.thePlayer.getActivePotionEffect(Potion.moveSlowdown).getAmplifier() + 1);
        if (MC.thePlayer.isPotionActive(Potion.moveSpeed))
            hDist *= 1.0 + 0.05 * (MC.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        return hDist * 4.8;
    }

    public static void setSpeed(final double speed) {
        final double yaw = getDirection();
        if(!isMoving())
            return;

        MC.thePlayer.motionX = -MathHelper.sin((float) yaw) * speed;
        MC.thePlayer.motionZ = MathHelper.cos((float) yaw) * speed;
    }

    public static void setSpeed(final double speed, double strafePercentage) {
        strafePercentage /= 100;
        strafePercentage = Math.min(1, Math.max(0, strafePercentage));

        final double motionX = MC.thePlayer.motionX;
        final double motionZ = MC.thePlayer.motionZ;

        setSpeed(MovementUtils.getSpeed());

        MC.thePlayer.motionX = motionX + (MC.thePlayer.motionX - motionX) * strafePercentage;
        MC.thePlayer.motionZ = motionZ + (MC.thePlayer.motionZ - motionZ) * strafePercentage;
    }

    public static boolean isForwardPressed() {
        if (MC.gameSettings.keyBindForward.isKeyDown() != MC.gameSettings.keyBindBack.isKeyDown())
            return true;
        return MC.gameSettings.keyBindLeft.isKeyDown() != MC.gameSettings.keyBindRight.isKeyDown();
    }

    public static int getForwardValue() {
        int forwardValue = 0;
        if (MC.gameSettings.keyBindForward.isKeyDown()) {
            ++forwardValue;
        }
        if (MC.gameSettings.keyBindBack.isKeyDown()) {
            --forwardValue;
        }
        return forwardValue;
    }

    public static int getLeftValue() {
        int leftValue = 0;
        if (MC.gameSettings.keyBindLeft.isKeyDown()) {
            ++leftValue;
        }
        if (MC.gameSettings.keyBindRight.isKeyDown()) {
            --leftValue;
        }
        return leftValue;
    }


    public static float adjustYaw(float yaw, float forward, float strafe) {
        if (forward < 0.0f) {
            yaw += 180.0f;
        }
        if (strafe != 0.0f) {
            float multiplier = forward == 0.0f ? 1.0f : 0.5f * Math.signum(forward);
            yaw += -90.0f * multiplier * Math.signum(strafe);
        }
        return MathHelper.wrapAngleTo180_float(yaw);
    }

    public static float getDirectionYaw() {
        if (MovementUtils.getSpeed() == 0.0) {
            return MathHelper.wrapAngleTo180_float(MC.thePlayer.rotationYaw);
        }
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(MC.thePlayer.motionZ, MC.thePlayer.motionX)) - 90.0f);
    }

    public static double getBaseMoveSpeed() {
        double baseSpeed = 0.28015;
        if (MovementUtils.getSpeedTime() > 0) {
            baseSpeed = 0.28015 * (1.0 + 0.15 * (double) MovementUtils.getSpeedLevel());
        }
        return baseSpeed;
    }

    public static double getBaseJumpHigh(int speedLevel) {
        double jumpHeight = 0.452;
        if (speedLevel == 1) {
            jumpHeight = 0.49720000000000003;
        } else if (speedLevel >= 2) {
            jumpHeight *= 1.2;
        }
        return jumpHeight;
    }


    public static int getSpeedLevel() {
        int speedLevel = 0;
        if (MC.thePlayer.isPotionActive(Potion.moveSpeed)) {
            speedLevel = (MC.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier() + 1);
        }
        return speedLevel;
    }

    public static int getSpeedTime() {
        if (MC.thePlayer.isPotionActive(Potion.moveSpeed)) {
            return MC.thePlayer.getActivePotionEffect(Potion.moveSpeed).getDuration();
        }
        return 0;
    }

    public static void fixStrafe(float targetYaw) {
        float angle = MathHelper.wrapAngleTo180_float(adjustYaw(MC.thePlayer.rotationYaw, getForwardValue(), getLeftValue()) - targetYaw + 22.5f);
        switch ((int) (angle + 180.0f) / 45 % 8) {
            case 0: {
                MC.thePlayer.movementInput.moveForward = -1.0f;
                MC.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 1: {
                MC.thePlayer.movementInput.moveForward = -1.0f;
                MC.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 2: {
                MC.thePlayer.movementInput.moveForward = 0.0f;
                MC.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 3: {
                MC.thePlayer.movementInput.moveForward = 1.0f;
                MC.thePlayer.movementInput.moveStrafe = 1.0f;
                break;
            }
            case 4: {
                MC.thePlayer.movementInput.moveForward = 1.0f;
                MC.thePlayer.movementInput.moveStrafe = 0.0f;
                break;
            }
            case 5: {
                MC.thePlayer.movementInput.moveForward = 1.0f;
                MC.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 6: {
                MC.thePlayer.movementInput.moveForward = 0.0f;
                MC.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
            case 7: {
                MC.thePlayer.movementInput.moveForward = -1.0f;
                MC.thePlayer.movementInput.moveStrafe = -1.0f;
                break;
            }
        }
        if (MC.thePlayer.movementInput.sneak) {
            MC.thePlayer.movementInput.moveForward *= 0.3f;
            MC.thePlayer.movementInput.moveStrafe *= 0.3f;
        }
    }

}
