package Windy.module.impl.player;

import Windy.event.impl.network.PacketSendEvent;
import Windy.Windy;
import Windy.module.impl.movement.Flight;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;


@ModuleInfo(name = "AntiVoid", description = "Buffers movement over the void and restores the last grounded position", category = Category.Player)
public final class AntiVoid extends Module {
    private final NumberSetting fallDistance = new NumberSetting("Fall Distance", 3, 1, 10, 0.5);
    private final List<Packet<?>> buffer = new ArrayList<>();
    private double safeX, safeY, safeZ;
    private boolean hasSafePosition;
    private World lastWorld;
    private long lastRescue;
    public AntiVoid() { addSettings(fallDistance); }

    @Override public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) { reset(); return; }
        if (lastWorld != mc.theWorld) { reset(); lastWorld = mc.theWorld; }
        if (mc.thePlayer.capabilities.allowFlying || Windy.INSTANCE.isToggled(Flight.class)
                || mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) {
            flush(); return;
        }
        if (mc.thePlayer.onGround) {
            safeX = mc.thePlayer.posX;
            safeY = mc.thePlayer.posY;
            safeZ = mc.thePlayer.posZ;
            hasSafePosition = true;
            flush();
            return;
        }
        if (!hasSafePosition || hasGroundBelow() || mc.thePlayer.fallDistance < fallDistance.getVal()
                || System.currentTimeMillis() - lastRescue < 1000) return;
        lastRescue = System.currentTimeMillis();
        buffer.clear();
        mc.thePlayer.motionX = mc.thePlayer.motionY = mc.thePlayer.motionZ = 0;
        mc.thePlayer.setPosition(safeX, safeY, safeZ);
        mc.thePlayer.fallDistance = 0;
        mc.getNetHandler().addToSendQueueNoEvent(
                new C03PacketPlayer.C04PacketPlayerPosition(safeX, safeY, safeZ, false));
    }

    @Override public void onPacketSendEvent(PacketSendEvent event) {
        if (!(event.getPacket() instanceof C03PacketPlayer) || mc.thePlayer == null || mc.theWorld == null
                || !hasSafePosition || mc.thePlayer.onGround || mc.thePlayer.capabilities.allowFlying
                || Windy.INSTANCE.isToggled(Flight.class)) return;
        if (!hasGroundBelow()) {
            event.setCancelled(true);
            if (mc.thePlayer.fallDistance < fallDistance.getVal() && buffer.size() < 80)
                buffer.add(event.getPacket());
        } else flush();
    }

    private boolean hasGroundBelow() {
        if (mc.thePlayer.posY < 0) return false;
        AxisAlignedBB box = mc.thePlayer.getEntityBoundingBox();
        for (double down = 0; down < mc.thePlayer.posY + 2; down += 2)
            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, box.offset(0, -down, 0)).isEmpty()) return true;
        return false;
    }
    private void flush() {
        if (mc.getNetHandler() != null)
            for (Packet<?> packet : buffer) mc.getNetHandler().addToSendQueueNoEvent(packet);
        buffer.clear();
    }
    private void reset() { buffer.clear(); hasSafePosition = false; lastWorld = null; }
    @Override public void onDisable() {
        if (mc.thePlayer != null && mc.theWorld != null && hasGroundBelow()) flush();
        reset();
    }
}