package Windy.module.impl.movement;

import Windy.event.impl.MotionEvent;
import Windy.event.impl.StrafeEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.movement.MovementUtils;
import Windy.util.time.TimerUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Packet;
import net.minecraft.util.Timer;
import org.lwjgl.input.Keyboard;
import static org.lwjgl.input.Keyboard.*;

@ModuleInfo(
        name = "Speed",
        description = "Makes you faster",
        category = Category.Movement,
        enabled = false
)

public final class Speed extends Module {

    private boolean damage;
    private boolean strafe, wasOnGround;
    private final TimerUtil timerUtil = new TimerUtil();

    private final List<Packet<?>> packetList = new ArrayList<>();

    private double speed;
    private double blocksSpeed;
    private int blocksAirTicks;
    private int vulcanStage;
    private double vulcanMoveSpeed;
    private boolean downmotioncustominit = false;
    private float lastMovementYaw;


    public final NumberSetting speedcustom = new NumberSetting("Speed", 0.5, 0.1, 10, 0.1);
    public final NumberSetting ymotioncustom = new NumberSetting("Y Motion", 0.42, 0, 10, 0.1);
    public final NumberSetting downmotioncustom = new NumberSetting("Down motion", -0.7543454, -10, -0.1, 0.1);
    public final NumberSetting timercustom = new NumberSetting("Timer", 1, 0.1, 10, 0.1);
    public final BooleanSetting downmotiontoggle = new BooleanSetting("Down Motion", false);
    public final NumberSetting ncpboost = new NumberSetting("Boost", 0.5, 0.1, 0.65, 0.05);
    public final NumberSetting verusboost = new NumberSetting("Boost", 1, 0.1, 10, 0.1);
    public final BooleanSetting verusboostbool = new BooleanSetting("Use boost", true);
    private final ModeSetting verus = new ModeSetting("Type", "Strafe", "Funny");
    private final ModeSetting vulcan = new ModeSetting("Type", "Semi Strafe", "Ground");
    private final ModeSetting mode = new ModeSetting("Mode", "BHop", "Strafe", "Verus", "Vulcan", "NCP", "Grim", "Legit", "Custom");

