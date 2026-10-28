package Windy.module.impl.movement;

import Windy.event.impl.MotionEvent;
import Windy.event.impl.StrafeEvent;
import Windy.event.impl.network.PacketReceiveEvent;
import Windy.event.impl.network.PacketSendEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.Timer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;


@ModuleInfo(name = "Fly", description = "Makes the player fly and experimental legacy strategies", category = Category.Movement)
public final class Flight extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "OpenMyau", "NCP Legacy", "Verus Legacy", "Vulcan Burst", "Grim TNT");
    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", 1, .1, 5, .1);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 1, .1, 5, .1);
    private final BooleanSetting correctionStop = new BooleanSetting("Disable on correction", true);
    private final BooleanSetting verusTimer = new BooleanSetting("Verus timer pulse", false);
    private final NumberSetting grimDistance = new NumberSetting("Grim packet distance", 500, 1, 500, 1);
    private final NumberSetting grimTicks = new NumberSetting("Grim impulse ticks", 20, 1, 100, 1);
    private double verticalMotion, sentX, sentY, sentZ;
    private EntityPlayerSP player;
    private String sessionMode;
    private int ticks, packets, armedAt;
    private long lastSent;
    private boolean initialized, setupPacket, sneakOwned, timerOwned, impulse;
    private float oldTimer, writtenTimer;
    private volatile boolean corrected, pendingImpulse;

    public Flight() {
        addSettings(mode, horizontalSpeed, verticalSpeed, correctionStop, verusTimer, grimDistance, grimTicks);
        horizontalSpeed.addParent(mode, s -> s.getCurrMode().equals("OpenMyau"));
        verticalSpeed.addParent(mode, s -> s.getCurrMode().equals("OpenMyau"));
        correctionStop.addParent(mode, s -> !s.getCurrMode().equals("OpenMyau"));
        verusTimer.addParent(mode, s -> s.getCurrMode().equals("Verus Legacy"));
        grimDistance.addParent(mode, s -> s.getCurrMode().equals("Grim TNT"));
        grimTicks.addParent(mode, s -> s.getCurrMode().equals("Grim TNT"));
    }
    private boolean pressed(KeyBinding binding) {
        int key=binding.getKeyCode();
        return key<0 ? Mouse.isButtonDown(key+100) : key>0 && Keyboard.isKeyDown(key);
    }
    @Override public void onEnable() {
        initialized=false; corrected=pendingImpulse=false; ticks=packets=0; impulse=false;
        if (mc.thePlayer!=null && mc.theWorld!=null) ready();
    }
    private void restoreOwnedState() {
        if (timerOwned && Float.compare(Timer.timerSpeed,writtenTimer)==0) Timer.timerSpeed=oldTimer;
        timerOwned=false;
        if (sneakOwned && mc.currentScreen==null)
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(),pressed(mc.gameSettings.keyBindSneak));
        sneakOwned=false;
    }
    private void timer(float value) {
        if (!timerOwned) {oldTimer=Timer.timerSpeed;timerOwned=true;}
        Timer.timerSpeed=writtenTimer=value;
    }
    private boolean ready() {
        if (mc.thePlayer==null || mc.theWorld==null) {restoreOwnedState();initialized=false;return false;}
        if (!initialized || player!=mc.thePlayer || !mode.getCurrMode().equals(sessionMode)) {
            restoreOwnedState();
            player=mc.thePlayer; sessionMode=mode.getCurrMode();
            ticks=packets=0; impulse=false; corrected=pendingImpulse=false;
            sentX=player.posX;sentY=player.posY;sentZ=player.posZ;
            lastSent=System.nanoTime(); initialized=true;
            if (sessionMode.equals("Vulcan Burst")) {
                setupPacket=true;
                try {player.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(
                        player.posX,player.posY-2,player.posZ,player.rotationYaw,player.rotationPitch,false));}
                finally {setupPacket=false;}
            }
        }
        if (corrected && correctionStop.isState() && !sessionMode.equals("OpenMyau")) {
            setEnabled(false);return false;
        }
        return true;
    }
    @Override public void onUpdate() {
        if (!ready()) return;
        ticks++;
        if (sessionMode.equals("OpenMyau")) {
            verticalMotion=0;
            if (mc.currentScreen==null) {
                if (pressed(mc.gameSettings.keyBindJump)) verticalMotion+=verticalSpeed.getVal()*.42;
                if (pressed(mc.gameSettings.keyBindSneak)) verticalMotion-=verticalSpeed.getVal()*.42;
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(),false);
                sneakOwned=true;
            }
            player.fallDistance=0;
        } else if (sessionMode.equals("Vulcan Burst") && (packets>=8 || ticks>160)) {
            setEnabled(false);
        } else if (sessionMode.equals("Grim TNT")) {
            if (pendingImpulse) {pendingImpulse=false;impulse=true;armedAt=ticks;}
            if (impulse && ticks-armedAt>grimTicks.getVal()) setEnabled(false);
        }
    }
    @Override public void onStrafe(StrafeEvent event) {
        if (!ready() || event.isCancelled() || sessionMode.equals("Grim TNT")) return;
        double base=.28015;
        if (player.isPotionActive(Potion.moveSpeed))
            base*=1+.15*(player.getActivePotionEffect(Potion.moveSpeed).getAmplifier()+1);
        double speed=base;
        switch (sessionMode) {
            case "OpenMyau":

                player.motionY=verticalMotion;
                speed*=horizontalSpeed.getVal();
                break;
            case "Verus Legacy": speed=.2555; break;
            case "Vulcan Burst": speed=1; break;
            default: break;
        }
        player.motionX=player.motionZ=0;
        event.setFriction((float)(speed*(event.getForward()!=0 && event.getStrafe()!=0 ? .98 : 1)));
    }
    @Override public void onMotionEvent(MotionEvent event) {
        if (!event.isPre() || !ready()) return;
        switch (sessionMode) {
            case "NCP Legacy":

                player.motionY=0;
                event.setY(event.getY()+.00001+(player.ticksExisted%2==0 ? .000001 : -.000001));
                break;
            case "Verus Legacy":

                player.motionY=mc.currentScreen==null && mc.gameSettings.keyBindJump.isKeyDown()
                        && player.ticksExisted%2==0 ? .42 : 0;
                event.setOnGround(true);
                if (verusTimer.isState()) timer(ticks%10==0 ? .1f : 5f);
                else if (timerOwned) restoreOwnedState();
                break;
            case "Vulcan Burst":
                player.motionY=-1E-10;
                if (mc.currentScreen==null) {
                    if (mc.gameSettings.keyBindJump.isKeyDown()) player.motionY+=1;
                    if (mc.gameSettings.keyBindSneak.isKeyDown()) player.motionY-=1;
                }
                break;
            case "Grim TNT":
                if (impulse) {
                    double yaw=Windy.util.movement.MovementUtils.direction(player.rotationYaw,
                            player.movementInput.moveForward,player.movementInput.moveStrafe);
                    event.setX(player.posX-Math.sin(yaw)*grimDistance.getVal());
                    event.setZ(player.posZ+Math.cos(yaw)*grimDistance.getVal());

                    event.setY(player.posY+(player.motionY-.08)*.98);
                }
                break;
            default: break;
        }
    }
    @Override public void onPacketSendEvent(PacketSendEvent event) {
        if (!initialized || !sessionMode.equals("Vulcan Burst") || setupPacket
                || corrected || event.isCancelled() || !(event.getPacket() instanceof C03PacketPlayer)) return;
        C03PacketPlayer packet=(C03PacketPlayer)event.getPacket();
        double x=packet.isMoving()?packet.getPositionX():sentX;
        double y=packet.isMoving()?packet.getPositionY():sentY;
        double z=packet.isMoving()?packet.getPositionZ():sentZ;
        double distance=Math.sqrt((x-sentX)*(x-sentX)+(y-sentY)*(y-sentY)+(z-sentZ)*(z-sentZ));
        long now=System.nanoTime();

        if (distance<=8.85 && now-lastSent<1_000_000_000L) {event.setCancelled(true);return;}
        sentX=x;sentY=y;sentZ=z;lastSent=now;packets++;
    }
    @Override public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (mc.thePlayer==null || mc.thePlayer.ticksExisted<20 || mode.getCurrMode().equals("OpenMyau")) return;
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {corrected=true;return;}
        if (!mode.getCurrMode().equals("Grim TNT")) return;
        if (event.getPacket() instanceof S12PacketEntityVelocity
                && ((S12PacketEntityVelocity)event.getPacket()).getEntityID()==mc.thePlayer.getEntityId()) pendingImpulse=true;
        else if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion p=(S27PacketExplosion)event.getPacket();
            if (p.field_149152_f!=0 || p.field_149153_g!=0 || p.field_149159_h!=0) pendingImpulse=true;
        }
    }
    @Override public void onDisable() {
        restoreOwnedState();
        if (mc.thePlayer!=null) {mc.thePlayer.motionY=0;mc.thePlayer.motionX=mc.thePlayer.motionZ=0;}
        initialized=false;impulse=false;corrected=pendingImpulse=false;verticalMotion=0;
    }
}
