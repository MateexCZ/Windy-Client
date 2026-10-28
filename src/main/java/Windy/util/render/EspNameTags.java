package Windy.util.render;

import Windy.font.CustomFont;
import Windy.util.render.WorldVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;


public final class EspNameTags {
    private EspNameTags() { }

    public static void draw(EntityPlayer player, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager camera = mc.getRenderManager();
        String name = player.getName().replaceAll("\u00a7.", "") + "  ";
        String health = Math.round(player.getHealth()) + " HP  ";
        String distance = Math.round(mc.thePlayer.getDistanceToEntity(player)) + "m";
        float nameWidth = CustomFont.light(18).getWidth(name);
        float healthWidth = CustomFont.light(18).getWidth(health);
        float width = nameWidth + healthWidth + CustomFont.light(18).getWidth(distance) + 14;
        WorldVisuals.begin(true, false);
        GL11.glPushMatrix();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glTranslated(x - camera.viewerPosX, y - camera.viewerPosY, z - camera.viewerPosZ);
            GL11.glRotatef(-camera.playerViewY, 0, 1, 0);
            GL11.glRotatef(camera.playerViewX, 1, 0, 0);
            GL11.glScalef(-.025f, -.025f, .025f);
            RenderUtil.drawRoundedRect(-width / 2, -12, width, 20, 5, 0xDA131C28);
            float textX = -width / 2 + 7;
            CustomFont.drawString(CustomFont.light(18), name, textX, -6, 0xFFE7EDF7, 1);
            int healthColor = player.getHealth() <= 6 ? 0xFFFF7C89 : 0xFF87D8AC;
            CustomFont.drawString(CustomFont.light(18), health, textX + nameWidth, -6, healthColor, 1);
            CustomFont.drawString(CustomFont.light(18), distance, textX + nameWidth + healthWidth, -6, 0xFFA9B6C9, 1);
        } finally {
            GL11.glPopMatrix();
            WorldVisuals.end();
        }
    }
}
