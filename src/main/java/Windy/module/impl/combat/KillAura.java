package Windy.module.impl.combat;

import Windy.event.Event;
import Windy.event.impl.*;
import Windy.event.impl.network.*;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.module.impl.movement.Scaffold;
import Windy.util.combat.AuraSupport;
import Windy.util.movement.ScaffoldSupport;
import Windy.util.network.PortBlink;
import Windy.util.rotation.PortRotation;
import Windy.util.rotation.PortRotations;
import Windy.util.time.TimerUtil;
import com.google.common.base.CaseFormat;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name="KillAura",description = "Automatically kills nearby entities", category=Category.Combat)
public class KillAura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final long ROTATION_UPDATE_INTERVAL_MS = 50;
    public final ModeSetting cpsMode;
    public final ModeSetting mode;
    public final ModeSetting sort;
    public ModeSetting autoBlock;

    public final BooleanSetting autoBlockRequirePress;
    public final NumberSetting swingRange;
    public final NumberSetting attackRange;
    public final NumberSetting fov;
    public final NumberSetting minCPS;
    public final NumberSetting maxCPS;
    public final NumberSetting switchDelay;
    public final ModeSetting rotations;

    public final BooleanSetting smoothBack;
    public final ModeSetting moveFix;
    public final NumberSetting smoothing;
    public final NumberSetting ravenSmoothing;
    public final NumberSetting ravenPredictTicks;
    public final NumberSetting ravenYawRandom;
    public final BooleanSetting throughWalls;
    public final BooleanSetting requirePress;
    public final BooleanSetting weaponsOnly;
    public final BooleanSetting inventoryCheck;
    public final BooleanSetting botCheck;
    public final BooleanSetting players;
    public final BooleanSetting mobs;
    public final BooleanSetting animals;
    public final BooleanSetting teams;
    public final ModeSetting debugLog;

    public final NumberSetting liquidBounceHorizontalSpeed;
    public final NumberSetting liquidBounceVerticalSpeed;
    public final NumberSetting liquidBounceSmoothFactor;
    public final BooleanSetting liquidBouncePredict;
    public final NumberSetting liquidBouncePredictSize;
    public final BooleanSetting liquidBounceRandomize;
    public final NumberSetting liquidBounceRandomizeRange;
    public final NumberSetting liquidBounceHorizontalSearch;
    public final NumberSetting liquidBounceBodyPointMin;
    public final NumberSetting liquidBounceBodyPointMax;
    private final int[] clickPattern = {16, 22, 14, 46, 18, 8, 8, 63, 25, 25, 12, 39, 26, 18, 6, 62, 26, 18, 21, 40, 26, 8, 16, 46, 26, 20, 15, 50, 25, 10, 11, 43, 25, 11, 37, 39, 25, 12, 18, 54, 25, 25, 15, 41, 27, 9, 1, 66, 26, 17, 21, 48, 27, 8, 6, 62, 28, 19, 13, 47, 26, 7, 14, 53, 27, 16, 29, 38, 27, 8, 6, 60, 27, 22, 19, 45, 26, 10, 10, 62, 25, 20, 28, 22, 26, 19, 11, 57, 26, 16, 32, 36, 26, 9, 9, 66, 27, 19, 27, 38, 26, 9, 10, 61, 26, 25, 15, 34, 26, 20, 10, 52, 26, 22, 28, 29, 27, 8, 3, 63, 26, 21, 27, 38, 26, 10, 11, 38, 27, 15, 31, 39, 25, 13, 10, 45, 27, 14, 27, 40, 26, 10, 6, 51, 26, 18, 31, 27, 27, 11, 14, 47, 26, 23, 21, 35, 26, 12, 13, 41, 26, 15, 31, 36, 27, 16, 9, 44, 27, 14, 30, 39, 25, 14, 10, 46, 28, 10, 24, 45, 26, 7, 5, 46, 26, 20, 6, 50, 26, 8, 6, 51, 26, 17, 20, 40, 27, 25, 1, 32, 26, 20, 9, 46, 25, 15, 12, 30, 26, 11, 25, 46, 27, 13, 10, 36, 27, 20, 15, 41, 26, 8, 6, 41, 26, 12, 29, 44, 26, 13, 11, 44, 26, 12, 27, 36, 26, 23, 4, 39, 26, 24, 12, 47, 26, 9, 2, 65, 26, 16, 27, 34, 26, 25, 0, 53, 26, 16, 3, 47, 27, 16, 10, 41, 26, 18, 25, 38, 26, 11, 10, 50, 27, 20, 20, 29, 26, 11, 7, 66, 26, 20, 18, 31, 26, 21, 21, 28, 26, 21, 29, 25, 27, 15, 12, 43, 28, 11, 31, 32, 27, 23, 0, 49, 27, 20, 30, 30, 25, 32, 0, 50, 26, 12, 25, 34, 27, 11, 11, 44, 27, 23, 26, 25, 27, 16, 11, 46, 26, 13, 32, 35, 28, 9, 5, 48, 26, 21, 29, 37, 26, 10, 7, 48, 27, 20, 21, 41, 24, 7, 18, 46, 25, 22, 22, 33, 25, 10, 5, 59, 26, 21, 19, 29, 26, 11, 10, 46, 25, 22, 29, 31, 25, 11, 12, 50, 24, 20, 28, 40, 25, 10, 4, 56, 25, 16, 36, 30, 24, 10, 9, 63, 25, 22, 22, 32, 25, 9, 8, 58, 27, 10, 43, 30, 26, 8, 3, 60, 26, 24, 14, 42, 26, 12, 9, 49, 25, 11, 32, 38, 27, 8, 8, 50, 26, 20, 26, 32, 25, 10, 4, 66, 25, 18, 28, 24, 26, 10, 8, 54, 25, 16, 32, 34, 24, 9, 12, 54, 25, 18, 18, 41, 28, 9, 16, 50, 28, 15, 21, 46, 27, 9, 8, 49, 26, 21, 18, 36, 26, 15, 10, 54, 27, 22, 27, 32, 25, 9, 15, 48, 28, 19, 26, 35, 27, 9, 13, 48, 25, 21, 23, 33, 27, 8, 3, 65, 26, 19, 23, 39, 25, 9, 13, 44, 26, 25, 19, 35, 26, 14, 6, 63, 27, 15, 23, 32, 28, 8, 2, 65, 26, 19, 24, 34, 27, 12, 0, 49, 26, 21, 34, 34, 26, 8, 9, 60, 26, 23, 19, 34, 26, 10, 5, 59, 26, 12, 36, 39, 26, 11, 11, 44, 26, 25, 5, 47, 25, 9, 10, 49, 27, 19, 24, 31, 26, 10, 4, 60, 27, 25, 9, 41, 26, 20, 7, 54, 24, 11, 35, 35, 26, 9, 5, 67, 26, 17, 19, 43, 26, 24, 17, 39, 25, 16, 11, 45, 25, 9, 3, 60, 25, 25, 16, 37, 28, 9, 5, 55, 26, 15, 12, 49, 25, 17, 8, 39, 25, 15, 16, 48, 25, 12, 9, 37, 25, 17, 31, 38, 27, 8, 8, 62, 26, 23, 14, 38, 27, 16, 10, 45, 26, 13, 25, 42, 25, 9, 8, 57, 27, 12, 36, 38, 27, 13, 11, 30, 27, 21, 24, 47, 25, 10, 6, 54, 26, 13, 28, 42, 25, 10, 5, 47, 26, 21, 22, 44, 26, 10, 8, 50, 28, 17, 26, 33, 26, 10, 14, 55, 27, 14, 30, 29, 25, 13, 1, 70, 26, 14, 30, 26, 27, 12, 14, 67, 25, 21, 4, 33, 25, 11, 5, 48, 26, 21, 21, 39, 25, 11, 1, 55, 26, 11, 29, 32, 26, 12, 10, 50, 27, 16, 26, 36, 27, 23, 3, 57, 27, 11, 23, 37, 26, 9, 16, 37, 26, 16, 38, 37, 26, 9, 2, 60, 27, 22, 16, 38, 27, 9, 5, 53, 26, 14, 33, 30, 25, 13, 11, 46, 25, 23, 22, 43, 24, 10, 13, 51, 25, 21, 25, 35, 27, 8, 16, 48, 25, 21, 19, 42, 25, 12, 12, 49, 26, 21, 18, 42, 25, 12, 13, 51, 27, 16, 25, 37, 26, 11, 12, 47, 27, 21, 13, 39, 27, 5, 9, 61, 25, 24, 11, 39, 26, 10, 9, 52, 26, 15, 33, 28, 38, 0, 9, 55, 26, 14, 39, 24, 25, 10, 9, 52, 27, 13, 29, 36, 25, 12, 9, 49, 25, 22, 30, 26, 26, 10, 2, 66, 27, 17, 30, 31, 26, 14, 7, 64, 28, 16, 31, 28, 24, 13, 14, 54, 25, 12, 29, 35, 27, 10, 8, 49, 27, 18, 26, 38, 25, 8, 14, 46, 26, 23, 15, 36, 26, 11, 5, 61, 27, 23, 8, 42, 25, 9, 10, 57, 26, 11, 29, 37, 25, 11, 9, 56, 27, 11, 32, 35, 26, 12, 6, 62, 27, 20, 33, 27, 27, 10, 14, 50, 27, 17, 28, 40, 25, 9, 8, 46, 26, 23, 16, 44, 26, 11, 13, 47, 28, 19, 19, 36, 26, 8, 7, 55, 26, 15, 24, 39, 26, 12, 9, 56, 26, 15, 28, 36, 25, 10, 10, 51, 25, 17, 32, 36, 25, 9, 7, 58, 26, 11, 31, 32, 26, 7, 14, 57, 26, 13, 22, 25, 24, 9, 14, 42, 26, 12, 27, 31, 25, 9, 2, 62, 27, 23, 12, 33, 26, 8, 18, 46, 25, 24, 14, 33, 24, 10, 14, 50, 25, 20, 21, 38, 26, 9, 1, 61, 25, 11, 30, 35, 26, 10, 10, 53, 25, 18, 22, 35, 25, 8, 4, 44, 25, 25, 21, 37, 24, 13, 6, 35, 27, 11, 34, 32, 25, 9, 10, 51, 26, 17, 18, 31, 24, 11, 8, 53, 26, 16, 30, 35, 26, 8, 10, 60, 25, 11, 32, 29, 25, 22, 2, 53, 26, 16, 30, 33, 27, 9, 11, 57, 25, 13, 32, 30, 25, 14, 10, 67, 24, 21, 29, 35, 27, 8, 12, 70, 26, 14, 19, 42, 27, 22, 0, 57, 27, 12, 31, 33, 25, 9, 12, 62, 27, 23, 14, 43, 25, 11, 2, 71, 28, 12, 33, 31, 27, 8, 12, 71, 26, 15, 23, 42, 28, 9, 8, 63, 26, 22, 22, 37, 27, 7, 4, 78, 27, 20, 26, 34, 25, 9, 15, 64, 27, 21, 23, 32, 26, 12, 11, 77, 25, 11, 32, 29, 26, 9, 15, 63, 27, 19, 23, 38, 26, 10, 15, 57, 26, 14, 37, 14, 26, 18, 6, 67, 26, 13, 31, 33, 26, 19, 1, 60, 27, 25, 22, 24, 27, 22, 2, 55, 26, 13, 25, 34, 26, 24, 0, 68, 25, 20, 22, 31, 25, 11, 4, 80, 24, 22, 22, 29, 26, 16, 8, 81, 25, 11, 22, 38, 27, 10, 11, 50, 27, 18, 35, 32, 26, 10, 5, 76, 26, 23, 22, 30, 24, 21, 8, 67, 27, 24, 16, 42, 27, 8, 3};
    private final TimerUtil timer = new TimerUtil();
    private final Random random = new Random();
    public AttackData attackData = null;
    public static EntityLivingBase target;
    public static Windy.setting.impl.NumberSetting range;
    private PortRotation serverRotation = new PortRotation(0, 0);
    private boolean isSmoothBacking = false;
    private boolean wantsToDisable = false;
    private Vec3 currentAimVec = null;
    private int patternIndex = 0;
    private int switchTick = 0;
    private boolean hitRegistered = false;
    private boolean blockingState = false;
    private boolean isBlocking = false;
    private boolean fakeBlockState = false;
    private int hypixel3Asw = 0;
    private boolean blinkReset = false;
    private long attackDelayMS = 0L;
    int blockTick = 0;
    private int lastTickProcessed;
    private long lastRotationUpdateTime = 0;

    public KillAura() {

        this.lastTickProcessed = 0;

        this.cpsMode = new ModeSetting("CPS Mode", 0, new String[]{"Normal", "Record"});

        this.mode = new ModeSetting("Mode", 1, new String[]{"Single", "Switch"});
        this.sort = new ModeSetting("Sort", 1, new String[]{"Distance", "Health", "HurtTime", "FOV"});
        this.autoBlock = new ModeSetting("auto-block", 3, new String[]{"NONE", "VANILLA", "SPOOF", "HYPIXEL", "BLINK", "INTERACT", "SWAP", "LEGIT", "FAKE", "Morden"});
        this.autoBlockRequirePress = new BooleanSetting("AutoBlockRequirePress", false);
        this.swingRange = new NumberSetting("SwingRange", 3.5F, 3.0F, 6.0F, 0.01);
        this.attackRange = new NumberSetting("AttackRange", 3.0F, 3.0F, 6.0F, 0.01);
        this.fov = new NumberSetting("FOV", 360, 30, 360, 1);
        this.minCPS = new NumberSetting("MinCPS", 14, 1, 20, 1);
        this.maxCPS = new NumberSetting("MaxCPS", 14, 1, 20, 1);
        this.switchDelay = new NumberSetting("SwitchDelay", 150, 0, 1000, 1);
        this.rotations = new ModeSetting("Rotations", 2, new String[]{"NONE", "Legit", "Silent", "LockView", "LiquidBounce", "Hypixel"});
        this.smoothBack = new BooleanSetting("SmoothBack", true);
        this.moveFix = new ModeSetting("MoveFix", 1, new String[]{"NONE", "Silent", "Strict"});
        this.smoothing = new NumberSetting("Smoothing", 0, 0, 100, 1);
        this.ravenSmoothing = new NumberSetting("HypixelSmoothing", 0, 0, 10, 1);
        this.ravenPredictTicks = new NumberSetting("HypixelPredict", 0, 0, 5, 1);
        this.ravenYawRandom = new NumberSetting("HypixelYawRandom", 0, 0, 5, 1);
        this.throughWalls = new BooleanSetting("ThroughWalls", true);
        this.requirePress = new BooleanSetting("RequirePress", false);
        this.weaponsOnly = new BooleanSetting("WeaponsOnly", true);
        this.inventoryCheck = new BooleanSetting("InventoryCheck", true);
        this.botCheck = new BooleanSetting("BotCheck", true);
        this.players = new BooleanSetting("Players", true);
        this.mobs = new BooleanSetting("Mobs", false);
        this.animals = new BooleanSetting("Animals", false);
        this.teams = new BooleanSetting("Teams", true);
        this.debugLog = new ModeSetting("Debug", 0, new String[]{"NONE", "Health"});

        this.liquidBounceHorizontalSpeed = new NumberSetting("LB-HSpeed", 180.0F, 1.0F, 180.0F, 0.01);
        this.liquidBounceVerticalSpeed = new NumberSetting("LB-VSpeed", 180.0F, 1.0F, 180.0F, 0.01);
        this.liquidBounceSmoothFactor = new NumberSetting("LB-Smooth", 0.5F, 0.1F, 1.0F, 0.01);
        this.liquidBouncePredict = new BooleanSetting("LB-Predict", true);
        this.liquidBouncePredictSize = new NumberSetting("LB-PredictSize", 1.0F, 0.0F, 3.0F, 0.01);
        this.liquidBounceRandomize = new BooleanSetting("LB-Randomize", true);
        this.liquidBounceRandomizeRange = new NumberSetting("LB-RandomRange", 0.5F, 0.0F, 1.0F, 0.01);
        this.liquidBounceHorizontalSearch = new NumberSetting("LB-HSearch", 0.5F, 0.0F, 1.0F, 0.01);
        this.liquidBounceBodyPointMin = new NumberSetting("LB-BodyMin", 0.1F, 0.0F, 1.0F, 0.01);
        this.liquidBounceBodyPointMax = new NumberSetting("LB-BodyMax", 0.9F, 0.0F, 1.0F, 0.01);
        smoothBack.addParent(smoothBack, ignored -> rotations.getModeIndex() == 4);
        ravenSmoothing.addParent(ravenSmoothing, ignored -> this.rotations.getModeIndex() == 5);
        ravenPredictTicks.addParent(ravenPredictTicks, ignored -> this.rotations.getModeIndex() == 5);
        ravenYawRandom.addParent(ravenYawRandom, ignored -> this.rotations.getModeIndex() == 5);
        liquidBounceHorizontalSpeed.addParent(liquidBounceHorizontalSpeed, ignored -> rotations.getModeIndex() == 4);
        liquidBounceVerticalSpeed.addParent(liquidBounceVerticalSpeed, ignored -> rotations.getModeIndex() == 4);
        liquidBounceSmoothFactor.addParent(liquidBounceSmoothFactor, ignored -> rotations.getModeIndex() == 4);
        liquidBouncePredict.addParent(liquidBouncePredict, ignored -> rotations.getModeIndex() == 4);
        liquidBouncePredictSize.addParent(liquidBouncePredictSize, ignored -> rotations.getModeIndex() == 4 && liquidBouncePredict.isState());
        liquidBounceRandomize.addParent(liquidBounceRandomize, ignored -> rotations.getModeIndex() == 4);
        liquidBounceRandomizeRange.addParent(liquidBounceRandomizeRange, ignored -> rotations.getModeIndex() == 4 && liquidBounceRandomize.isState());
        liquidBounceHorizontalSearch.addParent(liquidBounceHorizontalSearch, ignored -> rotations.getModeIndex() == 4);
        liquidBounceBodyPointMin.addParent(liquidBounceBodyPointMin, ignored -> rotations.getModeIndex() == 4);
        liquidBounceBodyPointMax.addParent(liquidBounceBodyPointMax, ignored -> rotations.getModeIndex() == 4);
        minCPS.addParent(cpsMode, setting -> setting.getModeIndex() == 0);
        maxCPS.addParent(cpsMode, setting -> setting.getModeIndex() == 0);
        switchDelay.addParent(mode, setting -> setting.getModeIndex() == 1);
        autoBlockRequirePress.addParent(autoBlock, setting -> setting.getModeIndex() != 0);
        smoothing.addParent(rotations, setting -> setting.getModeIndex() > 0 && setting.getModeIndex() < 4);
        addSettings(
                cpsMode, mode, sort, autoBlock, autoBlockRequirePress, swingRange, attackRange, fov, minCPS,
                maxCPS, switchDelay, rotations, smoothBack, moveFix, smoothing, ravenSmoothing, ravenPredictTicks,
                ravenYawRandom, throughWalls, requirePress, weaponsOnly, inventoryCheck, botCheck, players, mobs,
                animals, teams, debugLog,
                liquidBounceHorizontalSpeed, liquidBounceVerticalSpeed, liquidBounceSmoothFactor,
                liquidBouncePredict, liquidBouncePredictSize, liquidBounceRandomize, liquidBounceRandomizeRange, liquidBounceHorizontalSearch, liquidBounceBodyPointMin,
                liquidBounceBodyPointMax);
        range = attackRange;
    }

    private long getAttackDelay() {
            if (this.cpsMode.getModeIndex() == 1) {
                if (patternIndex >= clickPattern.length) {
                    patternIndex = 0;
                }
                return clickPattern[patternIndex];
            } else {
                return 1000L / ScaffoldSupport.nextLong(this.minCPS.getInt(), this.maxCPS.getInt());
            }
    }

    private boolean performAttack(float yaw, float pitch) {
        if (!AuraSupport.digging() && !AuraSupport.placing()) {
            if (this.isPlayerBlocking() && this.autoBlock.getModeIndex() != 1) {
                return false;
            } else if (this.attackDelayMS > 0L) {
                return false;
            } else {
                if (this.rotations.getModeIndex() == 4) {
                    MovingObjectPosition intercept = PortRotations.rayTrace(
                            this.attackData.getBox(),
                            yaw,
                            pitch,
                            this.attackRange.getFloat()
                    );
                    if (intercept == null) {
                        return false;
                    }
                }

                this.attackDelayMS += this.getAttackDelay();
                mc.thePlayer.swingItem();

                if (this.cpsMode.getModeIndex() == 1) {
                    patternIndex = (patternIndex + 1) % clickPattern.length;
                }

                if (this.rotations.getModeIndex() != 4 && this.rotations.getModeIndex() != 0) {
                    if (!this.isBoxInAttackRange(this.attackData.getBox())
                            && PortRotations.rayTrace(this.attackData.getBox(), yaw, pitch, this.attackRange.getFloat()) == null) {
                        return false;
                    }
                }
                mc.playerController.syncCurrentPlayItem();
                Windy.util.network.PacketUtils.sendPacket(new C02PacketUseEntity(this.attackData.getEntity(), Action.ATTACK));
                if (mc.playerController.getCurrentGameType() != GameType.SPECTATOR) {
                    AuraSupport.attackEntity(this.attackData.getEntity());
                }
                this.hitRegistered = true;
                return true;
            }
        } else {
            return false;
        }
    }

    private void sendUseItem() {
        mc.playerController.syncCurrentPlayItem();
        this.startBlock(mc.thePlayer.getHeldItem());
    }

    private void startBlock(ItemStack itemStack) {
        Windy.util.network.PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(itemStack));
        mc.thePlayer.setItemInUse(itemStack, itemStack.getMaxItemUseDuration());
        this.blockingState = true;
    }

    private void stopBlock() {
        Windy.util.network.PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
        this.blockingState = false;
    }

    private void interactAttack(float yaw, float pitch) {
        if (this.attackData != null) {
            MovingObjectPosition mop = PortRotations.rayTrace(this.attackData.getBox(), yaw, pitch, 8.0);
            if (mop != null) {
                mc.playerController.syncCurrentPlayItem();
                Windy.util.network.PacketUtils.sendPacket(
                        new C02PacketUseEntity(
                                this.attackData.getEntity(),
                                new Vec3(mop.hitVec.xCoord - this.attackData.getX(), mop.hitVec.yCoord - this.attackData.getY(), mop.hitVec.zCoord - this.attackData.getZ())
                        )
                );
                Windy.util.network.PacketUtils.sendPacket(new C02PacketUseEntity(this.attackData.getEntity(), Action.INTERACT));
                Windy.util.network.PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                this.blockingState = true;
            }
        }
    }


    private boolean canAttack() {
        return !(inventoryCheck.isState() && mc.currentScreen instanceof GuiContainer) && !AuraSupport.moduleEnabled("Scaffold")
                && !AuraSupport.digging() && !AuraSupport.placing()
                && !(mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())
                && (!weaponsOnly.isState() || AuraSupport.isHoldingSword() || AuraSupport.isHoldingTool())
                && (!requirePress.isState() || AuraSupport.isAttacking());
    }

    private boolean canAutoBlock() {
        if (!AuraSupport.isHoldingSword()) {
            return false;
        } else {
            return !this.autoBlockRequirePress.isState() || AuraSupport.isUsingItem();
        }
    }

    private boolean hasValidTarget() {
        return mc.theWorld
                .loadedEntityList
                .stream()
                .anyMatch(
                        entity -> entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                );
    }

    private boolean isValidTarget(EntityLivingBase entityLivingBase) {
        if (!mc.theWorld.loadedEntityList.contains(entityLivingBase)) {
            return false;
        } else if (entityLivingBase != mc.thePlayer && entityLivingBase != mc.thePlayer.ridingEntity) {
            if (entityLivingBase == mc.getRenderViewEntity() || entityLivingBase == mc.getRenderViewEntity().ridingEntity) {
                return false;
            } else if (entityLivingBase.deathTime > 0) {
                return false;
            } else if (PortRotations.angleToEntity(entityLivingBase) > this.fov.getFloat()) {
                return false;
            } else if (!this.throughWalls.isState() && PortRotations.rayTrace(entityLivingBase) != null) {
                return false;
            } else if (entityLivingBase instanceof EntityOtherPlayerMP) {
                if (!this.players.isState()) {
                    return false;
                } else if (AuraSupport.isFriend((EntityPlayer) entityLivingBase)) {
                    return false;
                } else {
                    return (!this.teams.isState() || !AuraSupport.isSameTeam((EntityPlayer) entityLivingBase)) && (!this.botCheck.isState() || !AuraSupport.isBot((EntityPlayer) entityLivingBase));
                }
            }  else if (!(entityLivingBase instanceof EntityMob) && !(entityLivingBase instanceof EntitySlime)) {
                if (entityLivingBase instanceof EntityAnimal
                        || entityLivingBase instanceof EntityBat
                        || entityLivingBase instanceof EntitySquid
                        || entityLivingBase instanceof EntityVillager) {
                    return this.animals.isState();
                } else if (!(entityLivingBase instanceof EntityIronGolem)) {
                    return false;
                } else {
                    return (!this.teams.isState() || !AuraSupport.hasTeamColor(entityLivingBase));
                }
            } else {
                return (!this.teams.isState() || !AuraSupport.hasTeamColor(entityLivingBase));
            }
        } else {
            return false;
        }
    }

    private boolean isInRange(EntityLivingBase entityLivingBase) {
       return this.isInSwingRange(entityLivingBase) || this.isInAttackRange(entityLivingBase);
    }


    private boolean isInSwingRange(EntityLivingBase entityLivingBase) {
        return PortRotations.distanceToEntity(entityLivingBase) <= (double) this.swingRange.getFloat();
    }

    private boolean isBoxInSwingRange(AxisAlignedBB axisAlignedBB) {
        return PortRotations.distanceToBox(axisAlignedBB) <= (double) this.swingRange.getFloat();
    }

    private boolean isInAttackRange(EntityLivingBase entityLivingBase) {
        return PortRotations.distanceToEntity(entityLivingBase) <= (double) this.attackRange.getFloat();
    }

    private boolean isBoxInAttackRange(AxisAlignedBB axisAlignedBB) {
        return PortRotations.distanceToBox(axisAlignedBB) <= (double) this.attackRange.getFloat();
    }

    private boolean isPlayerTarget(EntityLivingBase entityLivingBase) {
        return entityLivingBase instanceof EntityPlayer && AuraSupport.isTarget((EntityPlayer) entityLivingBase);
    }

    private int findEmptySlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && !stack.hasDisplayName()) {
                    return i;
                }
            }
        }
        return Math.floorMod(currentSlot - 1, 9);
    }

    private int findSwordSlot(int currentSlot) {
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
                ItemStack item = mc.thePlayer.inventory.getStackInSlot(i);
                if (item != null && item.getItem() instanceof ItemSword) {
                    return i;
                }
            }
        }
        return -1;
    }

    public EntityLivingBase getTarget() {
        return this.attackData != null ? this.attackData.getEntity() : null;
    }

    public boolean isAttackAllowed() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        Scaffold scaffold = (Scaffold) Windy.Windy.INSTANCE.getModuleManager().getModule(Scaffold.class);
        if (scaffold != null && scaffold.isToggled()) {
            return false;
        } else if (!this.weaponsOnly.isState()
                || AuraSupport.hasRawUnbreakingEnchant()
                || AuraSupport.isHoldingTool()) {
            return !this.requirePress.isState() || AuraSupport.isKeyDown(mc.gameSettings.keyBindAttack.getKeyCode());
        } else {
            return false;
        }
    }

    public boolean shouldAutoBlock() {
        if (this.isPlayerBlocking() && this.isBlocking) {
            return !mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() && (this.autoBlock.getModeIndex() == 3
                    || this.autoBlock.getModeIndex() == 4
                    || this.autoBlock.getModeIndex() == 5
                    || this.autoBlock.getModeIndex() == 6
                    || this.autoBlock.getModeIndex() == 7);
        } else {
            return false;
        }
    }

    public boolean isBlocking() {
        return this.fakeBlockState && AuraSupport.isHoldingSword();
    }

    public boolean isPlayerBlocking() {
        return (mc.thePlayer.isUsingItem() || this.blockingState) && AuraSupport.isHoldingSword();
    }

    private void processMotion(MotionEvent event, float previousYaw, float previousPitch) {
        if (mc.thePlayer == null || mc.theWorld == null) { target = null; return; }
        tickState(event);
        target = attackData == null ? null : attackData.getEntity();
        AuraSupport.serverYaw = previousYaw;
        if (event.getType() == Event.Type.POST && this.blinkReset) {
            this.blinkReset = false;
            PortBlink.set("AUTO_BLOCK", false);
            PortBlink.set("AUTO_BLOCK", true);
        }
        if (this.isToggled() || this.wantsToDisable) {
            if (event.getType() == Event.Type.PRE) {
                if (this.wantsToDisable) {
                    if (this.smoothBack.isState()) {
                        PortRotation currentRot = this.serverRotation;
                        PortRotation playerRot = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                            PortRotation nextRot = getSmoothBackRotation(currentRot, playerRot);
                            this.serverRotation = nextRot;
                            requestRotation(event, nextRot.yaw, nextRot.pitch);
                            if (this.moveFix.getModeIndex() != 0) {
                                net.minecraft.entity.player.EntityPlayer.movementYaw = nextRot.yaw;
                            }
                            mc.thePlayer.rotationYawHead = nextRot.yaw;
                            mc.thePlayer.renderYawOffset = nextRot.yaw;
                            return;
                        } else {
                            this.wantsToDisable = false;
                            this.setEnabled(false);
                            return;
                        }
                    } else {
                        this.wantsToDisable = false;
                        this.setEnabled(false);
                        return;
                    }
                }
                if (this.attackDelayMS > 0L) {
                    this.attackDelayMS -= 50L;
                }
                boolean attack = this.attackData != null && this.canAttack();
                boolean block = attack && this.canAutoBlock();
                if (!block) {
                    PortBlink.set("AUTO_BLOCK", false);
                    this.isBlocking = false;
                    this.fakeBlockState = false;
                    this.blockTick = 0;
                    this.hypixel3Asw = 0;
                }
                if (attack) {
                    boolean swap = false;
                    boolean blocked = false;
                    if (block) {
                        switch (this.autoBlock.getModeIndex()) {
                            case 0:
                                if (AuraSupport.isUsingItem()) {
                                    this.isBlocking = true;
                                    if (!this.isPlayerBlocking() && !AuraSupport.digging() && !AuraSupport.placing()
                                            && !(mc.objectMouseOver != null
                                            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                        swap = true;
                                    }
                                } else {
                                    this.isBlocking = false;
                                    if (this.isPlayerBlocking() && !AuraSupport.digging() && !AuraSupport.placing()
                                            && !(mc.objectMouseOver != null
                                            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                        this.stopBlock();
                                    }
                                }
                                PortBlink.set("AUTO_BLOCK", false);
                                this.fakeBlockState = false;
                                break;
                            case 1:
                                if (this.hasValidTarget()) {
                                    if (!this.isPlayerBlocking() && !AuraSupport.digging() && !AuraSupport.placing()
                                            && !(mc.objectMouseOver != null
                                            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                        swap = true;
                                    }
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 2:
                                if (this.hasValidTarget()) {
                                    int item = AuraSupport.getServerSlot();
                                    if (AuraSupport.digging() || AuraSupport.placing() || mc.thePlayer.inventory.currentItem != item || this.isPlayerBlocking() && this.blockTick != 0 || this.attackDelayMS > 0L && this.attackDelayMS <= 50L) {
                                        this.blockTick = 0;
                                    } else {
                                        int slot = this.findEmptySlot(item);
                                        Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
                                        Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(item));
                                        swap = true;
                                        this.blockTick = 1;
                                    }
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 3:
                                if (this.hasValidTarget()) {
                                    if (!AuraSupport.digging() && !AuraSupport.placing()) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                blocked = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    if (AuraSupport.moduleEnabled("NoSlow")) {
                                                        int randomSlot = new Random().nextInt(9);
                                                        while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                                            randomSlot = new Random().nextInt(9);
                                                        }
                                                        Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                                        Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                                                    }
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 4:
                                if (this.hasValidTarget()) {
                                    if (!AuraSupport.digging() && !AuraSupport.placing()) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blinkReset = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 5:
                                if (this.hasValidTarget()) {
                                    int item = AuraSupport.getServerSlot();
                                    if (mc.thePlayer.inventory.currentItem == item && !AuraSupport.digging() && !AuraSupport.placing()
                                            && !(mc.objectMouseOver != null
                                            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blinkReset = true;
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    int slot = this.findEmptySlot(item);
                                                    Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(slot));
                                                    AuraSupport.setServerSlot(slot);
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 6:
                                if (this.hasValidTarget()) {
                                    int item = AuraSupport.getServerSlot();
                                    if (mc.thePlayer.inventory.currentItem == item && !AuraSupport.digging() && !AuraSupport.placing()
                                            && !(mc.objectMouseOver != null
                                            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                        switch (this.blockTick) {
                                            case 0:
                                                int slot = this.findSwordSlot(item);
                                                if (slot != -1) {
                                                    if (!this.isPlayerBlocking()) {
                                                        swap = true;
                                                    }
                                                    this.blockTick = 1;
                                                }
                                                break;
                                            case 1:
                                                int swordsSlot = this.findSwordSlot(item);
                                                if (swordsSlot == -1) {
                                                    this.blockTick = 0;
                                                } else if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                } else if (this.attackDelayMS <= 50L) {
                                                    Windy.util.network.PacketUtils.sendPacket(new C09PacketHeldItemChange(swordsSlot));
                                                    AuraSupport.setServerSlot(swordsSlot);
                                                    this.startBlock(mc.thePlayer.inventory.getStackInSlot(swordsSlot));
                                                    attack = false;
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                        PortBlink.set("AUTO_BLOCK", false);
                                        this.isBlocking = true;
                                        this.fakeBlockState = true;
                                        break;
                                    }
                                }
                                PortBlink.set("AUTO_BLOCK", false);
                                this.isBlocking = false;
                                this.fakeBlockState = false;
                                break;
                            case 7:
                                if (this.hasValidTarget()) {
                                    if (!AuraSupport.digging() && !AuraSupport.placing()) {
                                        switch (this.blockTick) {
                                            case 0:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                this.blockTick = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                    attack = false;
                                                }
                                                if (this.attackDelayMS <= 50L) {
                                                    this.blockTick = 0;
                                                }
                                                break;
                                            default:
                                                this.blockTick = 0;
                                        }
                                    }
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = true;
                                    this.fakeBlockState = false;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                }
                                break;
                            case 9:

                                if (this.hasValidTarget()) {
                                    PortBlink.set("AUTO_BLOCK", true);
                                    if (!AuraSupport.digging() && !AuraSupport.placing()) {
                                        switch (this.hypixel3Asw) {
                                            case 0:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                }
                                                attack = false;
                                                this.hypixel3Asw = 1;
                                                break;
                                            case 1:
                                                if (this.isPlayerBlocking()) {
                                                    this.stopBlock();
                                                }
                                                attack = false;
                                                this.hypixel3Asw = 2;
                                                break;
                                            case 2:
                                                if (!this.isPlayerBlocking()) {
                                                    swap = true;
                                                }
                                                blocked = true;
                                                this.hypixel3Asw = 0;
                                                break;
                                            default:
                                                this.hypixel3Asw = 0;
                                        }
                                    } else {
                                        attack = false;
                                    }
                                    this.isBlocking = true;
                                    this.fakeBlockState = true;
                                } else {
                                    PortBlink.set("AUTO_BLOCK", false);
                                    this.isBlocking = false;
                                    this.fakeBlockState = false;
                                    this.hypixel3Asw = 0;
                                }
                                break;
                            case 8:
                                PortBlink.set("AUTO_BLOCK", false);
                                this.isBlocking = false;
                                this.fakeBlockState = this.hasValidTarget();
                                if (AuraSupport.isUsingItem() && !this.isPlayerBlocking() && !AuraSupport.digging() && !AuraSupport.placing()
                                        && !(mc.objectMouseOver != null
                                        && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && AuraSupport.isAttacking())) {
                                    swap = true;
                                }
                        }
                    }
                    boolean attacked = false;
                    if (this.rotations.getModeIndex() == 4 && this.smoothBack.isState() && (this.attackData == null || !this.isBoxInSwingRange(this.attackData.getBox()))) {
                        PortRotation currentRot = this.serverRotation;
                        PortRotation playerRot = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                            this.isSmoothBacking = true;
                            PortRotation nextRot = getSmoothBackRotation(currentRot, playerRot);

                            float[] fixed = PortRotations.gcd(new float[]{nextRot.yaw, nextRot.pitch}, new float[]{currentRot.yaw, currentRot.pitch});
                            nextRot = new PortRotation(fixed[0], fixed[1]);

                            this.serverRotation = nextRot;
                            requestRotation(event, nextRot.yaw, nextRot.pitch);
                            if (this.moveFix.getModeIndex() != 0) {
                                net.minecraft.entity.player.EntityPlayer.movementYaw = nextRot.yaw;
                            }
                        } else {
                            this.isSmoothBacking = false;
                            this.serverRotation = playerRot;
                        }
                    }
                    if (this.attackData != null && this.isBoxInSwingRange(this.attackData.getBox())) {
                        if (this.rotations.getModeIndex() == 4) {
                            PortRotation currentRot = this.serverRotation;
                            if (Float.isNaN(currentRot.yaw) || Float.isNaN(currentRot.pitch)) {
                                currentRot = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                            }

                            PortRotation nextRot = updateLiquidBounceRotation(currentRot);

                            float[] fixed = PortRotations.gcd(new float[]{nextRot.yaw, nextRot.pitch}, new float[]{currentRot.yaw, currentRot.pitch});
                            nextRot = new PortRotation(fixed[0], fixed[1]);

                            this.serverRotation = nextRot;
                            updateRenderAimPosition(nextRot);
                            requestRotation(event, nextRot.yaw, nextRot.pitch);
                            if (this.moveFix.getModeIndex() != 0) {
                                net.minecraft.entity.player.EntityPlayer.movementYaw = nextRot.yaw;
                            }
                            mc.thePlayer.rotationYawHead = nextRot.yaw;
                            mc.thePlayer.renderYawOffset = nextRot.yaw;
                            if (attack) {
                                attacked = this.performAttack(nextRot.yaw, nextRot.pitch);
                            }
                        } else if (this.rotations.getModeIndex() == 5) {

                            PortRotation currentRot = this.serverRotation;
                            if (Float.isNaN(currentRot.yaw) || Float.isNaN(currentRot.pitch)) {
                                currentRot = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                            }
                            float[] raw = this.getRavenRotations(this.attackData.getEntity());
                            float[] fixedRot = this.ravenFixRotation(raw[0], raw[1], currentRot.yaw, currentRot.pitch);
                            float[] smoothed = this.getRavenRotationsSmoothed(fixedRot, currentRot.yaw, currentRot.pitch);
                            float finalYaw = smoothed[0];
                            float finalPitch = smoothed[1];
                            if (finalPitch > 90) finalPitch = 90;
                            if (finalPitch < -90) finalPitch = -90;
                            this.serverRotation = new PortRotation(finalYaw, finalPitch);
                            requestRotation(event, finalYaw, finalPitch);
                            mc.thePlayer.rotationYawHead = finalYaw;
                            mc.thePlayer.renderYawOffset = finalYaw;
                            if (this.moveFix.getModeIndex() != 0) {
                                net.minecraft.entity.player.EntityPlayer.movementYaw = finalYaw;
                            }
                            if (attack) {
                                attacked = this.performAttack(event.getYaw(), event.getPitch());
                            }
                        } else if (this.rotations.getModeIndex() >= 1) {
                            float randomYaw = ScaffoldSupport.nextFloat(-2.5F, 2.5F);
                            float randomPitch = ScaffoldSupport.nextFloat(-1.5F, 1.5F);
                            float[] rotations = PortRotations.getRotationsToBox(
                                    this.attackData.getBox(),
                                    previousYaw,
                                    previousPitch,
                                    (float) 90 + ScaffoldSupport.nextFloat(-5.0F, 5.0F),
                                    (float) this.smoothing.getInt() / 100.0F
                            );
                            float finalYaw = rotations[0] + randomYaw;
                            float finalPitch = rotations[1] + randomPitch;

                            // GCD FIX
                            float[] fixed = PortRotations.gcd(new float[]{finalYaw, finalPitch}, new float[]{previousYaw, previousPitch});
                            finalYaw = fixed[0];
                            finalPitch = fixed[1];

                            if (finalPitch > 90) finalPitch = 90;
                            if (finalPitch < -90) finalPitch = -90;
                            requestRotation(event, finalYaw, finalPitch);
                            if (this.rotations.getModeIndex() == 3) {
                                requestRotation(event, finalYaw, finalPitch);
                            } else {
                                mc.thePlayer.rotationYawHead = finalYaw;
                                mc.thePlayer.renderYawOffset = finalYaw;
                            }
                            if (this.moveFix.getModeIndex() != 0 || this.rotations.getModeIndex() == 3) {
                                net.minecraft.entity.player.EntityPlayer.movementYaw = finalYaw;
                            }
                            if (attack) {
                                attacked = this.performAttack(event.getYaw(), event.getPitch());
                            }
                        } else {
                            if (attack) {
                                attacked = this.performAttack(event.getYaw(), event.getPitch());
                            }
                        }
                    }
                    if (swap) {
                        if (attacked) {
                            this.interactAttack(event.getYaw(), event.getPitch());
                        } else {
                            this.sendUseItem();
                        }
                    }
                    if (blocked) {
                        PortBlink.set("AUTO_BLOCK", false);
                        PortBlink.set("AUTO_BLOCK", true);
                    }
                }
            }
        }
    }


    private float[] getRavenRotations(EntityLivingBase entity) {
        double posX = entity.posX;
        double posZ = entity.posZ;
        int ticks = this.ravenPredictTicks.getInt();
        if (ticks > 0) {
            double dX = entity.posX - entity.lastTickPosX;
            double dZ = entity.posZ - entity.lastTickPosZ;
            for (int i = 0; i < ticks; i++) {
                posX += dX;
                posZ += dZ;
            }
        }
        double deltaX = posX - mc.thePlayer.posX;
        double deltaZ = posZ - mc.thePlayer.posZ;
        double deltaY = entity.posY + entity.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        float yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(
                (float) (Math.atan2(deltaZ, deltaX) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw);
        float pitch = MathHelper.clamp_float(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(
                (float) (-(Math.atan2(deltaY, MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f, -90.0f, 90.0f);
        return new float[]{yaw, pitch};
    }

    private int ravenRandInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + this.random.nextInt(max - min + 1);
    }


    private float[] ravenFixRotation(float targetYaw, float targetPitch, float yaw, float pitch) {
        float n5 = targetYaw - yaw;
        float abs = Math.abs(n5);
        float n7 = targetPitch - pitch;
        float n8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        double n9 = n8 * n8 * n8 * 1.2;
        float n10 = (float) (Math.round((double) n5 / n9) * n9);
        float n11 = (float) (Math.round((double) n7 / n9) * n9);
        targetYaw = yaw + n10;
        targetPitch = pitch + n11;
        if (abs >= 1.0f) {
            int factor = this.ravenYawRandom.getInt();
            if (factor != 0) {
                int n13 = factor * 100 + ravenRandInt(-30, 30);
                targetYaw += ravenRandInt(-n13, n13) / 100.0f;
            }
        } else if (abs <= 0.04f) {
            targetYaw += (abs > 0.0f) ? 0.01f : -0.01f;
        }
        return new float[]{targetYaw, MathHelper.clamp_float(targetPitch, -90.0f, 90.0f)};
    }


    private float[] getRavenRotationsSmoothed(float[] rotations, float serverYaw, float serverPitch) {
        float unwrappedYaw = serverYaw + ((((rotations[0] - serverYaw + 180f) % 360f) + 360f) % 360f - 180f);
        float deltaYaw = unwrappedYaw - serverYaw;
        float deltaPitch = rotations[1] - serverPitch;

        float yawSmoothing = (float) this.ravenSmoothing.getInt();
        float pitchSmoothing = yawSmoothing;

        float strafe = mc.thePlayer.moveStrafing;
        if (strafe < 0 && deltaYaw < 0 || strafe > 0 && deltaYaw > 0) {
            yawSmoothing = Math.max(1f, yawSmoothing / 2f);
        }

        float motionY = (float) mc.thePlayer.motionY;
        if (motionY > 0 && deltaPitch > 0 || motionY < 0 && deltaPitch < 0) {
            pitchSmoothing = Math.max(1f, pitchSmoothing / 2f);
        }

        serverYaw += deltaYaw / Math.max(1f, yawSmoothing);
        serverPitch += deltaPitch / Math.max(1f, pitchSmoothing);

        return new float[]{serverYaw, serverPitch};
    }

    private PortRotation updateLiquidBounceRotation(PortRotation current) {
        if (this.attackData == null || System.currentTimeMillis() - this.lastRotationUpdateTime < ROTATION_UPDATE_INTERVAL_MS) {
            return current;
        }

        this.lastRotationUpdateTime = System.currentTimeMillis();
        EntityLivingBase targetEntity = this.attackData.getEntity();

        AxisAlignedBB bb = targetEntity.getEntityBoundingBox().expand(targetEntity.getCollisionBorderSize(), targetEntity.getCollisionBorderSize(), targetEntity.getCollisionBorderSize());
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0F);

        Vec3 targetPoint = searchCenterPoint(bb, eyes);

        if (targetPoint == null) {
            targetPoint = new Vec3(
                    (bb.minX + bb.maxX) / 2.0,
                    (bb.minY + bb.maxY) / 2.0,
                    (bb.minZ + bb.maxZ) / 2.0
            );
        }

        if (this.liquidBouncePredict.isState()) {
            targetPoint = applyPrediction(targetPoint, targetEntity);
        }

        double diffX = targetPoint.xCoord - eyes.xCoord;
        double diffY = targetPoint.yCoord - eyes.yCoord;
        double diffZ = targetPoint.zCoord - eyes.zCoord;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float targetPitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / Math.PI));

        targetYaw = MathHelper.wrapAngleTo180_float(targetYaw);
        targetPitch = MathHelper.wrapAngleTo180_float(targetPitch);

        return limitAngleChange(current, new PortRotation(targetYaw, targetPitch));
    }

    private Vec3 searchCenterPoint(AxisAlignedBB bb, Vec3 eyes) {
        double scanRange = Math.max(this.attackRange.getFloat(), this.swingRange.getFloat());
        double attackRange = this.attackRange.getFloat();
        double throughWallsRange = this.throughWalls.isState() ? attackRange : 0;

        double minBody = this.liquidBounceBodyPointMin.getFloat();
        double maxBody = this.liquidBounceBodyPointMax.getFloat();
        double hMin = 0.0;
        double hMax = this.liquidBounceHorizontalSearch.getFloat();

        Vec3 bestPoint = null;
        double bestScore = Double.MAX_VALUE;

        for (double x = hMin; x <= hMax; x += 0.25) {
            for (double y = minBody; y <= maxBody; y += 0.25) {
                for (double z = hMin; z <= hMax; z += 0.25) {
                    Vec3 point = new Vec3(
                            bb.minX + (bb.maxX - bb.minX) * x,
                            bb.minY + (bb.maxY - bb.minY) * y,
                            bb.minZ + (bb.maxZ - bb.minZ) * z
                    );

                    double distance = eyes.distanceTo(point);

                    if (distance > scanRange) {
                        continue;
                    }

                    boolean visible = isVisible(eyes, point);
                    if (!visible && distance > throughWallsRange) {
                        continue;
                    }

                    double score = distance;
                    if (distance > attackRange) {
                        score += 10.0;
                    }
                    if (!visible) {
                        score += 5.0;
                    }

                    if (this.liquidBounceRandomize.isState()) {
                        score += random.nextDouble() * this.liquidBounceRandomizeRange.getFloat() * 5.0;
                    }

                    if (score < bestScore) {
                        bestScore = score;
                        bestPoint = point;
                    }
                }
            }
        }

        return bestPoint;
    }

    private boolean isVisible(Vec3 from, Vec3 to) {
        if (this.throughWalls.isState()) {
            return true;
        }
        return mc.theWorld.rayTraceBlocks(from, to, false, true, false) == null;
    }

    private Vec3 applyPrediction(Vec3 point, EntityLivingBase entity) {
        double predictX = entity.posX + entity.motionX * this.liquidBouncePredictSize.getFloat();
        double predictY = entity.posY + entity.motionY * this.liquidBouncePredictSize.getFloat() * 0.5;
        double predictZ = entity.posZ + entity.motionZ * this.liquidBouncePredictSize.getFloat();

        double offsetX = point.xCoord - entity.posX;
        double offsetY = point.yCoord - entity.posY;
        double offsetZ = point.zCoord - entity.posZ;

        return new Vec3(predictX + offsetX, predictY + offsetY, predictZ + offsetZ);
    }

    private PortRotation limitAngleChange(PortRotation current, PortRotation attackData) {
        float maxHorizontalChange = this.liquidBounceHorizontalSpeed.getFloat();
        float maxVerticalChange = this.liquidBounceVerticalSpeed.getFloat();
        float smoothFactor = this.liquidBounceSmoothFactor.getFloat();

        float yawDiff = MathHelper.wrapAngleTo180_float(attackData.yaw - current.yaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(attackData.pitch - current.pitch);

        yawDiff = MathHelper.clamp_float(yawDiff, -maxHorizontalChange, maxHorizontalChange);
        pitchDiff = MathHelper.clamp_float(pitchDiff, -maxVerticalChange, maxVerticalChange);

        yawDiff *= smoothFactor;
        pitchDiff *= smoothFactor;

        return new PortRotation(
                current.yaw + yawDiff,
                MathHelper.clamp_float(current.pitch + pitchDiff, -90.0f, 90.0f)
        );
    }

    private void updateRenderAimPosition(PortRotation rotation) {
        if (rotation == null || mc.thePlayer == null) {
            this.currentAimVec = null;
            return;
        }
        float yawRad = rotation.yaw * (float) Math.PI / 180.0F;
        float pitchRad = rotation.pitch * (float) Math.PI / 180.0F;
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0F);
        double renderDistance = Math.max(0.1, mc.thePlayer.getDistanceToEntity(this.attackData.getEntity()) * 0.5);
        renderDistance = Math.min(renderDistance, 4.0);
        this.currentAimVec = new Vec3(
                eyePos.xCoord + lookX * renderDistance,
                eyePos.yCoord + lookY * renderDistance,
                eyePos.zCoord + lookZ * renderDistance
        );
    }

    private PortRotation getSmoothBackRotation(PortRotation current, PortRotation attackData) {
        float yawDiff = MathHelper.wrapAngleTo180_float(attackData.yaw - current.yaw);
        float pitchDiff = MathHelper.wrapAngleTo180_float(attackData.pitch - current.pitch);
        float speed = Math.max(5.0f, Math.abs(yawDiff) * 0.3f);
        float yawStep = MathHelper.clamp_float(yawDiff, -speed, speed);
        float pitchStep = MathHelper.clamp_float(pitchDiff, -speed, speed);
        return new PortRotation(current.yaw + yawStep, current.pitch + pitchStep);
    }

    public void tickState(MotionEvent event) {
        if (this.isToggled() || this.wantsToDisable) {
            switch (event.getType()) {
                case PRE:
                    if (this.attackData == null && !this.isSmoothBacking && !this.wantsToDisable) {
                        this.serverRotation = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                        this.currentAimVec = null;
                    }
                    ArrayList<EntityLivingBase> validTargets = new ArrayList<>();
                    boolean needsNewTarget = false;
                    if (this.attackData == null
                            || !this.isValidTarget(this.attackData.getEntity())
                            || !this.isBoxInAttackRange(this.attackData.getBox())
                            || !this.isBoxInSwingRange(this.attackData.getBox())
                            || this.timer.hasTimeElapsed(this.switchDelay.getInt())) {
                        needsNewTarget = true;
                        this.timer.reset();
                    }
                    for (Entity entity : mc.theWorld.loadedEntityList) {
                        if (entity instanceof EntityLivingBase
                                && this.isValidTarget((EntityLivingBase) entity)
                                && this.isInRange((EntityLivingBase) entity)) {
                            validTargets.add((EntityLivingBase) entity);
                        }
                    }
                    if (validTargets.isEmpty()) {
                        this.attackData = null;
                        this.currentAimVec = null;
                    } else {
                        if (validTargets.stream().anyMatch(this::isInSwingRange)) {
                            validTargets.removeIf(entityLivingBase -> !this.isInSwingRange(entityLivingBase));
                        }
                        if (validTargets.stream().anyMatch(this::isInAttackRange)) {
                            validTargets.removeIf(entityLivingBase -> !this.isInAttackRange(entityLivingBase));
                        }
                        if (validTargets.stream().anyMatch(this::isPlayerTarget)) {
                            validTargets.removeIf(entityLivingBase -> !this.isPlayerTarget(entityLivingBase));
                        }
                        validTargets.sort(
                                (entityLivingBase1, entityLivingBase2) -> {
                                    int sortBase = 0;
                                    switch (this.sort.getModeIndex()) {
                                        case 1:
                                            sortBase = Float.compare(AuraSupport.getHealthScore(entityLivingBase1), AuraSupport.getHealthScore(entityLivingBase2));
                                            break;
                                        case 2:
                                            sortBase = Integer.compare(entityLivingBase1.hurtResistantTime, entityLivingBase2.hurtResistantTime);
                                            break;
                                        case 3:
                                            sortBase = Float.compare(
                                                    PortRotations.angleToEntity(entityLivingBase1),
                                                    PortRotations.angleToEntity(entityLivingBase2)
                                            );
                                    }
                                    return sortBase != 0
                                            ? sortBase
                                            : Double.compare(PortRotations.distanceToEntity(entityLivingBase1), PortRotations.distanceToEntity(entityLivingBase2));
                                }
                        );
                        if (needsNewTarget) {
                            if (this.mode.getModeIndex() == 1 && this.hitRegistered) {
                                this.hitRegistered = false;
                                this.switchTick++;
                            }
                            if (this.mode.getModeIndex() == 0 || this.switchTick >= validTargets.size()) {
                                this.switchTick = 0;
                            }
                            EntityLivingBase newTarget = validTargets.get(this.switchTick);
                            boolean targetChanged = this.attackData == null || this.attackData.getEntity() != newTarget;
                            if (targetChanged) {
                                this.attackData = new AttackData(newTarget, this);
                            }
                        }
                    }
                    if (this.attackData != null) {
                        this.attackData.update();
                    }
                    break;
                case POST:
                    if (this.isPlayerBlocking() && !mc.thePlayer.isBlocking()) {
                        mc.thePlayer.setItemInUse(mc.thePlayer.getHeldItem(), mc.thePlayer.getHeldItem().getMaxItemUseDuration());
                    }
            }
        }
    }

    public void inspectPacket(Windy.event.impl.network.PacketSendEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (this.isToggled() && !event.isCancelled()) {
            if (event.getPacket() instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging packet = (C07PacketPlayerDigging) event.getPacket();
                if (packet.getStatus() == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                    this.blockingState = false;
                }
            }
            if (event.getPacket() instanceof C09PacketHeldItemChange) {
                this.blockingState = false;
                if (this.isBlocking) {
                    mc.thePlayer.stopUsingItem();
                }
            }
            if (this.debugLog.getModeIndex() == 1 && this.isAttackAllowed()) {
                if (event.getPacket() instanceof S06PacketUpdateHealth) {
                    float packet = ((S06PacketUpdateHealth) event.getPacket()).getHealth() - mc.thePlayer.getHealth();
                    if (packet != 0.0F && this.lastTickProcessed != mc.thePlayer.ticksExisted) {
                        this.lastTickProcessed = mc.thePlayer.ticksExisted;
                        AuraSupport.logFormatted(
                                String.format(
                                        "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                                        "Windy ",
                                        packet > 0.0F ? "&a" : "&c",
                                        df.format(packet),
                                        mc.thePlayer.ticksExisted
                                )
                        );
                    }
                }
                if (event.getPacket() instanceof S1CPacketEntityMetadata) {
                    S1CPacketEntityMetadata packet = (S1CPacketEntityMetadata) event.getPacket();
                    if (packet.getEntityId() == mc.thePlayer.getEntityId()) {
                        for (WatchableObject watchableObject : packet.func_149376_c()) {
                            if (watchableObject.getDataValueId() == 6) {
                                float diff = (Float) watchableObject.getObject() - mc.thePlayer.getHealth();
                                if (diff != 0.0F && this.lastTickProcessed != mc.thePlayer.ticksExisted) {
                                    this.lastTickProcessed = mc.thePlayer.ticksExisted;
                                    AuraSupport.logFormatted(
                                            String.format(
                                                    "%sHealth: %s&l%s&r (&otick: %d&r)&r",
                                                    "Windy ",
                                                    diff > 0.0F ? "&a" : "&c",
                                                    df.format(diff),
                                                    mc.thePlayer.ticksExisted
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void onMoveInput(MoveInputEvent event) {
        if (this.isToggled() || this.wantsToDisable) {
            boolean isSilent = this.rotations.getModeIndex() == 2;
            boolean isLiquidBounce = this.rotations.getModeIndex() == 4;
            boolean isRavenBS = this.rotations.getModeIndex() == 5;
            if (this.moveFix.getModeIndex() != 0 && (isSilent || isLiquidBounce || isRavenBS)) {
                if (true && 1.0F == 1.0F && ScaffoldSupport.isForwardPressed()) {
                    ScaffoldSupport.fixStrafe(AuraSupport.serverYaw);
                }
            }
            if (this.shouldAutoBlock()) {
                event.setJump(false);
            }
        }
    }




    @Override
    public void onEnable() {
        if (mc.thePlayer == null) { target = null; return; }
        if (!this.wantsToDisable) {
            this.attackData = null;
            this.switchTick = 0;
            this.hitRegistered = false;
            this.attackDelayMS = 0L;
            this.blockTick = 0;
            this.serverRotation = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            this.currentAimVec = null;
            this.isSmoothBacking = false;
            this.lastRotationUpdateTime = 0;
            this.patternIndex = 0;
        }
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null) {
            target = null;
            attackData = null;
            PortBlink.set("AUTO_BLOCK", false);
            return;
        }
        target = null;
        net.minecraft.entity.player.EntityPlayer.movementYaw = Float.NaN;
        if (this.rotations.getModeIndex() == 4 && this.smoothBack.isState()) {
            PortRotation currentRot = this.serverRotation;
            PortRotation playerRot = new PortRotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            if (Math.abs(MathHelper.wrapAngleTo180_float(currentRot.yaw - playerRot.yaw)) > 1.0F || Math.abs(MathHelper.wrapAngleTo180_float(currentRot.pitch - playerRot.pitch)) > 1.0F) {
                this.wantsToDisable = true;
                this.setEnabled(true);
                return;
            }
        }
        PortBlink.set("AUTO_BLOCK", false);
        this.blockingState = false;
        this.isBlocking = false;
        this.fakeBlockState = false;
        this.wantsToDisable = false;
        this.currentAimVec = null;
    }

    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getCurrMode())};
    }

    @Getter
    public static class AttackData {
        private final EntityLivingBase entity;
        private final KillAura killAura;
        private AxisAlignedBB box;
        private double x;
        private double y;
        private double z;

        public AttackData(EntityLivingBase entityLivingBase, KillAura killAura) {
            this.entity = entityLivingBase;
            this.killAura = killAura;
            update();
        }

        public void update() {
            double collisionBorderSize = entity.getCollisionBorderSize();
            this.box = entity.getEntityBoundingBox().expand(collisionBorderSize, collisionBorderSize, collisionBorderSize);
            this.x = entity.posX;
            this.y = entity.posY;
            this.z = entity.posZ;
        }
    }

    public static boolean isVisualBlocking() {
        Windy.module.Module module = Windy.Windy.INSTANCE.getModuleManager().getModule(KillAura.class);
        return module instanceof KillAura && module.isToggled() && (((KillAura)module).fakeBlockState || ((KillAura)module).isBlocking);
    }
    @Override public void onPacketSendEvent(PacketSendEvent event) {
        inspectPacket(event);
        PortBlink.capture(event);
    }
    @Override public void onPacketReceiveEvent(PacketReceiveEvent event) {
        inspectPacket(new PacketSendEvent(event.getPacket()));
    }

    public boolean blocksManualAction() {
        return isToggled() && (isBlocking || attackData != null && canAttack());
    }

    private boolean rotationRequested;

    private void requestRotation(MotionEvent event, float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        event.setYaw(yaw);
        event.setPitch(MathHelper.clamp_float(pitch, -90, 90));
        rotationRequested = true;
    }


    public boolean prepareMotion(MotionEvent event, float previousYaw, float previousPitch) {
        rotationRequested = false;
        if (mc.thePlayer == null || mc.theWorld == null) { target = null; return false; }
        net.minecraft.entity.player.EntityPlayer.movementYaw = Float.NaN;
        try {
            processMotion(event, previousYaw, previousPitch);
        } finally {
            AuraSupport.serverYaw = rotationRequested ? event.getYaw() : previousYaw;
            target = attackData == null ? null : attackData.getEntity();
        }
        return rotationRequested;
    }

    @Override public void onMotionEvent(MotionEvent event) {

        if (event.getType() != Event.Type.POST) return;
        if (mc.thePlayer == null || mc.theWorld == null) { target = null; return; }
        try {
            processMotion(event, mc.thePlayer.windyServerYaw, mc.thePlayer.windyServerPitch);
        } finally {
            target = attackData == null ? null : attackData.getEntity();
        }
    }
}
