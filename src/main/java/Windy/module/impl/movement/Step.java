package Windy.module.impl.movement;

import Windy.event.impl.network.PacketReceiveEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.movement.MovementUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Timer;

@ModuleInfo(name = "Step", description = "Packet step, collision jump and extended packet strategies", category = Category.Movement)
public final class Step extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "NCP", "Jump", "NCP Extended", "Vulcan");
    private final NumberSetting height = new NumberSetting("Height", 1, .6, 1, .1);
    private final NumberSetting extendedHeight = new NumberSetting("Extended Height", 1, 1, 2.5, .1);
    private final NumberSetting stepTimer = new NumberSetting("Step timer", .5, .2, 1, .05);
    private final BooleanSetting correctionStop = new BooleanSetting("Disable on correction", true);
    private EntityPlayerSP owner;
    private float previousHeight, writtenHeight, oldTimer, writtenTimer;
    private boolean heightOwned, timerOwned, wasGround;
    private int restoreAt, lastJumpTick;
    private String lastMode;
    private long lastStep;
    private volatile boolean corrected;

    public Step() {
        addSettings(mode, height, extendedHeight, stepTimer, correctionStop);
        height.addParent(mode, s -> s.getCurrMode().equals("NCP") || s.getCurrMode().equals("Vulcan"));
        extendedHeight.addParent(mode, s -> s.getCurrMode().equals("NCP Extended"));
        stepTimer.addParent(mode, s -> packetMode(s.getCurrMode()));
        correctionStop.addParent(mode, s -> packetMode(s.getCurrMode()));
    }
    private static boolean packetMode(String value) { return value.equals("NCP Extended") || value.equals("Vulcan"); }
    @Override public void onEnable() {
        owner=null; heightOwned=timerOwned=false; corrected=false; lastStep=0; lastMode=mode.getCurrMode();
        trackPlayer();
    }
    private void restoreTimer() {
        if (timerOwned && Float.compare(Timer.timerSpeed,writtenTimer)==0) Timer.timerSpeed=oldTimer;
        timerOwned=false;
    }
    private void restoreHeight() {
        if (owner!=null && heightOwned && Float.compare(owner.stepHeight,writtenHeight)==0) owner.stepHeight=previousHeight;
        heightOwned=false;
    }
    private void trackPlayer() {
        if (owner==mc.thePlayer) return;
        restoreTimer();restoreHeight();owner=mc.thePlayer;
        if (owner!=null) {previousHeight=owner.stepHeight;wasGround=owner.onGround;lastJumpTick=owner.ticksExisted-12;}
        lastStep=0;corrected=false;
    }
    private void writeHeight(float value) {
        owner.stepHeight=writtenHeight=value;heightOwned=true;
    }
    private boolean usable() {
        return owner!=null && mc.theWorld!=null && !owner.isInWater() && !owner.isInLava()
                && !owner.isOnLadder() && !owner.isRiding() && !owner.capabilities.isFlying;
    }
    @Override public void onUpdate() {
        trackPlayer();
        if (owner==null || mc.theWorld==null) {restoreTimer();restoreHeight();return;}
        String current=mode.getCurrMode();
        if (!current.equals(lastMode)) {restoreTimer();lastMode=current;corrected=false;}
        if (corrected && packetMode(current) && correctionStop.isState()) {setEnabled(false);return;}
        if (timerOwned && owner.ticksExisted>=restoreAt) restoreTimer();
        if (wasGround && !owner.onGround) lastJumpTick=owner.ticksExisted;
        wasGround=owner.onGround;
        if (!usable()) {writeHeight(previousHeight);return;}
        if (current.equals("Jump")) {
            writeHeight(.6f);
            if (owner.onGround && owner.isCollidedHorizontally && MovementUtils.isMoving()
                    && System.currentTimeMillis()-lastStep>250) {
                owner.jump();lastStep=System.currentTimeMillis();
            }
        } else {
            boolean canStep=owner.onGround && !mc.gameSettings.keyBindJump.isKeyDown();
            if (current.equals("Vulcan") && owner.ticksExisted-lastJumpTick<=11) canStep=false;
            writeHeight(canStep ? (float)(current.equals("NCP Extended")?extendedHeight.getVal():height.getVal()) : .6f);
        }
    }

    public void onStep(double oldY, double stepDelta) {
        if (owner!=mc.thePlayer) trackPlayer();
        if (!usable() || stepDelta<=.625 || System.currentTimeMillis()-lastStep<100) return;
        String current=mode.getCurrMode();
        if (current.equals("Jump") || corrected && packetMode(current)) return;
        double limit=current.equals("NCP Extended") ? extendedHeight.getVal() : height.getVal();
        if (stepDelta>limit+.05 || !Double.isFinite(oldY) || !Double.isFinite(stepDelta)) return;
        lastStep=System.currentTimeMillis();
        double[] offsets;
        boolean ground=false;
        if (current.equals("Vulcan")) {
            offsets=new double[]{.5};ground=true;
        } else if (current.equals("NCP Extended")) {

            if (stepDelta>2.019) offsets=new double[]{.425,.821,.699,.599,1.022,1.372,1.652,1.869,2.019,1.919};
            else if (stepDelta>1.869) offsets=new double[]{.425,.821,.699,.599,1.022,1.372,1.652,1.869};
            else if (stepDelta>1.5) offsets=new double[]{.425,.821,.699,.599,1.022,1.372,1.652};
            else if (stepDelta>1.015) offsets=new double[]{.42,.7532,1.01,1.093,1.015};
            else if (stepDelta>.875) offsets=new double[]{.42,.7532};
            else offsets=new double[]{.39,.6938};
        } else {

            offsets=new double[]{.41999998688698,.7531999805212};
        }
        if (packetMode(current)) {
            if (!timerOwned) oldTimer=Timer.timerSpeed;
            timerOwned=true;Timer.timerSpeed=writtenTimer=(float)stepTimer.getVal();
            restoreAt=owner.ticksExisted+2;
        }
        for (double offset:offsets) owner.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(
                owner.posX,oldY+offset,owner.posZ,ground));
    }
    @Override public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (packetMode(mode.getCurrMode()) && mc.thePlayer!=null && mc.thePlayer.ticksExisted>20
                && event.getPacket() instanceof S08PacketPlayerPosLook) corrected=true;
    }
    @Override public void onDisable() {restoreTimer();restoreHeight();owner=null;corrected=false;}
}
