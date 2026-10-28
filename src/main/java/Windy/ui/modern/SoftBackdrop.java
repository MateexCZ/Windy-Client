package Windy.ui.modern;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;


public final class SoftBackdrop {
    private static ResourceLocation texture;

    private SoftBackdrop() { }

    public static void draw(int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        if (texture == null) {
            texture = mc.getTextureManager().getDynamicTextureLocation(
                    "windy_soft_backdrop", new DynamicTexture(createImage()));
        }
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawScaledCustomSizeModalRect(0, 0, 0, 0, 256, 144, width, height, 256, 144);
    }

    private static BufferedImage createImage() {
        BufferedImage image = new BufferedImage(256, 144, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(10, 14, 21));
        graphics.fillRect(0, 0, 256, 144);
        drawGlow(graphics, 56, 18, 160, new Color(35, 57, 83, 140));
        drawGlow(graphics, 235, 140, 130, new Color(48, 38, 70, 100));
        graphics.dispose();
        return gaussianBlur(image, 10);
    }

    private static void drawGlow(Graphics2D graphics, int x, int y, int radius, Color color) {
        graphics.setPaint(new RadialGradientPaint(x, y, radius,
                new float[]{0f, 1f}, new Color[]{color, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
        graphics.fillRect(0, 0, 256, 144);
    }

    private static BufferedImage gaussianBlur(BufferedImage source, int radius) {
        int size = radius * 2 + 1;
        float[] weights = new float[size];
        float sigma = radius / 2f;
        float sum = 0;
        for (int i = 0; i < size; i++) {
            int distance = i - radius;
            weights[i] = (float) Math.exp(-(distance * distance) / (2f * sigma * sigma));
            sum += weights[i];
        }
        for (int i = 0; i < size; i++) {
            weights[i] /= sum;
        }
        ConvolveOp horizontal = new ConvolveOp(new Kernel(size, 1, weights), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp vertical = new ConvolveOp(new Kernel(1, size, weights), ConvolveOp.EDGE_NO_OP, null);
        return vertical.filter(horizontal.filter(source, null), null);
    }
}
