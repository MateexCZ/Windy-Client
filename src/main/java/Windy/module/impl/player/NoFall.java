package Windy.module.impl.player;

import Windy.event.impl.MotionEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;


@ModuleInfo(name = "NoFall", description = "Spoof ground after a configurable fall", category = Category.Player)
public final class NoFall extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Vanilla", "Packet");
    private final NumberSetting distance = new NumberSetting("Distance", 3.0, 2.5, 10, 0.5);
    public NoFall() { addSettings(mode, distance); }
    @Override public void onMotionEvent(MotionEvent event) {
        if (!event.isPre() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.thePlayer.onGround || mc.thePlayer.fallDistance < distance.getVal()
                || mc.thePlayer.motionY >= 0 || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()
                || mc.thePlayer.isOnLadder() || !hasGroundBelow()) return;
        if (mode.getCurrMode().equals("Packet")) mc.thePlayer.sendQueue.addToSendQueue(new C03PacketPlayer(true));
        else event.setOnGround(true);
        mc.thePlayer.fallDistance = 0;
    }
    private boolean hasGroundBelow() {
        if (mc.thePlayer.posY < 0) return false;
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        for (double down = 0; down < mc.thePlayer.posY + 2; down += 2) {
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box.offset(0, -down, 0)).isEmpty()) return true;
        }
        return false;
    }
}