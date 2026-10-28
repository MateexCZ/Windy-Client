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
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "JumpCircles", description = "Expanding rings when leaving the ground", category = Category.Render)
public final class JumpCircles extends Module {
    private final NumberSetting radius = new NumberSetting("Radius", 1.4, .3, 3, .1);
    private final NumberSetting duration = new NumberSetting("Duration", .8, .2, 2, .1);
    private final BooleanSetting theme = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", VisualTheme.DEFAULT_ACCENT);
    private final List<Ring> rings = new ArrayList<>();
    private boolean grounded;
    private java.lang.ref.WeakReference<Object> world;

    public JumpCircles() { addSettings(radius, duration, theme, color); }

    @Override public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) { rings.clear(); grounded = false; return; }
        if ((world == null || world.get() != mc.theWorld)) { rings.clear(); grounded = false; world = new java.lang.ref.WeakReference<>(mc.theWorld); }
        if (grounded && !mc.thePlayer.onGround && mc.thePlayer.motionY > 0) {
            if (rings.size() == 12) rings.remove(0);
            rings.add(new Ring(mc.thePlayer.posX, mc.thePlayer.posY + .02, mc.thePlayer.posZ, System.nanoTime()));
        }
        grounded = mc.thePlayer.onGround;
    }

    @Override public void on3D(float partialTicks) {
        long now = System.nanoTime();
        double lifetime = duration.getVal() * 1_000_000_000;
        rings.removeIf(ring -> now - ring.time > lifetime);
        if (rings.isEmpty()) return;
        RenderManager camera = mc.getRenderManager();
        int tint = theme.isState() ? VisualTheme.accent() : color.getColor();
        WorldVisuals.begin(false, false);
        try {
            for (Ring ring : rings) {
                float progress = (float) ((now - ring.time) / lifetime);
                double outer = radius.getVal() * (1 - Math.pow(1 - progress, 3));
                GL11.glBegin(GL11.GL_QUAD_STRIP);
                for (int segment = 0; segment <= 96; segment++) {
                    double angle = segment * Math.PI * 2 / 96;
                    WorldVisuals.color(tint, (1 - progress) * .7f);
                    GL11.glVertex3d(ring.x + Math.cos(angle) * outer - camera.viewerPosX,
                            ring.y - camera.viewerPosY, ring.z + Math.sin(angle) * outer - camera.viewerPosZ);
                    WorldVisuals.color(tint, 0);
                    GL11.glVertex3d(ring.x + Math.cos(angle) * outer * .78 - camera.viewerPosX,
                            ring.y - camera.viewerPosY, ring.z + Math.sin(angle) * outer * .78 - camera.viewerPosZ);
                }
                GL11.glEnd();
            }
        } finally { WorldVisuals.end(); }
    }

    @Override public void onDisable() { rings.clear(); grounded = false; world = null; }
    private static final class Ring {
        final double x, y, z;
        final long time;
        Ring(double x, double y, double z, long time) { this.x = x; this.y = y; this.z = z; this.time = time; }
    }
}
