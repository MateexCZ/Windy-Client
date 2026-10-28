package Windy.module.impl.render;

import Windy.event.impl.network.PacketReceiveEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.NumberSetting;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.world.World;

@ModuleInfo(name = "TimeChange", description = "Local world time", category = Category.Render)
public final class TimeChange extends Module {
    private final NumberSetting time = new NumberSetting("Time", 18000, 0, 23999, 100);
    private final NumberSetting timeSpeed = new NumberSetting("Time Speed", 0, 0, 20, 1);
    private World savedWorld;
    private long oldTime;
    private int startTick;
    public TimeChange() { addSettings(time, timeSpeed); }
    private void capture() {
        savedWorld = mc.theWorld;
        if (savedWorld == null) return;
        oldTime = savedWorld.getWorldTime();
        startTick = mc.thePlayer == null ? 0 : mc.thePlayer.ticksExisted;
    }
    @Override public void onEnable() { capture(); }
    @Override public void onUpdate() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (savedWorld != mc.theWorld) capture();
        mc.theWorld.setWorldTime(((long)time.getVal()
                + (long)(mc.thePlayer.ticksExisted - startTick) * (long)timeSpeed.getVal()) % 24000);
    }
    @Override public void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.getPacket() instanceof S03PacketTimeUpdate) event.setCancelled(true);
    }
    @Override public void onDisable() {
        if (savedWorld != null && savedWorld == mc.theWorld) savedWorld.setWorldTime(oldTime);
        savedWorld = null;
    }
}