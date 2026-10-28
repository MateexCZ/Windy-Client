package Windy.util.render;

import Windy.util.render.WorldVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;


public final class ModuleGeometry {
    private ModuleGeometry() { }

    public static float[] rotations(Vec3 point) {
        Minecraft mc = Minecraft.getMinecraft();
        double x = point.xCoord - mc.thePlayer.posX;
        double y = point.yCoord - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
        double z = point.zCoord - mc.thePlayer.posZ;
        return new float[] {
                (float) Math.toDegrees(Math.atan2(z, x)) - 90,
                (float) -Math.toDegrees(Math.atan2(y, Math.hypot(x, z)))
        };
    }

    public static void box(AxisAlignedBB box, int color, boolean throughWalls) {
        net.minecraft.client.renderer.entity.RenderManager camera = Minecraft.getMinecraft().getRenderManager();
        AxisAlignedBB bounds = box.offset(-camera.viewerPosX, -camera.viewerPosY, -camera.viewerPosZ);
        WorldVisuals.begin(false, false);
        try {
            if (throughWalls) GL11.glDisable(GL11.GL_DEPTH_TEST);
            WorldVisuals.color(color, 1);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);
            for (int axis = 0; axis < 3; axis++) {
                for (int first = 0; first < 2; first++) {
                    for (int second = 0; second < 2; second++) {
                        for (int end = 0; end < 2; end++) {
                            double x = axis == 0 ? end : first;
                            double y = axis == 1 ? end : axis == 0 ? first : second;
                            double z = axis == 2 ? end : second;
                            GL11.glVertex3d(bounds.minX + x * (bounds.maxX - bounds.minX),
                                    bounds.minY + y * (bounds.maxY - bounds.minY),
                                    bounds.minZ + z * (bounds.maxZ - bounds.minZ));
                        }
                    }
                }
            }
            GL11.glEnd();
        } finally {
            WorldVisuals.end();
        }
    }
}