    public Speed() {
        addSettings(mode, speedcustom, ymotioncustom, downmotiontoggle, downmotioncustom, timercustom, vulcan, ncpboost, verus, verusboost, verusboostbool);
        downmotioncustom.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Custom"));
        speedcustom.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Custom"));
        downmotiontoggle.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Custom"));
        ymotioncustom.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Custom"));
        timercustom.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Custom"));
        verus.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Verus"));
        vulcan.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Vulcan"));
        ncpboost.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("NCP"));
        verusboost.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Verus"));
        verusboostbool.addParent(mode, modeSetting -> mode.getCurrMode().equalsIgnoreCase("Verus"));

    }

    @Override
    public void onMotionEvent(MotionEvent e) {
        super.onMotionEvent(e);
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (e.isPre()) {
            if (!mode.getCurrMode().equals("Custom")) Timer.timerSpeed = 1f;
            switch (mode.getCurrMode()) {
                case "BHop": {
                    if (MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) mc.thePlayer.jump();
                        MovementUtils.setSpeed(Math.max(0.36, MovementUtils.getSpeed()));
                    }
                    break;
                }
                case "Strafe": {
                    if (MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) mc.thePlayer.jump();
                        MovementUtils.setSpeed(Math.max(0.29, MovementUtils.getSpeed()));
                    }
                    break;
                }

                case "Vulcan": {
                    switch (vulcan.getCurrMode()) {
                        case "Semi Strafe": {
                            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                                MovementUtils.setSpeed(MovementUtils.getAllowedHDistNCP());
                                mc.thePlayer.motionY = 0.42f;
                            } else if (mc.thePlayer.hurtTime == 9) {
                                MovementUtils.setSpeed(MovementUtils.getSpeed());
                            }
                            if (Keyboard.isKeyDown(KEY_SPACE)) {
                                toggle();
                            }
                            break;
                        }
                        case "Ground": {

                            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                                mc.thePlayer.motionX *= 0.75;
                                mc.thePlayer.motionZ *= 0.75;
                            }
                            if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                                double groundSpeed = Math.hypot(mc.thePlayer.motionX, mc.thePlayer.motionZ);
                                boolean potionBoost = mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSpeed)
                                        && mc.thePlayer.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier() >= 1;
                                switch (vulcanStage) {
                                    case 1:
                                        vulcanMoveSpeed = 0.58;
                                        groundSpeed = potionBoost ? groundSpeed + 0.2 : 0.487;
                                        e.setOnGround(true);
                                        break;
                                    case 2:
                                        groundSpeed = potionBoost ? groundSpeed * 0.71 : 0.197;
                                        vulcanMoveSpeed -= 0.0784;
                                        e.setOnGround(false);
                                        break;
                                    default:
                                        vulcanStage = 0;
                                        groundSpeed /= potionBoost ? 0.64 : 0.58;
                                        e.setOnGround(true);
                                        break;
                                }
                                MovementUtils.setSpeed(groundSpeed);
                                vulcanStage++;
                                e.setY(e.getY() + vulcanMoveSpeed);
                            } else {
                                vulcanStage = 0;
                                vulcanMoveSpeed = 0;
                            }
                            break;
                        }
                    }
                    break;
                }

                case "Legit": {
                    if (MovementUtils.isMoving() && mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    break;
                }

                case "Verus": {
                    switch (verus.getCurrMode()) {
                        case "Strafe": {
                            mc.thePlayer.cameraYaw = 0f;
                            if (mc.thePlayer.hurtTime >= 1) {
                                MovementUtils.setSpeed((float) verusboost.getVal() + MovementUtils.getSpeed());
                            }
                            if (MovementUtils.isMoving()) {
                                if (mc.thePlayer.onGround) {
                                    mc.thePlayer.motionY = 0.42;
                                    wasOnGround = true;
                                } else if (wasOnGround) {
                                    if (!mc.thePlayer.isCollidedHorizontally) {
                                        mc.thePlayer.motionY = -0.0784000015258789;
                                    }
                                    wasOnGround = false;
                                }
                                MovementUtils.setSpeed(0.33);
                            } else {
                                mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
                            }
                            break;
                        }

                        case "Funny": {
                            if (mc.gameSettings.keyBindJump.isKeyDown() && MovementUtils.isMoving()) {
                                MovementUtils.setSpeed(0.1);
                            } else if (!mc.gameSettings.keyBindJump.isKeyDown() && MovementUtils.isMoving()) {
                                MovementUtils.setSpeed(0.32);
                            }
                            if (mc.thePlayer.hurtTime == 9) {
                                MovementUtils.setSpeed(MovementUtils.getSpeed() + (float) verusboost.getVal());
                            }
                            if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                                mc.thePlayer.jump();
                            }
                            break;
                        }
                    }
                    break;
                }

                case "Grim": {
                    if (mc.thePlayer.onGround && MovementUtils.isMoving() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.thePlayer.jump();
                    }
                    break;
                }


                case "NCP": {
                    if (mc.thePlayer.onGround && MovementUtils.isMoving()) {
                        mc.thePlayer.motionY = 0.42f;
                        Timer.timerSpeed = 1f;
                        MovementUtils.setSpeed(MovementUtils.getAllowedHDistNCP());
                    } else if (mc.thePlayer.hurtTime == 9) {
                        MovementUtils.setSpeed(MovementUtils.getSpeed() + (float) ncpboost.getVal());
                    }
                    break;
                }

                case "Custom": {
                    if (MovementUtils.isMoving()) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump();
                            if (downmotiontoggle.isState()) {
                                mc.thePlayer.motionY = (float) downmotioncustom.getVal();
                            } else {
                                mc.thePlayer.motionY = (float) ymotioncustom.getVal();
                            }
                        }
                        Timer.timerSpeed = (float) timercustom.getVal();
                        MovementUtils.setSpeed((float) speedcustom.getVal());
                    } else {
                        Timer.timerSpeed = 1f;
                    }
                    break;
                }

            }
        }
    }

    @Override
    public void onStrafe(StrafeEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || !mode.getCurrMode().equals("BlocksMC")) return;

        double base = 0.2873;
        boolean potion = mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSpeed);
        if (potion) base *= 1 + 0.2 * (mc.thePlayer.getActivePotionEffect(
                net.minecraft.potion.Potion.moveSpeed).getAmplifier() + 1);
        if (!MovementUtils.isMoving()) {
            blocksSpeed = base;
            blocksAirTicks = 0;
            mc.thePlayer.motionX = mc.thePlayer.motionZ = 0;
            return;
        }
        if (mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42;
            if (mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.jump))
                mc.thePlayer.motionY += 0.1 * (mc.thePlayer.getActivePotionEffect(
                        net.minecraft.potion.Potion.jump).getAmplifier() + 1);
            blocksSpeed = base * (potion ? 1.4 : 2.15);
            blocksAirTicks = 0;
        } else if (blocksAirTicks++ == 0) {
            blocksSpeed -= 0.8 * (blocksSpeed - base);
        } else {
            blocksSpeed -= blocksSpeed / 159.9;
        }
        if (mc.thePlayer.isCollidedHorizontally) blocksSpeed = base;
        MovementUtils.setSpeed(Math.max(blocksSpeed, base));
        event.setFriction(0);
    }
    @Override
    public void onEnable () {
        downmotioncustominit = false;
        vulcanStage = 0;
        vulcanMoveSpeed = 0;
        blocksSpeed = 0;
        blocksAirTicks = 0;
        if (mc.thePlayer == null) {
            return;
        }
        damage = false;
        packetList.clear();
        lastMovementYaw = -1;
        super.onEnable();
    }

    @Override
    public void onDisable () {
        downmotioncustominit = false;
        if (!packetList.isEmpty()) {
            packetList.forEach(mc.getNetHandler()::addToSendQueueNoEvent);
            packetList.clear();
        }
        Timer.timerSpeed = 1;
        super.onDisable();
    }
}
