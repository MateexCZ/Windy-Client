package Windy.module.impl.render;

import Windy.event.impl.network.PacketReceiveEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.ModeSetting;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.world.World;

@ModuleInfo(name = "Weather", description = "Local rain and precipitation colour", category = Category.Render)
public final class Weather extends Module {
    private static Weather instance;
    private final ModeSetting weather = new ModeSetting("Weather", "Unchanged", "Clear", "Rain");
    private final ColorSetting color = new ColorSetting("Rain Color", 0xFF62BCFF);
    private World savedWorld;
    private boolean oldRaining, oldThundering, changed;
    private float oldStrength, oldThunderStrength;

    public Weather() { instance = this; addSettings(weather, color); }
    private void capture() {
        savedWorld = mc.theWorld;
        changed = false;
        if (savedWorld == null) return;
        oldRaining = savedWorld.getWorldInfo().isRaining();
        oldThundering = savedWorld.getWorldInfo().isThundering();
        oldStrength = savedWorld.getRainStrength(1);
        oldThunderStrength = oldStrength > .0001f ? savedWorld.getThunderStrength(1) / oldStrength : 0;
    }
    private void restore() {
        if (changed && savedWorld != null && savedWorld == mc.theWorld) {
            savedWorld.setRainStrength(oldStrength);
            savedWorld.setThunderStrength(oldThunderStrength);
            savedWorld.getWorldInfo().setRaining(oldRaining);
            savedWorld.getWorldInfo().setThundering(oldThundering);
        }
        changed = false;
    }
    @Override public synchronized void onEnable() { capture(); }
    @Override public synchronized void onUpdate() {
        if (savedWorld != mc.theWorld) capture();
        if (savedWorld == null) return;
        if (weather.getCurrMode().equals("Unchanged")) { restore(); capture(); return; }
        changed = true;
        boolean rain = weather.getCurrMode().equals("Rain");
        savedWorld.setRainStrength(rain ? 1 : 0);
        savedWorld.getWorldInfo().setRaining(rain);
        if (!rain) {
            savedWorld.setThunderStrength(0);
            savedWorld.getWorldInfo().setThundering(false);
        }
    }
    @Override public synchronized void onPacketReceiveEvent(PacketReceiveEvent event) {
        if (event.isCancelled() || !(event.getPacket() instanceof S2BPacketChangeGameState)
                || weather.getCurrMode().equals("Unchanged")) return;
        if (savedWorld != mc.theWorld) capture();
        if (savedWorld == null) return;
        S2BPacketChangeGameState packet = (S2BPacketChangeGameState) event.getPacket();
        switch (packet.getGameState()) {
            case 1: oldRaining = true; oldStrength = 0; break;
            case 2: oldRaining = false; oldStrength = 1; break;
            case 7: oldStrength = packet.func_149137_d(); break;
            case 8: oldThunderStrength = packet.func_149137_d(); break;
            default: return;
        }
        event.setCancelled(true);
    }
    public static float channel(int shift) {
        if (instance == null || !instance.isToggled()) return 1;
        return ((instance.color.getColor() >> shift) & 255) / 255f;
    }
    @Override public synchronized void onDisable() { restore(); savedWorld = null; }
}
