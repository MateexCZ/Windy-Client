package Windy.util.rotation;

import Windy.util.math.RandomUtil;
import Windy.util.movement.ScaffoldUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final FontRenderer fr = mc.fontRendererObj;

        public static void setRotations(float yaw, float pitch) {
            mc.thePlayer.rotationYawHead = mc.thePlayer.renderYawOffset = yaw;
            mc.thePlayer.rotationPitch = pitch;
        }

        public static float clampRotation() {
            float rotationYaw = Minecraft.getMinecraft().thePlayer.rotationYaw;
            float n = 1.0f;
            if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward < 0.0f) {
                rotationYaw += 180.0f;
                n = -0.5f;
            }
            else if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward > 0.0f) {
                n = 0.5f;
            }
            if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe > 0.0f) {
                rotationYaw -= 90.0f * n;
            }
            if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe < 0.0f) {
                rotationYaw += 90.0f * n;
            }
            return rotationYaw * 0.017453292f;
        }

        public static float getSensitivityMultiplier() {
            float SENSITIVITY = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
            return (SENSITIVITY * SENSITIVITY * SENSITIVITY * 8.0F) * 0.15F;
        }

        public static float smoothRotation(float from, float to, float speed) {
            float f = MathHelper.wrapAngleTo180_float(to - from);

            if (f > speed) {
                f = speed;
            }

            if (f < -speed) {
                f = -speed;
            }

            return from + f;
        }

        /*
         * Sets the player's head rotations to the given yaw and pitch (visual-only).
         */
        public static void setRotations(float[] rotations) {
            setRotations(rotations[0], rotations[1]);
        }

        public static float[] getFacingRotations(ScaffoldUtils.BlockCache blockCache) {
            double d1 = blockCache.position.getX() + 0.5D - mc.thePlayer.posX + blockCache.facing.getFrontOffsetX() / 2.0D;
            double d2 = blockCache.position.getZ() + 0.5D - mc.thePlayer.posZ + blockCache.facing.getFrontOffsetZ() / 2.0D;
            double d3 = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - (blockCache.position.getY());
            double d4 = MathHelper.sqrt_double(d1 * d1 + d2 * d2);
            float f1 = (float) (Math.atan2(d2, d1) * 180.0D / Math.PI) - 90.0F;
            float f2 = (float) (Math.atan2(d3, d4) * 180.0D / Math.PI);
            if (f1 < 0.0F) {
                f1 += 360.0F;
            }
            return new float[]{f1, f2};
        }

        public static float[] getRotationsNeeded(final Entity entity) {
            if (entity == null) {
                return null;
            }
            Minecraft mc = Minecraft.getMinecraft();
            final double xSize = entity.posX - mc.thePlayer.posX;
            final double ySize = entity.posY + entity.getEyeHeight() / 2 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
            final double zSize = entity.posZ - mc.thePlayer.posZ;
            final double theta = MathHelper.sqrt_double(xSize * xSize + zSize * zSize);
            final float yaw = (float) (Math.atan2(zSize, xSize) * 180 / Math.PI) - 90;
            final float pitch = (float) (-(Math.atan2(ySize, theta) * 180 / Math.PI));
            return new float[]{(mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw)) % 360, (mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)) % 360.0f};
        }

        public static float[] getFacingRotations2(final int paramInt1, final double d, final int paramInt3) {
            final EntitySnowball localEntityPig = new EntitySnowball(Minecraft.getMinecraft().theWorld);
            localEntityPig.posX = paramInt1 + 0.5;
            localEntityPig.posY = d + 0.5;
            localEntityPig.posZ = paramInt3 + 0.5;
            return getRotationsNeeded(localEntityPig);
        }

        public static float getYaw(Vec3 to) {
            float x = (float) (to.xCoord - mc.thePlayer.posX);
            float z = (float) (to.zCoord - mc.thePlayer.posZ);
            float var1 = (float) (StrictMath.atan2(z, x) * 180.0D / StrictMath.PI) - 90.0F;
            float rotationYaw = mc.thePlayer.rotationYaw;
            return rotationYaw + MathHelper.wrapAngleTo180_float(var1 - rotationYaw);
        }

        public static Vec3 getVecRotations(float yaw, float pitch) {
            double d = Math.cos(Math.toRadians(-yaw) - Math.PI);
            double d1 = Math.sin(Math.toRadians(-yaw) - Math.PI);
            double d2 = -Math.cos(Math.toRadians(-pitch));
            double d3 = Math.sin(Math.toRadians(-pitch));
            return new Vec3(d1 * d2, d3, d * d2);
        }

        public static float[] getRotations(double posX, double posY, double posZ) {
            double x = posX - mc.thePlayer.posX, z = posZ - mc.thePlayer.posZ, y = posY + mc.thePlayer.getEyeHeight() - mc.thePlayer.posY;
            double d3 = MathHelper.sqrt_double(x * x + z * z);
            float yaw = (float) (MathHelper.atan2(z, x) * 180.0D / Math.PI) - 90.0F;
            float pitch = (float) (-(MathHelper.atan2(y, d3) * 180.0D / Math.PI));
            return new float[]{yaw, pitch};
        }

    public static float[] getFixedRotation(final float[] rotations, final float[] lastRotations) {
        final Minecraft mc = Minecraft.getMinecraft();

        final float yaw = rotations[0];
        final float pitch = rotations[1];

        final float lastYaw = lastRotations[0];
        final float lastPitch = lastRotations[1];

        final float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        final float gcd = f * f * f * 1.2F;

        final float deltaYaw = yaw - lastYaw;
        final float deltaPitch = pitch - lastPitch;

        final float fixedDeltaYaw = deltaYaw - (deltaYaw % gcd);
        final float fixedDeltaPitch = deltaPitch - (deltaPitch % gcd);

        final float fixedYaw = lastYaw + fixedDeltaYaw;
        final float fixedPitch = lastPitch + fixedDeltaPitch;

        return new float[]{fixedYaw, fixedPitch};
    }

    public static float wrapAngleDiff(float angle, float target) {
        return target + MathHelper.wrapAngleTo180_float(angle - target);
    }

    public static float clampAngle(float angle, float maxAngle) {
        maxAngle = Math.max(0.0f, Math.min(180.0f, maxAngle));
        if (angle > maxAngle) {
            angle = maxAngle;
        } else if (angle < -maxAngle) {
            angle = -maxAngle;
        }
        return angle;
    }

    public static float smoothAngle(float angle, float smoothFactor) {
        return angle * (0.5f + 0.5f * (1.0f - Math.max(0.0f, Math.min(1.0f, smoothFactor + RandomUtil.nextFloat(-0.1f, 0.1f)))));
    }

    public static float quantizeAngle(float angle) {
        return (float) ((double) angle - (double) angle % (double) 0.0096f);
    }

    public static float[] getRotationsToBox(AxisAlignedBB boundingBox, float yaw, float pitch, float maxAngle, float smoothFactor) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        double minTargetY = boundingBox.minY + 0.05 * (boundingBox.maxY - boundingBox.minY);
        double maxTargetY = boundingBox.minY + 0.75 * (boundingBox.maxY - boundingBox.minY);
        double deltaX = (boundingBox.minX + boundingBox.maxX) / 2.0 - eyePos.xCoord;
        double deltaY = eyePos.yCoord >= maxTargetY ? maxTargetY - eyePos.yCoord : (eyePos.yCoord <= minTargetY ? minTargetY - eyePos.yCoord : 0.0);
        double deltaZ = (boundingBox.minZ + boundingBox.maxZ) / 2.0 - eyePos.zCoord;
        return getRotations(deltaX, deltaY, deltaZ, yaw, pitch, maxAngle, smoothFactor);
    }

    public static float[] getRotationsTo(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch) {
        return getRotations(targetX, targetY, targetZ, currentYaw, currentPitch, 180.0f, 0.0f);
    }

    public static float[] getRotations(double targetX, double targetY, double targetZ, float currentYaw, float currentPitch, float maxAngle, float smoothFactor) {
        double horizontalDistance = Math.sqrt(targetX * targetX + targetZ * targetZ);
        float yawDelta = MathHelper.wrapAngleTo180_float((float) (Math.atan2(targetZ, targetX) * 180.0 / Math.PI) - 90.0f - currentYaw);
        float pitchDelta = MathHelper.wrapAngleTo180_float((float) (-Math.atan2(targetY, horizontalDistance) * 180.0 / Math.PI) - currentPitch);
        yawDelta = Math.abs(yawDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(yawDelta, maxAngle), smoothFactor);
        pitchDelta = Math.abs(pitchDelta) <= 1.0f ? 0.0f : smoothAngle(clampAngle(pitchDelta, maxAngle), smoothFactor);
        return new float[]{quantizeAngle(currentYaw + yawDelta), quantizeAngle(currentPitch + pitchDelta)};
    }

    public static Vec3 clampVecToBox(Vec3 vector, AxisAlignedBB boundingBox) {
        double[] coords = new double[]{vector.xCoord, vector.yCoord, vector.zCoord};
        double[] minCoords = new double[]{boundingBox.minX, boundingBox.minY, boundingBox.minZ};
        double[] maxCoords = new double[]{boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ};
        for (int i = 0; i < 3; ++i) {
            if (coords[i] > maxCoords[i]) {
                coords[i] = maxCoords[i];
                continue;
            }
            if (!(coords[i] < minCoords[i])) continue;
            coords[i] = minCoords[i];
        }
        return new Vec3(coords[0], coords[1], coords[2]);
    }

    public static double distanceToEntity(Entity entity) {
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        return distanceToBox(boundingBox);
    }

    public static double distanceToBox(Entity entity, Vec3 point) {
        float borderSize = entity.getCollisionBorderSize();
        return clampVecToBox(entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize), point);
    }

    public static double distanceToBox(AxisAlignedBB boundingBox) {
        return clampVecToBox(boundingBox, mc.thePlayer.getPositionEyes(1.0f));
    }

    public static double clampVecToBox(AxisAlignedBB boundingBox, Vec3 point) {
        if (boundingBox.isVecInside(point)) {
            return 0.0;
        }
        Vec3 clampedPoint = clampVecToBox(point, boundingBox);
        double deltaX = clampedPoint.xCoord - point.xCoord;
        double deltaY = clampedPoint.yCoord - point.yCoord;
        double deltaZ = clampedPoint.zCoord - point.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static float angleToEntity(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        AxisAlignedBB boundingBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
        if (boundingBox.isVecInside(eyePos)) {
            return 0.0f;
        }
        double deltaX = entity.posX - eyePos.xCoord;
        double deltaZ = entity.posZ - eyePos.zCoord;
        return Math.abs(MathHelper.wrapAngleTo180_float((float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw)) * 2.0f;
    }

    public static float getYawBetween(double x1, double z1, double x2, double z2) {
        return MathHelper.wrapAngleTo180_float((float) (Math.atan2(z2 - z1, x2 - x1) * 180.0 / Math.PI) - 90.0f - mc.thePlayer.rotationYaw);
    }

    public static MovingObjectPosition rayTrace(float yaw, float pitch, double distance, float partialTicks) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);


        Vec3 targetPos = eyePos.addVector(
                lookVec.xCoord * distance,
                lookVec.yCoord * distance,
                lookVec.zCoord * distance
        );

        return mc.theWorld.rayTraceBlocks(eyePos, targetPos, false, false, true);
    }

    private static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public static MovingObjectPosition rayTrace(Entity entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        float borderSize = entity.getCollisionBorderSize();
        Vec3 targetPos = clampVecToBox(eyePos, entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize));
        return mc.theWorld.rayTraceBlocks(eyePos, targetPos);
    }

    public static MovingObjectPosition rayTrace(AxisAlignedBB boundingBox, float yaw, float pitch, double distance) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 lookVec = getVectorForRotation(pitch, yaw);


        Vec3 targetPos = eyePos.addVector(
                lookVec.xCoord * distance,
                lookVec.yCoord * distance,
                lookVec.zCoord * distance
        );

        return boundingBox.calculateIntercept(eyePos, targetPos);
    }

    }
