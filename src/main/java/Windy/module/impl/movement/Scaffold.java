package Windy.module.impl.movement;

import Windy.event.Event;
import Windy.event.impl.*;
import Windy.event.impl.network.*;
import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.combat.AuraSupport;
import Windy.util.movement.ScaffoldSupport;
import Windy.util.network.PortBlink;
import Windy.util.render.ColorUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import Windy.util.rotation.PortRotations;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.input.Keyboard;

@ModuleInfo(name="Scaffold", description = "Places blocks under you", category=Category.Movement)
public class Scaffold extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int ROTATION_SNAP = 7;
    private static final int ROTATION_THREE_FMC = 8;
    private static final int ROTATION_SNAP2 = 9;
    private static final double[] placeOffsets = new double[]{
            0.03125,
            0.09375,
            0.15625,
            0.21875,
            0.28125,
            0.34375,
            0.40625,
            0.46875,
            0.53125,
            0.59375,
            0.65625,
            0.71875,
            0.78125,
            0.84375,
            0.90625,
            0.96875
    };
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    @Getter
    public static float yaw = -180.0F;
    @Getter
    public static float pitch = 0.0F;
    private boolean canRotate = false;
    private int towerTick = 0;
    private int towerDelay = 0;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private EnumFacing targetFacing = null;
    private int safeStuckTicks = 0;
    private int safeStuckDelayTicks = 0;
    private double safePrevMotionY = 0.0;
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private boolean safeStuckActive = false;
    private boolean snapRotating = false;
    private boolean placedThisTick = false;
    private int threeFmcAirTicks = 0;
    private int threeFmcGroundTicks = 0;
    private int threeFmcPlaceCooldown = 0;
    private float lastSnapPlaceYaw = Float.NaN;
    private float lastSnapPlacePitch = Float.NaN;
    public final ModeSetting rotationMode = new ModeSetting("rotations", 2, new String[]{"none", "default", "backwards", "sideways", "godbridge", "smooth","SNAP"});
    public final NumberSetting tellystartrotationminspeed = new NumberSetting("Fast Turn Min", 90.0F, 1.0F, 180.0F, 0.01);
    public final NumberSetting tellystartrotationmaxspeed = new NumberSetting("Fast Turn Max", 95.0F, 1.0F, 180.0F, 0.01);
    public final NumberSetting tellynormalrotationminspeed = new NumberSetting("Slow Turn Min", 30.0F, 1.0F, 180.0F, 0.01);
    public final NumberSetting tellynormalrotationmaxspeed = new NumberSetting("Slow Turn Max", 35.0F, 1.0F, 180.0F, 0.01);
    public final ModeSetting moveFix = new ModeSetting("move-fix", 1, new String[]{"none", "silent"});
    public final ModeSetting sprintMode = new ModeSetting("sprint", 0, new String[]{"none", "vanila"});
    public final ModeSetting tower = new ModeSetting("tower", 0, new String[]{"none", "vanilla", "extra", "telly"});
    public final BooleanSetting hypixeltower = new BooleanSetting("hypixeltower", false);
    public final BooleanSetting safe = new BooleanSetting("safe", false);
    public final NumberSetting safeStuckDelayTicksSetting = new NumberSetting("safe-delay-ticks", 1, 1, 3, 1);
    public final ModeSetting keepY = new ModeSetting("keep-y", 0, new String[]{"none", "vanilla", "extra", "telly"});
    public final BooleanSetting keepYonPress = new BooleanSetting("keep-y-on-press", false);
    public final BooleanSetting disableWhileJumpActive = new BooleanSetting("no-keep-y-on-jump-potion", false);
    public final BooleanSetting multiplace = new BooleanSetting("multi-place", true);
    public final BooleanSetting safeWalk = new BooleanSetting("safe-walk", true);
    public final BooleanSetting swing = new BooleanSetting("swing", true);
    public final BooleanSetting itemSpoof = new BooleanSetting("Keep Held Item", false);
    public final BooleanSetting blockCounter = new BooleanSetting("block-counter", true);
    public final BooleanSetting eagle = new BooleanSetting("eagle", false);
    public final NumberSetting edgeDistance = new NumberSetting("edge-distance", 0.13F, 0.0F, 0.5F, 0.01);
    public final NumberSetting sneakDelay = new NumberSetting("sneak-delay", 80, 0, 500, 1);
    public final NumberSetting blocksPerSneak = new NumberSetting("blocks-per-sneak", 1, 1, 5, 1);
    private boolean eagleSneaking = false;
    private int eagleSneakTicks = 0;
    private long eagleLastSneakTime = 0L;
    private int eagleBlocksPlaced = 0;
    public final BooleanSetting espOutline = new BooleanSetting("outline-esp", false);
    public final ModeSetting espColor = new ModeSetting("outline-color", 0, new String[]{"Default", "HUD"});
    private final Map<BlockPos, Long> espHighlight = new HashMap<>();

    private boolean shouldStopSprint() {
        if (this.isThreeFmcMode() && !this.isThreeFmcTellyMode()) {
            return true;
        }
        if (this.isTowering()) {
            return false;
        } else {
            boolean stage = this.keepY.getModeIndex() == 1 || this.keepY.getModeIndex() == 2;
            return (!stage || this.stage <= 0) && this.sprintMode.getModeIndex() == 0;
        }
    }


    private boolean isThreeFmcMode() {
        return this.rotationMode.getModeIndex() == ROTATION_THREE_FMC;
    }

    private boolean isThreeFmcTellyMode() {
        return this.isThreeFmcMode() && (this.keepY.getModeIndex() == 3);
    }

    private void updateThreeFmcState() {
        if (!this.isThreeFmcMode() || mc.thePlayer == null) {
            this.threeFmcAirTicks = 0;
            this.threeFmcGroundTicks = 0;
            this.threeFmcPlaceCooldown = 0;
            return;
        }

        if (mc.thePlayer.onGround) {
            this.threeFmcGroundTicks++;
            this.threeFmcAirTicks = 0;
        } else {
            this.threeFmcAirTicks++;
            this.threeFmcGroundTicks = 0;
        }

        if (this.threeFmcPlaceCooldown > 0) {
            this.threeFmcPlaceCooldown--;
        }
    }

    private void quietThreeFmcMovement() {
        if (!this.isThreeFmcMode() || this.isThreeFmcTellyMode() || mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.setSprinting(false);
        if (mc.gameSettings != null) {
            AuraSupport.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }
    }

    private boolean canThreeFmcPlaceNow() {
        if (!this.isThreeFmcMode()) {
            return true;
        }
        if (mc.thePlayer == null || this.placedThisTick || this.threeFmcPlaceCooldown > 0) {
            return false;
        }
        if ((!this.isThreeFmcTellyMode() && mc.thePlayer.isSprinting()) || mc.thePlayer.isCollidedHorizontally || mc.thePlayer.hurtTime > 0) {
            return false;
        }
        if (mc.thePlayer.onGround) {
            return Math.abs(mc.thePlayer.motionY) < 1.0E-4 && this.threeFmcGroundTicks > 0;
        }
        return this.isThreeFmcTellyMode() ? this.threeFmcAirTicks > 1 : this.threeFmcAirTicks > 2;
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing enumFacing = null;
        for (EnumFacing facing : EnumFacing.VALUES) {
            if (facing != EnumFacing.DOWN) {
                BlockPos pos = blockPos1.offset(facing);
                if (pos.getY() <= blockPos3.getY()) {
                    double distance = pos.distanceSqToCenter((double) blockPos3.getX() + 0.5, (double) blockPos3.getY() + 0.5, (double) blockPos3.getZ() + 0.5);
                    if (enumFacing == null || distance < offset || distance == offset && facing == EnumFacing.UP) {
                        offset = distance;
                        enumFacing = facing;
                    }
                }
            }
        }
        return enumFacing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (this.stage != 0 && !this.shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ)
        );
        if (!ScaffoldSupport.isReplaceable(targetPos)) {
            return null;
        } else {
            ArrayList<BlockPos> positions = new ArrayList<>();
            for (int x = -4; x <= 4; x++) {
                for (int y = -4; y <= 0; y++) {
                    for (int z = -4; z <= 4; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        if (!ScaffoldSupport.isReplaceable(pos)
                                && !ScaffoldSupport.isInteractable(pos)
                                && !(
                                mc.thePlayer.getDistance((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5)
                                        > (double) mc.playerController.getBlockReachDistance()
                        )
                                && (this.stage == 0 || this.shouldKeepY || pos.getY() < this.startY)) {
                            for (EnumFacing facing : EnumFacing.VALUES) {
                                if (facing != EnumFacing.DOWN) {
                                    BlockPos blockPos = pos.offset(facing);
                                    if (ScaffoldSupport.isReplaceable(blockPos)) {
                                        positions.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (positions.isEmpty()) {
                return null;
            } else {
                positions.sort(
                        Comparator.comparingDouble(
                                o -> o.distanceSqToCenter((double) targetPos.getX() + 0.5, (double) targetPos.getY() + 0.5, (double) targetPos.getZ() + 0.5)
                        )
                );
                BlockPos blockPos = positions.get(0);
                EnumFacing facing = this.getBestFacing(blockPos, targetPos);
                return facing == null ? null : new BlockData(blockPos, facing);
            }
        }
    }

    private void place(BlockPos blockPos, EnumFacing enumFacing, Vec3 vec3) {
        if (!this.canThreeFmcPlaceNow()) {
            return;
        }
        if (AuraSupport.isHoldingBlock() && this.blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.inventory.getCurrentItem(), blockPos, enumFacing, vec3)) {
                if (mc.playerController.getCurrentGameType() != GameType.CREATIVE) {
                    this.blockCount--;
                }
                this.placedThisTick = true;
                if (this.isThreeFmcMode()) {
                    this.threeFmcPlaceCooldown = 1;
                }
                this.markPlaced(blockPos.offset(enumFacing));
                this.eagleBlocksPlaced++;
                if (this.swing.isState()) {
                    mc.thePlayer.swingItem();
                } else {
                    Windy.util.network.PacketUtils.sendPacket(new C0APacketAnimation());
                }
            }
        }
    }

    private MovingObjectPosition getPlacementMop(BlockData blockData, float yaw, float pitch) {
        MovingObjectPosition mop = PortRotations.rayTrace(yaw, pitch, mc.playerController.getBlockReachDistance(), 1.0F);
        if (mop == null
                || mop.typeOfHit != MovingObjectType.BLOCK
                || !mop.getBlockPos().equals(blockData.blockPos())
                || mop.sideHit != blockData.facing()) {
            return null;
        }
        return mop;
    }

    private boolean isDuplicateSnapRotation(float yaw, float pitch) {
        return !Float.isNaN(this.lastSnapPlaceYaw)
                && Math.abs(MathHelper.wrapAngleTo180_float(yaw - this.lastSnapPlaceYaw)) < 0.35F;
    }

    private float[] getSnapRotation(BlockData blockData, float yaw, float pitch) {
        float baseYaw = PortRotations.quantizeAngle(yaw);
        float basePitch = PortRotations.quantizeAngle(MathHelper.clamp_float(pitch, -90.0F, 90.0F));

        if (!this.isDuplicateSnapRotation(baseYaw, basePitch)) {
            return new float[]{baseYaw, basePitch};
        }

        for (int i = 0; i < 24; i++) {
            float yawStep = 0.35F + 0.075F * (float) (i / 2);
            float pitchStep = 0.025F + 0.01F * (float) (i / 3);
            float testYaw = PortRotations.quantizeAngle(baseYaw + (i % 2 == 0 ? yawStep : -yawStep));
            float testPitch = PortRotations.quantizeAngle(MathHelper.clamp_float(basePitch + (i % 4 < 2 ? pitchStep : -pitchStep), -90.0F, 90.0F));

            if (!this.isDuplicateSnapRotation(testYaw, testPitch) && this.getPlacementMop(blockData, testYaw, testPitch) != null) {
                return new float[]{testYaw, testPitch};
            }
        }

        return null;
    }

    private void rememberSnapRotation() {
        this.lastSnapPlaceYaw = this.yaw;
        this.lastSnapPlacePitch = this.pitch;
    }

    private EnumFacing yawToFacing(float yaw) {
        if (yaw < -135.0F || yaw > 135.0F) {
            return EnumFacing.NORTH;
        } else if (yaw < -45.0F) {
            return EnumFacing.EAST;
        } else {
            return yaw < 45.0F ? EnumFacing.SOUTH : EnumFacing.WEST;
        }
    }

    private double distanceToEdge(EnumFacing enumFacing) {
        switch (enumFacing) {
            case NORTH:
                return mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
            case EAST:
                return Math.ceil(mc.thePlayer.posX) - mc.thePlayer.posX;
            case SOUTH:
                return Math.ceil(mc.thePlayer.posZ) - mc.thePlayer.posZ;
            case WEST:
            default:
                return mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        }
    }

    private boolean isNearEdge() {
        if (!mc.thePlayer.onGround) {
            return false;
        }
        double fracX = mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
        double fracZ = mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
        double threshold = this.edgeDistance.getFloat();
        double minDist = Math.min(Math.min(fracX, 1.0 - fracX), Math.min(fracZ, 1.0 - fracZ));
        return minDist <= threshold;
    }

    private boolean shouldSneak() {
        if (!this.eagle.isState() || !mc.thePlayer.onGround) {
            return false;
        }
        if (this.eagleBlocksPlaced < this.blocksPerSneak.getInt()) {
            return false;
        }
        if (System.currentTimeMillis() - this.eagleLastSneakTime < (long) this.sneakDelay.getInt()) {
            return false;
        }
        return this.isNearEdge();
    }

    private void updateEagle() {
        if (!this.eagle.isState()) {
            this.eagleSneaking = false;
            this.eagleSneakTicks = 0;
            return;
        }
        if (this.eagleSneakTicks > 0) {
            this.eagleSneakTicks--;
            if (this.eagleSneakTicks == 0) {
                this.eagleSneaking = false;
            }
            return;
        }
        if (this.shouldSneak()) {
            this.eagleSneaking = true;
            this.eagleSneakTicks = 2;
            this.eagleLastSneakTime = System.currentTimeMillis();
            this.eagleBlocksPlaced = 0;
        }
    }

    private float getSpeed() {
        if (!mc.thePlayer.onGround) {
            return (float) 100 / 100.0F;
        } else {
            return (float) 100 / 100.0F;
        }
    }

    private double getRandomOffset() {
        return 0.2155 - ScaffoldSupport.nextDouble(1.0E-4, 9.0E-4);
    }

    private float getCurrentYaw() {
        return ScaffoldSupport.adjustYaw(
                mc.thePlayer.rotationYaw, (float) ScaffoldSupport.getForwardValue(), (float) ScaffoldSupport.getLeftValue()
        );
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (mc.thePlayer.onGround && ScaffoldSupport.isForwardPressed() && ScaffoldSupport.hasHeadroom()) {
            boolean keepY = this.keepY.getModeIndex() == 3;
            boolean tower = this.tower.getModeIndex() == 3;
            return keepY && this.stage > 0 || tower && mc.gameSettings.keyBindJump.isKeyDown();
        } else {
            return false;
        }
    }

    public Scaffold() {
        tellystartrotationminspeed.addParent(rotationMode, setting -> keepY.getModeIndex() == 3);
        tellystartrotationmaxspeed.addParent(rotationMode, setting ->  keepY.getModeIndex() == 3);
        tellynormalrotationminspeed.addParent(rotationMode, setting -> keepY.getModeIndex() == 3);
        tellynormalrotationmaxspeed.addParent(rotationMode, setting -> keepY.getModeIndex() == 3);
        hypixeltower.addParent(hypixeltower, ignored -> this.tower.getModeIndex() == 3);
        safe.addParent(safe, ignored -> this.tower.getModeIndex() == 3);
        safeStuckDelayTicksSetting.addParent(safeStuckDelayTicksSetting, ignored -> this.tower.getModeIndex() == 3 && this.safe.isState());
        keepYonPress.addParent(keepYonPress, ignored -> this.keepY.getModeIndex() != 0);
        disableWhileJumpActive.addParent(disableWhileJumpActive, ignored -> this.keepY.getModeIndex() != 0);
        edgeDistance.addParent(edgeDistance, ignored -> this.eagle.isState());
        sneakDelay.addParent(sneakDelay, ignored -> this.eagle.isState());
        blocksPerSneak.addParent(blocksPerSneak, ignored -> this.eagle.isState());
        espColor.addParent(espColor, ignored -> this.espOutline.isState());
        addSettings(
                rotationMode, tellystartrotationminspeed, tellystartrotationmaxspeed, tellynormalrotationminspeed, tellynormalrotationmaxspeed, moveFix,
                sprintMode, tower, hypixeltower,
                safe, safeStuckDelayTicksSetting, keepY, keepYonPress, disableWhileJumpActive, multiplace,
                safeWalk, swing, itemSpoof, blockCounter, eagle, edgeDistance,
                sneakDelay, blocksPerSneak, espOutline, espColor);
    }

    public int getSlot() {
        return this.lastSlot;
    }

    private boolean rotationRequested;
    private float requestedMovementYaw = Float.NaN;


    public boolean prepareMotion(MotionEvent event, float previousYaw, float previousPitch) {
        rotationRequested = false;
        requestedMovementYaw = Float.NaN;
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        processScaffoldMotion(event, previousYaw, previousPitch);
        if (rotationRequested) requestedMovementYaw = event.getYaw();
        return rotationRequested;
    }

    @Override
    public void onMotionEvent(MotionEvent event) {

    }

    private void processScaffoldMotion(MotionEvent event, float previousYaw, float previousPitch) {
        if (this.isToggled() && event.getType() == Event.Type.PRE) {
            this.placedThisTick = false;
            this.updateThreeFmcState();
            this.quietThreeFmcMovement();
            if (this.safeStuckDelayTicks > 0) {
                this.safeStuckDelayTicks--;
                if (this.safeStuckDelayTicks <= 0) {
                    this.safeStuckTicks = 1;
                }
            }
            if (this.safeStuckTicks > 0) {
                if (!this.safeStuckActive) {
                    this.savedMotionX = mc.thePlayer.motionX;
                    this.savedMotionY = mc.thePlayer.motionY;
                    this.savedMotionZ = mc.thePlayer.motionZ;
                    this.safeStuckActive = true;
                }
                PortBlink.set("BLINK", true);
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;
            } else if (this.safeStuckActive) {
                PortBlink.set("BLINK", false);
                mc.thePlayer.motionX = this.savedMotionX;
                mc.thePlayer.motionY = this.savedMotionY;
                mc.thePlayer.motionZ = this.savedMotionZ;
                this.safeStuckActive = false;
            }
            if (this.rotationTick > 0) {
                this.rotationTick--;
            }
            this.updateEagle();
            if (hypixeltower.isState() && mc.thePlayer.motionY <= 0.0 && Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ) <= 0.02D && mc.thePlayer.motionY >= -0.09 && !(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()) ||
                    Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()) ||
                    Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()) ||
                    Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) && Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                mc.thePlayer.motionY = -0.38;
            }
            if (mc.thePlayer.onGround) {
                if (this.stage > 0) {
                    this.stage--;
                }
                if (this.stage < 0) {
                    this.stage++;
                }
                if (this.stage == 0
                        && this.keepY.getModeIndex() != 0
                        && (!(Boolean) this.keepYonPress.isState() || AuraSupport.isUsingItem())
                        && (!this.disableWhileJumpActive.isState() || !mc.thePlayer.isPotionActive(Potion.jump))
                        && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    this.stage = 1;
                }
                this.startY = this.shouldKeepY ? this.startY : MathHelper.floor_double(mc.thePlayer.posY);
                this.shouldKeepY = false;
                this.towering = false;
            }
            if ((mc.currentScreen == null)) {
                ItemStack stack = mc.thePlayer.getHeldItem();
                int count = AuraSupport.isBlock(stack) ? stack.stackSize : 0;
                this.blockCount = Math.min(this.blockCount, count);
                if (this.blockCount <= 0) {
                    int slot = mc.thePlayer.inventory.currentItem;
                    if (this.blockCount == 0) {
                        slot--;
                    }
                    for (int i = slot; i > slot - 9; i--) {
                        int hotbarSlot = (i % 9 + 9) % 9;
                        ItemStack candidate = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);
                        if (AuraSupport.isBlock(candidate)) {
                            mc.thePlayer.inventory.currentItem = hotbarSlot;
                            this.blockCount = candidate.stackSize;
                            break;
                        }
                    }
                }
                float currentYaw = this.getCurrentYaw();
                float yawDiffTo180 = PortRotations.wrapAngleDiff(currentYaw - 180.0F, previousYaw);
                float diagonalYaw = this.isDiagonal(currentYaw)
                        ? yawDiffTo180
                        : PortRotations.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), previousYaw);
                boolean snapMode = this.rotationMode.getModeIndex() == ROTATION_SNAP || this.rotationMode.getModeIndex() == ROTATION_SNAP2;
                boolean threeFmcMode = this.rotationMode.getModeIndex() == ROTATION_THREE_FMC;
                boolean threeFmcTelly = this.isThreeFmcTellyMode();
                this.snapRotating = false;
                if (!this.canRotate) {
                    switch (this.rotationMode.getModeIndex()) {
                        case 1:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                                this.pitch = PortRotations.quantizeAngle(85.0F);
                            } else {
                                this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 2:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = PortRotations.quantizeAngle(yawDiffTo180);
                                this.pitch = PortRotations.quantizeAngle(85.0F);
                            } else {
                                this.yaw = PortRotations.quantizeAngle(yawDiffTo180);
                            }
                            break;
                        case 3:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                                this.pitch = PortRotations.quantizeAngle(85.0F);
                            } else {
                                this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                            }
                            break;
                        case 4: // God Bridge Mode
                            // 1. SNAP YAW TO NEAREST 45-DEGREE DIAGONAL
                            // This finds if you are facing 45, 135, -45, or -135 and locks you there perfectly.
                            float roundedYaw = Math.round(currentYaw / 45.0f) * 45.0f;
                            this.yaw = PortRotations.quantizeAngle(roundedYaw);

                            // 2. SET THE GODBRIDGE PITCH
                            if (this.pitch == 0.0F || !this.canRotate) {
        /*
           75.6f to 79.5f is the "Golden Range" for Godbridging.
           - 75.6f is good for high CPS/Short Drag
           - 79.3f is common for "Telly" or standard Godbridge
        */
                                float godBridgePitch = 79.3f;
                                this.pitch = PortRotations.quantizeAngle(godBridgePitch);
                            }
                            break;
                        case 5:
                            if (this.yaw == -180.0F && this.pitch == 0.0F) {
                                this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                                this.pitch = PortRotations.quantizeAngle(85.0F);
                            } else {
                                float targetYaw = this.isDiagonal(currentYaw) ? diagonalYaw : yawDiffTo180;
                                float yawDiff = MathHelper.wrapAngleTo180_float(targetYaw - this.yaw);
                                float pitchDiff = MathHelper.wrapAngleTo180_float(85.0F - this.pitch);
                                float yawTolerance = this.rotationTick >= 2 ? ScaffoldSupport.nextFloat(tellystartrotationminspeed.getFloat(), tellystartrotationmaxspeed.getFloat()) : ScaffoldSupport.nextFloat(tellynormalrotationminspeed.getFloat(), tellynormalrotationmaxspeed.getFloat());
                                float pitchTolerance = this.rotationTick >= 2 ? ScaffoldSupport.nextFloat(tellystartrotationminspeed.getFloat(), tellystartrotationmaxspeed.getFloat()) : ScaffoldSupport.nextFloat(tellynormalrotationminspeed.getFloat(), tellynormalrotationmaxspeed.getFloat());
                                this.yaw = PortRotations.quantizeAngle(this.yaw + PortRotations.clampAngle(yawDiff, yawTolerance));
                                this.pitch = PortRotations.quantizeAngle(this.pitch + PortRotations.clampAngle(pitchDiff, pitchTolerance));
                            }
                            break;
                        case ROTATION_SNAP:
                    }
                }
                BlockData blockData = this.getBlockData();

                Vec3 hitVec = null;
                if (blockData != null) {
                    double[] x = placeOffsets;
                    double[] y = placeOffsets;
                    double[] z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH:
                            z = new double[]{0.0};
                            break;
                        case EAST:
                            x = new double[]{1.0};
                            break;
                        case SOUTH:
                            z = new double[]{1.0};
                            break;
                        case WEST:
                            x = new double[]{0.0};
                            break;
                        case DOWN:
                            y = new double[]{0.0};
                            break;
                        case UP:
                            y = new double[]{1.0};
                    }
                    float bestYaw = -180.0F;
                    float bestPitch = 0.0F;
                    float bestDiff = 0.0F;
                    for (double dx : x) {
                        for (double dy : y) {
                            for (double dz : z) {
                                double relX = (double) blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                                double relY = (double) blockData.blockPos().getY() + dy - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double relZ = (double) blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                                float baseYaw = PortRotations.wrapAngleDiff(this.yaw, previousYaw);
                                float[] rotations = PortRotations.getRotationsTo(relX, relY, relZ, baseYaw, this.pitch);
                                MovingObjectPosition mop = PortRotations.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop != null
                                        && mop.typeOfHit == MovingObjectType.BLOCK
                                        && mop.getBlockPos().equals(blockData.blockPos())
                                        && mop.sideHit == blockData.facing()) {
                                    float totalDiff = Math.abs(rotations[0] - baseYaw) + Math.abs(rotations[1] - this.pitch);
                                    if (bestYaw == -180.0F && bestPitch == 0.0F || totalDiff < bestDiff) {
                                        bestYaw = rotations[0];
                                        bestPitch = rotations[1];
                                        bestDiff = totalDiff;
                                        hitVec = mop.hitVec;
                                    }
                                }
                            }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        this.yaw = bestYaw;
                        this.pitch = bestPitch;
                        this.canRotate = true;
                    } else if (threeFmcMode) {
                        this.canRotate = false;
                    }
                }
                boolean towerRotating = this.towering || this.isTowering();
                boolean snapAlreadyLooking = false;
                boolean snapCanPlace = true;
                if (snapMode && !towerRotating && blockData != null) {
                    MovingObjectPosition currentMop = this.getPlacementMop(blockData, previousYaw, previousPitch);
                    if (currentMop != null) {
                        float[] snapRotation = this.getSnapRotation(blockData, previousYaw, previousPitch);
                        if (snapRotation == null) {
                            snapCanPlace = false;
                            hitVec = null;
                        } else {
                            this.yaw = snapRotation[0];
                            this.pitch = snapRotation[1];
                            this.canRotate = true;
                            MovingObjectPosition snapMop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            hitVec = snapMop != null ? snapMop.hitVec : currentMop.hitVec;
                            this.snapRotating = true;
                            int snapDelay = this.rotationMode.getModeIndex() == ROTATION_SNAP2 ? 0 : 1;
                            if (this.rotationTick > snapDelay) {
                                this.rotationTick = snapDelay;
                            }
                        }
                    } else if (hitVec != null && this.canRotate) {
                        float[] snapRotation = this.getSnapRotation(blockData, this.yaw, this.pitch);
                        if (snapRotation == null) {
                            snapCanPlace = false;
                            hitVec = null;
                        } else {
                            this.yaw = snapRotation[0];
                            this.pitch = snapRotation[1];
                            MovingObjectPosition snapMop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            if (snapMop != null) {
                                hitVec = snapMop.hitVec;
                            }
                            this.snapRotating = true;
                            int snapDelay = this.rotationMode.getModeIndex() == ROTATION_SNAP2 ? 0 : 1;
                            if (this.rotationTick > snapDelay) {
                                this.rotationTick = snapDelay;
                            }
                        }
                    }
                }
                if (this.canRotate && ScaffoldSupport.isForwardPressed() && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - this.yaw)) < 90.0F) {
                    switch (this.rotationMode.getModeIndex()) {
                        case 2:
                            this.yaw = PortRotations.quantizeAngle(yawDiffTo180);
                            break;
                        case 3:
                            this.yaw = PortRotations.quantizeAngle(diagonalYaw);
                    }
                }
                float placeYaw = this.yaw;
                float placePitch = this.pitch;
                if (this.rotationMode.getModeIndex() != 0 && (!snapMode || this.snapRotating || towerRotating)) {
                    float targetYaw = this.yaw;
                    float targetPitch = this.pitch;
                    if ((!threeFmcMode || threeFmcTelly) && this.towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > (double) (this.startY + 1))) {
                        float yawDiff = MathHelper.wrapAngleTo180_float(this.yaw - previousYaw);
                        float tolerance = this.rotationTick >= 2 ? ScaffoldSupport.nextFloat(tellystartrotationminspeed.getFloat(), tellystartrotationmaxspeed.getFloat()) : ScaffoldSupport.nextFloat(tellynormalrotationminspeed.getFloat(), tellynormalrotationmaxspeed.getFloat());
                        if (Math.abs(yawDiff) > tolerance) {
                            float clampedYaw = PortRotations.clampAngle(yawDiff, tolerance);
                            targetYaw = PortRotations.quantizeAngle(previousYaw + clampedYaw);
                            this.rotationTick = Math.max(this.rotationTick, 1);
                        }
                    }
                    if (towerRotating && this.isTowering()) {
                        if (!threeFmcMode || threeFmcTelly) {
                            float yawDelta = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - previousYaw);
                            targetYaw = PortRotations.quantizeAngle(previousYaw + yawDelta * ScaffoldSupport.nextFloat(0.98F, 0.99F));
                            targetPitch = PortRotations.quantizeAngle(ScaffoldSupport.nextFloat(30.0F, 80.0F));
                        }
                        this.rotationTick = 3;
                        this.towering = true;
                    }
                    placeYaw = targetYaw;
                    placePitch = targetPitch;
                    event.setYaw(targetYaw);
                    event.setPitch(targetPitch);
                    rotationRequested = true;
                    if (this.moveFix.getModeIndex() == 1) {
                        net.minecraft.entity.player.EntityPlayer.movementYaw = targetYaw;
                    }
                }
                if (threeFmcMode && blockData != null && hitVec != null) {
                    MovingObjectPosition verifiedMop = this.getPlacementMop(blockData, placeYaw, placePitch);
                    if (verifiedMop == null) {
                        hitVec = null;
                    } else {
                        hitVec = verifiedMop.hitVec;
                    }
                }
                if (blockData != null && hitVec != null && snapCanPlace && (this.rotationTick <= 0 || snapAlreadyLooking)) {
                    this.place(blockData.blockPos(), blockData.facing(), hitVec);
                    if (snapMode) {
                        this.rememberSnapRotation();
                    }
                    if (this.multiplace.isState() && !snapMode) {
                        for (int i = 0; i < 3; i++) {
                            blockData = this.getBlockData();
                            if (blockData == null) {
                                break;
                            }
                            MovingObjectPosition mop = PortRotations.rayTrace(this.yaw, this.pitch, mc.playerController.getBlockReachDistance(), 1.0F);
                            if (mop != null
                                    && mop.typeOfHit == MovingObjectType.BLOCK
                                    && mop.getBlockPos().equals(blockData.blockPos())
                                    && mop.sideHit == blockData.facing()) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            } else {
                                hitVec = ScaffoldSupport.getClickVec(blockData.blockPos(), blockData.facing());
                                double dx = hitVec.xCoord - mc.thePlayer.posX;
                                double dy = hitVec.yCoord - mc.thePlayer.posY - (double) mc.thePlayer.getEyeHeight();
                                double dz = hitVec.zCoord - mc.thePlayer.posZ;
                                float[] rotations = PortRotations.getRotationsTo(dx, dy, dz, previousYaw, previousPitch);
                                if (!(Math.abs(rotations[0] - this.yaw) < 120.0F) || !(Math.abs(rotations[1] - this.pitch) < 60.0F)) {
                                    break;
                                }
                                mop = PortRotations.rayTrace(rotations[0], rotations[1], mc.playerController.getBlockReachDistance(), 1.0F);
                                if (mop == null
                                        || mop.typeOfHit != MovingObjectType.BLOCK
                                        || !mop.getBlockPos().equals(blockData.blockPos())
                                        || mop.sideHit != blockData.facing()) {
                                    break;
                                }
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            }
                        }
                    }
                }
                if (this.targetFacing != null) {
                    if (threeFmcMode) {
                        this.targetFacing = null;
                    } else if (this.rotationTick <= 0 && !this.placedThisTick) {
                        int playerBlockX = MathHelper.floor_double(mc.thePlayer.posX);
                        int playerBlockY = MathHelper.floor_double(mc.thePlayer.posY);
                        int playerBlockZ = MathHelper.floor_double(mc.thePlayer.posZ);
                        BlockPos belowPlayer = new BlockPos(playerBlockX, playerBlockY - 1, playerBlockZ);
                        hitVec = ScaffoldSupport.getHitVec(belowPlayer, this.targetFacing, this.yaw, this.pitch);
                        this.place(belowPlayer, this.targetFacing, hitVec);
                    }
                    this.targetFacing = null;
                } else if ((this.keepY.getModeIndex() == 2) && this.stage > 0 && !mc.thePlayer.onGround) {
                    int nextBlockY = MathHelper.floor_double(mc.thePlayer.posY + mc.thePlayer.motionY);
                    if (nextBlockY <= this.startY && mc.thePlayer.posY > (double) (this.startY + 1)) {
                        this.shouldKeepY = true;
                        blockData = this.getBlockData();
                        if (blockData != null && this.rotationTick <= 0 && !this.placedThisTick) {
                            MovingObjectPosition mop = this.getPlacementMop(blockData, this.yaw, this.pitch);
                            if (mop != null) {
                                this.place(blockData.blockPos(), blockData.facing(), mop.hitVec);
                            }
                        }
                    }
                }
            }
        }
    }

    public void onStrafe(StrafeEvent event) {
        if (this.isToggled()) {
            if (this.safeStuckTicks > 0) {
                event.setForward(0.0F);
                event.setStrafe(0.0F);
                return;
            }
            if (this.isThreeFmcMode() && !this.isThreeFmcTellyMode()) {
                this.towerTick = 0;
                this.towerDelay = 0;
                return;
            }
            if (!mc.thePlayer.isCollidedHorizontally
                    && mc.thePlayer.hurtTime <= 5
                    && !mc.thePlayer.isPotionActive(Potion.jump)
                    && mc.gameSettings.keyBindJump.isKeyDown()
                    && AuraSupport.isHoldingBlock()) {
                int yState = (int) (mc.thePlayer.posY % 1.0 * 100.0);
                switch (this.tower.getModeIndex()) {
                    case 1:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && ScaffoldSupport.hasSupportBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    this.towerTick = 2;
                                    mc.thePlayer.motionY = 0.42F;
                                    if (ScaffoldSupport.isForwardPressed()) {
                                        ScaffoldSupport.setSpeed(ScaffoldSupport.getSpeed(), ScaffoldSupport.getMoveYaw());
                                    } else {
                                        ScaffoldSupport.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                    }
                                    return;
                                } else {
                                    this.towerTick = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = 0.75 - mc.thePlayer.posY % 1.0;
                                return;
                            case 3:
                                this.towerTick = 1;
                                mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                return;
                            default:
                                this.towerTick = 0;
                                return;
                        }
                    case 2:
                        switch (this.towerTick) {
                            case 0:
                                if (mc.thePlayer.onGround) {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = -0.0784000015258789;
                                }
                                return;
                            case 1:
                                if (yState == 0 && ScaffoldSupport.hasSupportBelow()) {
                                    this.startY = MathHelper.floor_double(mc.thePlayer.posY);
                                    if (!ScaffoldSupport.isForwardPressed()) {
                                        this.towerDelay = 2;
                                        ScaffoldSupport.setSpeed(0.0);
                                        event.setForward(0.0F);
                                        event.setStrafe(0.0F);
                                        EnumFacing facing = this.yawToFacing(MathHelper.wrapAngleTo180_float(this.yaw - 180.0F));
                                        double distance = this.distanceToEdge(facing);
                                        if (distance > 0.1) {
                                            if (mc.thePlayer.onGround) {
                                                Vec3i directionVec = facing.getDirectionVec();
                                                double offset = Math.min(this.getRandomOffset(), distance - 0.05);
                                                double jitter = ScaffoldSupport.nextDouble(0.02, 0.03);
                                                AxisAlignedBB nextBox = mc.thePlayer
                                                        .getEntityBoundingBox()
                                                        .offset((double) directionVec.getX() * (offset - jitter), 0.0, (double) directionVec.getZ() * (offset - jitter));
                                                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, nextBox).isEmpty()) {
                                                    mc.thePlayer.motionY = -0.0784000015258789;
                                                    mc.thePlayer
                                                            .setPosition(nextBox.minX + (nextBox.maxX - nextBox.minX) / 2.0, nextBox.minY, nextBox.minZ + (nextBox.maxZ - nextBox.minZ) / 2.0);
                                                }
                                                return;
                                            }
                                        } else {
                                            this.towerTick = 2;
                                            this.targetFacing = facing;
                                            mc.thePlayer.motionY = 0.42F;
                                        }
                                        return;
                                    } else {
                                        this.towerTick = 2;
                                        this.towerDelay++;
                                        mc.thePlayer.motionY = 0.42F;
                                        ScaffoldSupport.setSpeed(ScaffoldSupport.getSpeed(), ScaffoldSupport.getMoveYaw());
                                        return;
                                    }
                                } else {
                                    this.towerTick = 0;
                                    this.towerDelay = 0;
                                    return;
                                }
                            case 2:
                                this.towerTick = 3;
                                mc.thePlayer.motionY = mc.thePlayer.motionY - ScaffoldSupport.nextDouble(0.00101, 0.00109);
                                return;
                            case 3:
                                if (this.towerDelay >= 4) {
                                    this.towerTick = 4;
                                    this.towerDelay = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                }
                                return;
                            case 4:
                                this.towerTick = 5;
                                return;
                            case 5:
                                if (!ScaffoldSupport.hasSupportBelow()) {
                                    this.towerTick = 0;
                                } else {
                                    this.towerTick = 1;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                    mc.thePlayer.motionY -= 0.08;
                                    mc.thePlayer.motionY *= 0.98F;
                                }
                                return;
                            default:
                                this.towerTick = 0;
                                this.towerDelay = 0;
                                return;
                        }
                    default:
                        this.towerTick = 0;
                        this.towerDelay = 0;
                }
            } else {
                this.towerTick = 0;
                this.towerDelay = 0;
            }
        }
    }

    private void adjustInput(MoveInputEvent event) {
        if (this.isToggled()) {
            if (this.safeStuckTicks > 0) {
                mc.thePlayer.movementInput.moveForward = 0.0f;
                mc.thePlayer.movementInput.moveStrafe = 0.0f;
                mc.thePlayer.movementInput.jump = false;
                mc.thePlayer.movementInput.sneak = false;
                return;
            }
            this.quietThreeFmcMovement();
            if (this.moveFix.getModeIndex() == 1 && rotationRequested && Float.isFinite(requestedMovementYaw)
                    && ScaffoldSupport.isForwardPressed()) {

                ScaffoldSupport.fixStrafe(requestedMovementYaw);
            }
            if (mc.thePlayer.onGround && this.stage > 0 && ScaffoldSupport.isForwardPressed()) {
                mc.thePlayer.movementInput.jump = true;
            }
            if (this.eagleSneaking && !mc.thePlayer.movementInput.sneak) {
                mc.thePlayer.movementInput.sneak = true;

            }
        }
    }


    public void updateLivingMotion() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (this.isToggled()) {
            if (this.safeStuckTicks > 0) {
                mc.thePlayer.motionX = 0.0;
                mc.thePlayer.motionY = 0.0;
                mc.thePlayer.motionZ = 0.0;
                this.safeStuckTicks--;
            }
            this.quietThreeFmcMovement();
            float speed = this.isThreeFmcMode() && !this.isThreeFmcTellyMode() ? 1.0F : this.getSpeed();
            if (speed != 1.0F) {
                if (mc.thePlayer.movementInput.moveForward != 0.0F && mc.thePlayer.movementInput.moveStrafe != 0.0F) {
                    mc.thePlayer.movementInput.moveForward = mc.thePlayer.movementInput.moveForward * (1.0F / (float) Math.sqrt(2.0));
                    mc.thePlayer.movementInput.moveStrafe = mc.thePlayer.movementInput.moveStrafe * (1.0F / (float) Math.sqrt(2.0));
                }
                mc.thePlayer.movementInput.moveForward *= speed;
                mc.thePlayer.movementInput.moveStrafe *= speed;
            }
            if (this.shouldStopSprint()) {
                mc.thePlayer.setSprinting(false);
            }

            if (this.safe.isState() && this.tower.getModeIndex() == 3 && mc.gameSettings.keyBindJump.isKeyDown()) {
                float moveYaw = this.getCurrentYaw();
                boolean diagonal = this.isDiagonal(moveYaw);
                if (diagonal && !mc.thePlayer.onGround) {
                    double motionY = mc.thePlayer.motionY;
                    if (this.safePrevMotionY > 0.0 && motionY <= 0.0) {
                        double motionXZ = Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
                        double motionXZSpeedBps = motionXZ * 20.0;
                        if (this.safeStuckDelayTicks <= 0 && this.safeStuckTicks <= 0 && motionXZSpeedBps >= 4.67) {
                            this.safeStuckDelayTicks = this.safeStuckDelayTicksSetting.getInt();
                        }
                    }
                    this.safePrevMotionY = motionY;
                } else {
                    this.safePrevMotionY = mc.thePlayer.motionY;
                }
            } else {
                this.safePrevMotionY = mc.thePlayer.motionY;
            }
        }
    }

    public void onSafeWalk(SafeWalkEvent event) {
        if (!mc.isCallingFromMinecraftThread() || mc.thePlayer == null || mc.theWorld == null) return;
        if (this.isToggled() && this.safeWalk.isState()) {
            if (mc.thePlayer.onGround && mc.thePlayer.motionY <= 0.0 && AuraSupport.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)) {
                event.setSafeWalk(true);
            }
        }
    }


    private void markPlaced(BlockPos pos) {
        if (this.espOutline.isState()) {
            this.espHighlight.put(pos, System.currentTimeMillis());
        }
    }

    public void on3D(float partialTicks) {
        if (!this.espOutline.isState() || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (this.espHighlight.isEmpty()) {
            return;
        }
        int themeColor;
        if (this.espColor.getModeIndex() == 1) {
            themeColor = Windy.util.render.VisualTheme.accent();
        } else {
            themeColor = Color.CYAN.getRGB();
        }
        Iterator<Map.Entry<BlockPos, Long>> iterator = this.espHighlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            long time = System.currentTimeMillis() - entry.getValue();
            if (time > 750L) {
                iterator.remove();
                continue;
            }
            int currentAlpha = (int) (210 - (time / 750.0 * 210));
            if (currentAlpha <= 0) {
                iterator.remove();
                continue;
            }
            AuraSupport.renderBlock(
                    entry.getKey(),
                    (themeColor & 0xFFFFFF) | (currentAlpha << 24),
                    true,
                    false);
        }
    }


    @Override
    public void onEnable() {
        rotationRequested = false;
        requestedMovementYaw = Float.NaN;
        if (mc.thePlayer != null) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
        } else {
            this.lastSlot = -1;
        }
        this.blockCount = -1;
        this.rotationTick = 3;
        this.yaw = -180.0F;
        this.pitch = 0.0F;
        this.canRotate = false;
        this.towerTick = 0;
        this.towerDelay = 0;
        this.towering = false;
        this.safeStuckTicks = 0;
        this.safeStuckDelayTicks = 0;
        this.safePrevMotionY = 0.0;
        this.safeStuckActive = false;
        this.eagleSneaking = false;
        this.eagleSneakTicks = 0;
        this.eagleBlocksPlaced = 0;
        this.eagleLastSneakTime = 0L;
        this.snapRotating = false;
        this.threeFmcAirTicks = 0;
        this.threeFmcGroundTicks = 0;
        this.threeFmcPlaceCooldown = 0;
        this.lastSnapPlaceYaw = Float.NaN;
        this.lastSnapPlacePitch = Float.NaN;
        this.espHighlight.clear();
    }

    @Override
    public void onDisable() {
        rotationRequested = false;
        requestedMovementYaw = Float.NaN;
        net.minecraft.entity.player.EntityPlayer.movementYaw = Float.NaN;
        if (mc.thePlayer != null && this.lastSlot != -1) {
            mc.thePlayer.inventory.currentItem = this.lastSlot;
        }
        PortBlink.set("BLINK", false);
        if (this.safeStuckActive && mc.thePlayer != null) {
            mc.thePlayer.motionX = this.savedMotionX;
            mc.thePlayer.motionY = this.savedMotionY;
            mc.thePlayer.motionZ = this.savedMotionZ;
        }
        this.safeStuckTicks = 0;
        this.safeStuckDelayTicks = 0;
        this.safePrevMotionY = 0.0;
        this.safeStuckActive = false;
        this.eagleSneaking = false;
        this.eagleSneakTicks = 0;
        this.threeFmcAirTicks = 0;
        this.threeFmcGroundTicks = 0;
        this.threeFmcPlaceCooldown = 0;
    }

    public int getBlockCount() {
        return this.blockCount;
    }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;

        public BlockData(BlockPos blockPos, EnumFacing enumFacing) {
            this.blockPos = blockPos;
            this.facing = enumFacing;
        }

        public BlockPos blockPos() {
            return this.blockPos;
        }

        public EnumFacing facing() {
            return this.facing;
        }
    }

    @Override public void onPacketSendEvent(PacketSendEvent event) { PortBlink.capture(event); }
    @Override public void on2D(ScaledResolution resolution) {

        if (!blockCounter.isState() || mc.thePlayer == null) return;
        int total = 0;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (AuraSupport.isBlock(stack)) total += stack.stackSize;
        }

        RenderUtil.drawRoundedRect(resolution.getScaledWidth()/2f+12, resolution.getScaledHeight()/2f+12, 40, 25, 5, 0xDD151B24);

        if (mc.thePlayer.getHeldItem() != null) {
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(mc.thePlayer.getHeldItem(), (int) (resolution.getScaledWidth()/2f+34), (int) (resolution.getScaledHeight()/2f+17));
            RenderHelper.disableStandardItemLighting();
        }
        CustomFont.drawString(CustomFont.light(18), total + "x ", resolution.getScaledWidth()/2f+16, resolution.getScaledHeight()/2f+21, -1, 1);
    }

    @Override public void onMoveInput(MoveInputEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (rotationMode.getModeIndex() == 0) net.minecraft.entity.player.EntityPlayer.movementYaw = Float.NaN;
        mc.thePlayer.movementInput.moveForward = event.getForward();
        mc.thePlayer.movementInput.moveStrafe = event.getStrafe();
        mc.thePlayer.movementInput.jump = event.isJump();
        mc.thePlayer.movementInput.sneak = event.isSneak();
        adjustInput(event);
        event.setForward(mc.thePlayer.movementInput.moveForward);
        event.setStrafe(mc.thePlayer.movementInput.moveStrafe);
        event.setJump(mc.thePlayer.movementInput.jump);
        event.setSneak(mc.thePlayer.movementInput.sneak);
    }
    public net.minecraft.item.ItemStack renderStack(net.minecraft.item.ItemStack actual) {
        return itemSpoof.isState() && lastSlot >= 0
                ? mc.thePlayer.inventory.getStackInSlot(lastSlot) : actual;
    }
    public boolean scrollSlot(int delta) {
        if (lastSlot < 0) return false;
        lastSlot = Math.floorMod(lastSlot - Integer.signum(delta), 9);
        return true;
    }
}
