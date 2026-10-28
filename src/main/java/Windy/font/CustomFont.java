package Windy.font;

import Windy.font.fontmanager.FontManager;
import Windy.font.fontmanager.TTFFontRenderer;
import Windy.util.animation.AnimationUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

@UtilityClass
public final class CustomFont {

    public static final FontManager FONT_MANAGER = new FontManager();


    public static final TTFFontRenderer LIGHT_14 = FONT_MANAGER.getFont("Light 14");
    public static final TTFFontRenderer LIGHT_18 = FONT_MANAGER.getFont("Light 18");
    public static final TTFFontRenderer LIGHT_22 = FONT_MANAGER.getFont("Light 22");
    public static final TTFFontRenderer LIGHT_24 = FONT_MANAGER.getFont("Light 24");
    public static final TTFFontRenderer LIGHT_36 = FONT_MANAGER.getFont("Light 36");
    public static final TTFFontRenderer LIGHT_52 = FONT_MANAGER.getFont("Light 52");
    public static final TTFFontRenderer LIGHT_72 = FONT_MANAGER.getFont("Light 72");

    public static final TTFFontRenderer COMFORTAA_14 = FONT_MANAGER.getFont("Comf 14");
    public static final TTFFontRenderer COMFORTAA_18 = FONT_MANAGER.getFont("Comf 18");
    public static final TTFFontRenderer COMFORTAA_22 = FONT_MANAGER.getFont("Comf 22");
    public static final TTFFontRenderer COMFORTAA_24 = FONT_MANAGER.getFont("Comf 24");
    public static final TTFFontRenderer COMFORTAA_36 = FONT_MANAGER.getFont("Comf 36");


    public static final TTFFontRenderer DEFAULT = LIGHT_18;


    public static TTFFontRenderer light(int size) {
        return FONT_MANAGER.getFont("Light " + size);
    }


    public static TTFFontRenderer rounded(int size) {
        return FONT_MANAGER.getFont("Rounded " + size);
    }

    public static TTFFontRenderer comfortaa(int size) {
        return FONT_MANAGER.getFont("Comf " + size);
    }

    public static void drawString(TTFFontRenderer font, String text, double x, double y,
                                  int color, float opacity) {
        if (text == null || text.isEmpty()) return;
        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        int alphaFunction = GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);
        float alphaReference = GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);
        GlStateManager.enableTexture2D();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0);
        try { font.drawString(text, (float) x, (float) y, AnimationUtil.multiplyAlpha(color, opacity)); }
        finally {
            GlStateManager.alphaFunc(alphaFunction, alphaReference);
            if (!alphaTest) GlStateManager.disableAlpha();
        }
    }

    public static void drawCenteredString(TTFFontRenderer font, String text, double x, double y,
                                          int color, float opacity) {
        drawString(font, text, x - font.getWidth(text) / 2f, y, color, opacity);
    }

    public static String trimToWidth(TTFFontRenderer font, String text, float width) {
        if (text == null || width <= 0) return "";
        if (font.getWidth(text) <= width) return text;
        if (font.getWidth("...") > width) return "";
        int end = text.length();
        while (end > 0 && font.getWidth(text.substring(0, end) + "...") > width) end--;
        return text.substring(0, end) + "...";
    }

    public static void drawCenteredString(TTFFontRenderer font, String text, double x, double y, int color) {
        drawCenteredString(font, text, x, y, color, 1);
    }

    public static void drawStringWithShadow(TTFFontRenderer font, String text, double x, double y, int color) {
        drawString(font, text, x + .5, y + .5, color & 0xFF000000, 1);
        drawString(font, text, x, y, color, 1);
    }

    public static void drawStringWithDropShadow(TTFFontRenderer font, String text, double x, double y, int color) {
        drawString(font, text, x + .6, y + .6, 0x64000000, (color >>> 24) / 255f);
        drawString(font, text, x, y, color, 1);
    }

    public static void drawString(String text, double x, double y, int color) {
        drawString(DEFAULT, text, x, y, color, 1);
    }

    public static void drawCenteredString(String text, double x, double y, int color) {
        drawCenteredString(DEFAULT, text, x, y, color, 1);
    }

    public static void drawStringWithShadow(String text, double x, double y, int color) {
        drawStringWithShadow(DEFAULT, text, x, y, color);
    }

    public static float getWidth(String text) { return DEFAULT.getWidth(text); }

    public static float getHeight() { return DEFAULT.getHeight("I"); }
}
