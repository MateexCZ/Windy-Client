package Windy.util.render;

import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;




public final class WorldVisuals {
    private static ResourceLocation glow;
    
    private WorldVisuals() {
    }
    
    public static void begin(boolean texture, boolean additive) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, additive ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDepthMask(false);
        if (texture) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            bindGlow();
        } else GL11.glDisable(GL11.GL_TEXTURE_2D);
    }
    
    public static void end() {
        GL11.glPopAttrib();
    }
    
    private static void bindGlow() {
        Minecraft mc = Minecraft.getMinecraft();
        if (glow == null) {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
                double distance = Math.hypot((x - 31.5) / 31.5, (y - 31.5) / 31.5);
                double opacity = Math.pow(Math.max(0, 1 - distance), 2.4);
                image.setRGB(x, y, ((int)(opacity * 255) << 24) | 0xFFFFFF);
            }
            DynamicTexture texture = new DynamicTexture(image);
            glow = mc.getTextureManager().getDynamicTextureLocation("windy_firefly", texture);
        }
        mc.getTextureManager().bindTexture(glow);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }
    
    public static void color(int color, float opacity) {
        GL11.glColor4f(((color >> 16) & 255) / 255.0F, ((color >> 8) & 255) / 255.0F, (color & 255) / 255.0F, ((color >>> 24) / 255.0F) * Math.max(0, Math.min(1, opacity)));
    }
    
    public static void sprite(double x, double y, double z, float size, int color, float alpha) {
        net.minecraft.client.renderer.entity.RenderManager camera = Minecraft.getMinecraft().getRenderManager();
        GL11.glPushMatrix();
        GL11.glTranslated(x - camera.viewerPosX, y - camera.viewerPosY, z - camera.viewerPosZ);
        GL11.glRotatef(-camera.playerViewY, 0, 1, 0);
        GL11.glRotatef(camera.playerViewX, 1, 0, 0);
        color(color, alpha);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex3f(-size, -size, 0);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex3f(-size, size, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex3f(size, size, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex3f(size, -size, 0);
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
