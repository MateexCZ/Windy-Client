package Windy.module.impl.movement;

import Windy.event.impl.network.PacketReceiveEvent;
import Windy.event.impl.MotionEvent;
import Windy.event.impl.StrafeEvent;
import Windy.setting.impl.BooleanSetting;
import net.minecraft.util.Timer;
import net.minecraft.potion.Potion;
import Windy.event.impl.network.PacketSendEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.chat.ChatUtil;
import Windy.util.movement.MovementUtils;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;


@ModuleInfo(name = "LongJump", description = "Jump long", category = Category.Movement)
public final class LongJump extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Fireball", "Fireball Manual", "Fireball High", "Fireball Flat", "NCP", "Vulcan", "Verus Fireball");
    private final NumberSetting boost = new NumberSetting("Boost", 1.4, 1, 3, 0.05);
    private final NumberSetting ncpGround = new NumberSetting("NCP ground speed", .4, .1, 3, .05);
    private final NumberSetting ncpAir = new NumberSetting("NCP jump speed", 1.4, .1, 3, .05);
    private final NumberSetting ncpFriction = new NumberSetting("NCP friction", 159.9, 59, 259, 1);
    private final NumberSetting ncpGlide = new NumberSetting("NCP glide", 0, 0, 3, .5);
    private final NumberSetting ncpTimer = new NumberSetting("NCP timer", 1, .2, 2, .05);
    private final BooleanSetting ignoreFirst = new BooleanSetting("Vulcan ignore first correction", true);
    private final NumberSetting verusSpeed = new NumberSetting("Verus burst speed", 9.6, .3, 9.6, .1);
    private final NumberSetting verusHeight = new NumberSetting("Verus burst height", 1, .42, 4, .1);
    private boolean launched, jumping, timerOwned;
    private volatile boolean corrected, ignoreCorrection;
    private float oldTimer, writtenTimer;
    private int airTicks;
    private double moveSpeed;
    private String runningMode;
    private int ticks;
    private long startTime, launchTime;
    public LongJump() {
        addSettings(mode, boost, ncpGround, ncpAir, ncpFriction, ncpGlide, ncpTimer, ignoreFirst, verusSpeed, verusHeight);
        boost.addParent(mode, s -> s.getCurrMode().startsWith("Fireball"));
        ncpGround.addParent(mode, s -> s.getCurrMode().equals("NCP"));
        ncpAir.addParent(mode, s -> s.getCurrMode().equals("NCP"));
        ncpFriction.addParent(mode, s -> s.getCurrMode().equals("NCP"));
        ncpGlide.addParent(mode, s -> s.getCurrMode().equals("NCP"));
        ncpTimer.addParent(mode, s -> s.getCurrMode().equals("NCP"));
        ignoreFirst.addParent(mode, s -> s.getCurrMode().equals("Vulcan"));
        verusSpeed.addParent(mode, s -> s.getCurrMode().equals("Verus Fireball"));
        verusHeight.addParent(mode, s -> s.getCurrMode().equals("Verus Fireball"));
    }

    private int fireballSlot() {
        if (mc.thePlayer == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() instanceof ItemFireball) return slot;
        }
        return -1;
    }
    private boolean manual() { return mode.getCurrMode().equals("Fireball Manual"); }
    @Override public void onEnable() {
        restoreTimer();
        runningMode = mode.getCurrMode();
        ignoreCorrection = false;
        airTicks = 0;
        moveSpeed = 0;
        launched = jumping = corrected = false;
        ticks = 0;
        startTime = System.currentTimeMillis();
        if (kinetic()) {
            if (mc.thePlayer != null && !mc.thePlayer.onGround) {
                ChatUtil.warning("This experimental LongJump must start on the ground.");
                setEnabled(false);
            }
            return;
        }
        if (mc.thePlayer != null && !manual() && fireballSlot() < 0) {
            ChatUtil.warning("LongJump requires a fireball in the hotbar.");
            setEnabled(false);
        }
    }
    @Override public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) { restoreTimer(); return; }
        if (!mode.getCurrMode().equals(runningMode)) { onEnable(); return; }
        if (corrected) { setEnabled(false); return; }
        if (kinetic()) {
            if (mode.getCurrMode().equals("NCP")) ticks++;
            if (mode.getCurrMode().equals("Vulcan") && ticks >= 7) restoreTimer();
            if (System.currentTimeMillis() - startTime > 5000
                    || launched && mc.thePlayer.onGround && ticks > (mode.getCurrMode().equals("Vulcan") ? 7 : 3)) setEnabled(false);
            return;
        }
        if (!manual() && !launched) launch();
        if (!jumping) {
            if (System.currentTimeMillis() - startTime > 1500) {
                if (launched) ChatUtil.warning("LongJump received no fireball knockback.");
                setEnabled(false);
            }
            return;
        }
        ticks++;
        if (mode.getCurrMode().equals("Verus Fireball")) {

            mc.thePlayer.motionY = verusHeight.getVal();
            MovementUtils.setSpeed(verusSpeed.getVal());
            setEnabled(false);
            return;
        }
        if (ticks == 2 && MovementUtils.isMoving())
            MovementUtils.setSpeed(MovementUtils.getSpeed() * boost.getVal());
        if (mode.getCurrMode().equals("Fireball High")) {
            if (ticks == 1) mc.thePlayer.motionY *= 0.75;
            else if (ticks <= 30) {
                double motion = mc.thePlayer.motionY / 0.98 + 0.055;
                if (motion > 0) mc.thePlayer.motionY = motion;
            }
        } else if (mode.getCurrMode().equals("Fireball Flat")) {
            if (ticks == 1) mc.thePlayer.motionY *= 0.75;
            else if (ticks <= 30) mc.thePlayer.motionY = 0.01 + ticks * 0.003;
        }
        if (ticks >= 30 || (mc.thePlayer.onGround && ticks > 3)) setEnabled(false);
    }
    private void launch() {
        int slot = fireballSlot();
        if (slot < 0) { setEnabled(false); return; }
        int saved = mc.thePlayer.inventory.currentItem;
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C05PacketPlayerLook(mc.thePlayer.rotationYaw, 89.0f, mc.thePlayer.onGround));
        mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(stack));
        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(saved));
        launched = true;
        launchTime = System.currentTimeMillis();
    }
    @Override public void onPacketSendEvent(PacketSendEvent event) {
        if (manual() && event.getPacket() instanceof C08PacketPlayerBlockPlacement
                && mc.thePlayer != null && mc.thePlayer.getHeldItem() != null
                && mc.thePlayer.getHeldItem().getItem() instanceof ItemFireball) {
            launched = true;
            launchTime = System.currentTimeMillis();
        }
    }
    @Override public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            if (mode.getCurrMode().equals("Vulcan") && ignoreFirst.isState() && ignoreCorrection
                    && System.currentTimeMillis() - launchTime <= 750) {
                ignoreCorrection = false;
                event.setCancelled(true);
                return;
            }
            corrected = true;
            return;
        }
        if (kinetic()) return;
        if (!launched || jumping || System.currentTimeMillis() - launchTime > 1000 || mc.thePlayer == null) return;
        if (event.getPacket() instanceof S12PacketEntityVelocity
                && ((S12PacketEntityVelocity)event.getPacket()).getEntityID() == mc.thePlayer.getEntityId()
                || event.getPacket() instanceof S27PacketExplosion) {
            jumping = true;
            ticks = 0;
        }
    }
    private boolean kinetic() {
        return mode.getCurrMode().equals("NCP") || mode.getCurrMode().equals("Vulcan");
    }
    private void timer(float speed) {
        if (!timerOwned) {oldTimer=Timer.timerSpeed;timerOwned=true;}
        Timer.timerSpeed=writtenTimer=speed;
    }
    private void restoreTimer() {
        if (timerOwned && Float.compare(Timer.timerSpeed,writtenTimer)==0) Timer.timerSpeed=oldTimer;
        timerOwned=false;
    }
    @Override public void onStrafe(StrafeEvent event) {
        if (!mode.getCurrMode().equals("NCP") || mc.thePlayer==null || mc.theWorld==null || corrected) return;
        if (!MovementUtils.isMoving()) return;
        double base=.221*(mc.thePlayer.isSprinting()?1.3:1);
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) base*=1+.2*(mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).getAmplifier()+1);
        if (mc.thePlayer.onGround) {
            if (launched) {setEnabled(false);return;}
            mc.thePlayer.motionY=.42;
            if (mc.thePlayer.isPotionActive(Potion.jump)) mc.thePlayer.motionY+=.1*(mc.thePlayer.getActivePotionEffect(Potion.jump).getAmplifier()+1);
            moveSpeed=ncpGround.getVal();launched=true;airTicks=0;ticks=0;
        } else {
            if (!launched) return;
            if (++airTicks==1) moveSpeed=ncpAir.getVal();
            else moveSpeed-=moveSpeed/ncpFriction.getVal();
        }
        if (mc.thePlayer.fallDistance>0) mc.thePlayer.motionY+=ncpGlide.getVal()/100;
        if (mc.thePlayer.isCollidedHorizontally) moveSpeed=base;
        timer((float)ncpTimer.getVal());
        mc.thePlayer.motionX*=.0005;mc.thePlayer.motionZ*=.0005;
        event.setFriction((float)(Math.max(moveSpeed,base)*(event.getForward()!=0 && event.getStrafe()!=0?.98:1)));
    }
    @Override public void onMotionEvent(MotionEvent event) {
        if (!event.isPre() || !mode.getCurrMode().equals("Vulcan")
                || mc.thePlayer==null || mc.theWorld==null || corrected) return;
        ticks++;
        if (mc.thePlayer.fallDistance>0 && ticks%2==0 && mc.thePlayer.fallDistance<2.2) mc.thePlayer.motionY+=.14f;
        switch (ticks) {
            case 1:
                launched=true;launchTime=System.currentTimeMillis();ignoreCorrection=ignoreFirst.isState();
                timer(.5f);
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ,mc.thePlayer.onGround));
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,mc.thePlayer.posY-.0784000015258789,mc.thePlayer.posZ,mc.thePlayer.onGround));
                mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX,mc.thePlayer.posY,mc.thePlayer.posZ,false));
                MovementUtils.setSpeed(7.9);mc.thePlayer.motionY=.42f;
                break;
            case 2: mc.thePlayer.motionY+=.1f;MovementUtils.setSpeed(2.79);break;
            case 3: MovementUtils.setSpeed(2.56);break;
            case 4: event.setOnGround(true);mc.thePlayer.onGround=true;MovementUtils.setSpeed(.49);break;
            case 5: MovementUtils.setSpeed(.59);break;
            case 6: mc.thePlayer.motionX=mc.thePlayer.motionZ=0;MovementUtils.setSpeed(.3);break;
            default: break;
        }
    }
    @Override public void onDisable() {
        restoreTimer();
        if (runningMode!=null && (runningMode.equals("NCP") || runningMode.equals("Vulcan")) && mc.thePlayer!=null)
            mc.thePlayer.motionX=mc.thePlayer.motionZ=0;
        launched=jumping=corrected=ignoreCorrection=false;ticks=airTicks=0;moveSpeed=0;
    }
}