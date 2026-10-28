package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.render.VisualTheme;
import Windy.util.render.WorldVisuals;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(name = "Fireflies", description = "Soft floating lights around the player", category = Category.Render)
public final class Fireflies extends Module {
    private final NumberSetting count = new NumberSetting("Count", 32, 4, 96, 1);
    private final NumberSetting radius = new NumberSetting("Radius", 8, 2, 20, .5);
    private final NumberSetting size = new NumberSetting("Size", .13, .04, .5, .01);
    private final NumberSetting speed = new NumberSetting("Speed", .4, .05, 2, .05);
    private final BooleanSetting theme = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", VisualTheme.DEFAULT_ACCENT);
    private final List<Firefly> particles = new ArrayList<>();
    private final Random random = new Random();
    private java.lang.ref.WeakReference<Object> world;

    public Fireflies() { addSettings(count, radius, size, speed, theme, color); }

    @Override public void on3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null) { particles.clear(); return; }
        if ((world == null || world.get() != mc.theWorld)) { particles.clear(); world = new java.lang.ref.WeakReference<>(mc.theWorld); }
        while (particles.size() > count.getVal()) particles.remove(particles.size() - 1);
        while (particles.size() < count.getVal()) particles.add(spawn());
        double time = System.nanoTime() / 1_000_000_000.0 * speed.getVal();
        int tint = theme.isState() ? VisualTheme.accent() : color.getColor();
        WorldVisuals.begin(true, true);
        try {
            for (int i = 0; i < particles.size(); i++) {
                Firefly particle = particles.get(i);
                if (mc.thePlayer.getDistanceSq(particle.x, particle.y, particle.z) > radius.getVal() * radius.getVal() * 4) {
                    particles.set(i, particle = spawn());
                }
                double phase = time + particle.phase;
                float opacity = .45f + .35f * (float) Math.sin(phase * 1.7);
                double x = particle.x + Math.sin(phase) * .55;
                double y = particle.y + Math.sin(phase * .7) * .4;
                double z = particle.z + Math.cos(phase * .8) * .55;
                WorldVisuals.sprite(x, y, z, (float) size.getVal() * 2.5f, tint, opacity * .3f);
                WorldVisuals.sprite(x, y, z, (float) size.getVal(), tint, opacity);
            }
        } finally { WorldVisuals.end(); }
    }

    private Firefly spawn() {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 1 + random.nextDouble() * radius.getVal();
        return new Firefly(mc.thePlayer.posX + Math.cos(angle) * distance,
                mc.thePlayer.posY + random.nextDouble() * 4, mc.thePlayer.posZ + Math.sin(angle) * distance,
                random.nextDouble() * Math.PI * 2);
    }

    @Override public void onDisable() { particles.clear(); world = null; }
    private static final class Firefly {
        final double x, y, z, phase;
        Firefly(double x, double y, double z, double phase) {
            this.x = x; this.y = y; this.z = z; this.phase = phase;
        }
    }
}
