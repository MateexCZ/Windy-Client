package Windy.module.impl.render;

import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.render.VisualTheme;
import Windy.util.render.WorldVisuals;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.renderer.entity.RenderManager;
import org.lwjgl.opengl.GL11;

@ModuleInfo(name = "Trails", description = "Fading ribbon following your movement", category = Category.Render)
public final class Trails extends Module {
    private final NumberSetting lifetime = new NumberSetting("Lifetime", 1.4, 0.2, 5, 0.1);
    private final NumberSetting height = new NumberSetting("Height", 0.65, 0.05, 1.8, 0.05);
    private final NumberSetting opacity = new NumberSetting("Opacity", 0.45, 0.05, 1, 0.05);
    private final BooleanSetting firstPerson = new BooleanSetting("First Person", true);
    private final BooleanSetting theme = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", VisualTheme.DEFAULT_ACCENT);
    private final Deque<Point> points = new ArrayDeque<>();
    private java.lang.ref.WeakReference<Object> world;
    
    public Trails() {
        addSettings(lifetime, height, opacity, firstPerson, theme, color);
    }
    
    @Override
    public void on3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null) {
            points.clear();
            return;
        }
        if ((world == null || world.get() != mc.theWorld)) {
            world = new java.lang.ref.WeakReference<>(mc.theWorld);
            points.clear();
        }
        long now = System.nanoTime();
        long duration = (long)(lifetime.getVal() * 1000000000L);
        while (!points.isEmpty() && now - points.peekFirst().time > duration) points.removeFirst();
        double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks + 0.04;
        double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;
        Point previous = points.peekLast();
        double distance = previous == null ? 0 : Math.pow(x - previous.x, 2) + Math.pow(y - previous.y, 2) + Math.pow(z - previous.z, 2);
        if (distance > 64) points.clear();
        if (points.isEmpty() || distance > 0.0025) points.addLast(new Point(x, y, z, now));
        while (points.size() > 400) points.removeFirst();
        if (points.size() < 2 || (mc.gameSettings.thirdPersonView == 0 && !firstPerson.isState())) return;
        RenderManager camera = mc.getRenderManager();
        int tint = theme.isState() ? VisualTheme.accent() : color.getColor();
        WorldVisuals.begin(false, false);
        try {
            GL11.glBegin(GL11.GL_QUAD_STRIP);
            for (Point point : points) {
                float alpha = (1 - (float)(now - point.time) / duration) * (float)opacity.getVal();
                WorldVisuals.color(tint, alpha);
                GL11.glVertex3d(point.x - camera.viewerPosX, point.y - camera.viewerPosY, point.z - camera.viewerPosZ);
                WorldVisuals.color(tint, 0);
                GL11.glVertex3d(point.x - camera.viewerPosX, point.y + height.getVal() - camera.viewerPosY, point.z - camera.viewerPosZ);
            }
            GL11.glEnd();
        } finally {
            WorldVisuals.end();
        }
    }
    
    @Override
    public void onDisable() {
        points.clear();
        world = null;
    }
    
    private static final class Point {
        final double x;
        final double y;
        final double z;
        final long time;
        
        Point(double x, double y, double z, long time) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.time = time;
        }
    }
}
