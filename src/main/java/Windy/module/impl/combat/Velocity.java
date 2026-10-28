package Windy.module.impl.combat;

import Windy.event.impl.network.PacketReceiveEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.movement.MovementUtils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;

@ModuleInfo(name = "Velocity", description = "Adjusts knockback and explosion impulse", category = Category.Combat)
public final class Velocity extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Cancel", "NCP", "Smooth", "Jump");
    private final NumberSetting horizontal = new NumberSetting("Horizontal", 0, 0, 100, 1);
    private final NumberSetting vertical = new NumberSetting("Vertical", 100, 0, 100, 1);
    private volatile boolean pendingJump;

    public Velocity() {
        addSettings(mode, horizontal, vertical);
        horizontal.addParent(mode, setting -> mode.getCurrMode().equals("NCP") || mode.getCurrMode().equals("Smooth"));
        vertical.addParent(mode, setting -> mode.getCurrMode().equals("NCP") || mode.getCurrMode().equals("Smooth"));
    }

    @Override
    public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (mc.thePlayer == null) return;
        Packet<?> packet = event.getPacket();
        boolean cancel = mode.getCurrMode().equals("Cancel");
        boolean jump = mode.getCurrMode().equals("Jump");
        double h = horizontal.getVal() / 100.0;
        double v = vertical.getVal() / 100.0;
        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity velocity = (S12PacketEntityVelocity) packet;
            if (velocity.getEntityID() != mc.thePlayer.getEntityId()) return;
            if (cancel || jump) {
                event.setCancelled(true);
                if (jump && mc.thePlayer.onGround) pendingJump = true;
            } else {
                velocity.motionX = (int) Math.round(velocity.motionX * h);
                velocity.motionY = (int) Math.round(velocity.motionY * v);
                velocity.motionZ = (int) Math.round(velocity.motionZ * h);
            }
        } else if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion explosion = (S27PacketExplosion) packet;
            if (cancel || jump) {
                explosion.field_149152_f = 0;
                explosion.field_149153_g = 0;
                explosion.field_149159_h = 0;
                if (jump && mc.thePlayer.onGround) pendingJump = true;
            } else {
                explosion.field_149152_f *= h;
                explosion.field_149153_g *= v;
                explosion.field_149159_h *= h;
            }
        }
    }

    @Override
    public void onUpdate() {
        if (pendingJump && mc.thePlayer != null && mc.thePlayer.onGround && MovementUtils.isMoving()) {
            mc.thePlayer.jump();
        }
        pendingJump = false;
    }

    @Override
    public void onDisable() {
        pendingJump = false;
    }
}
