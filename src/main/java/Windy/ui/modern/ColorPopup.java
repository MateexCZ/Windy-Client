package Windy.ui.modern;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.setting.impl.ColorSetting;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.awt.Color;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;


public final class ColorPopup {
    private static final float WIDTH = 218, HEIGHT = 282, PAD = 12, FIELD = 194;
    private static final IntBuffer SCISSOR = BufferUtils.createIntBuffer(16);
    private static ColorSetting setting;
    private static GuiScreen owner;
    private static int original;
    private static float left, top, scale = 1;
    private static final float[] hsv = new float[3];
    private static float alpha;
    private static String hex = "", error = "";
    private static boolean hexFocused, selectAll;
    private static int dragging = -1;

    private ColorPopup() { }

    public static boolean isOpen() {
        if (setting != null && Minecraft.getMinecraft().currentScreen != owner) close(false);
        return setting != null;
    }

    public static void open(ColorSetting value) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        openAt(value, Mouse.getX() * sr.getScaledWidth() / (float) mc.displayWidth,
                sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / (float) mc.displayHeight);
    }

    public static void openAt(ColorSetting value, float x, float y) {
        if (setting != null) close(false);
        Minecraft mc = Minecraft.getMinecraft();
        owner = mc.currentScreen;
        setting = value;
        original = value.getColor();
        readColor();
        dragging = -1;
        hexFocused = selectAll = false;
        error = "";
        ScaledResolution sr = new ScaledResolution(mc);
        scale = (float) Math.max(.1f, Math.min(0.7, Math.min((sr.getScaledWidth() - 8) / WIDTH, (sr.getScaledHeight() - 8) / HEIGHT)));
        left = Math.max(4, Math.min(sr.getScaledWidth() - WIDTH * scale - 4, x));
        top = Math.max(4, Math.min(sr.getScaledHeight() - HEIGHT * scale - 4, y));
    }

    private static void readColor() {
        Color color = setting.asColor();
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
        alpha = color.getAlpha() / 255f;
        hex = setting.toHex();
    }

    private static void preview() {
        setting.setColor(Math.round(alpha * 255) << 24 | Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]) & 0xFFFFFF);
        hex = setting.toHex();
        error = "";
    }

    public static void draw(int mouseX, int mouseY) {
        if (!isOpen()) return;
        if (dragging >= 0) {
            if (Mouse.isButtonDown(0)) drag(mouseX, mouseY);
            else dragging = -1;
        }
        boolean clipped = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        SCISSOR.clear();
        GL11.glGetInteger(GL11.GL_SCISSOR_BOX, SCISSOR);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPushMatrix();
        GL11.glTranslatef(left, top, 0);
        GL11.glScalef(scale, scale, 1);
        try {
            RenderUtil.drawRoundedRect(3, 4, WIDTH, HEIGHT, 3, 0x45000000);
            RenderUtil.drawRoundedRect(0, 0, WIDTH, HEIGHT, 3, 0xFF171B23);
            RenderUtil.drawBorder(0, 0, WIDTH, HEIGHT, 0xFF353D4C);
            CustomFont.drawString(CustomFont.light(20), "Color Picker", PAD, 11, VisualTheme.TEXT, 1);
            CustomFont.drawString(CustomFont.light(18), "x", WIDTH - 22, 12, VisualTheme.MUTED, 1);
            CustomFont.drawString(CustomFont.light(16),
                    CustomFont.trimToWidth(CustomFont.light(16), setting.getName(), FIELD),
                    PAD,
                    31,
                    VisualTheme.MUTED,
                    1);
            int hue = Color.HSBtoRGB(hsv[0], 1, 1);
            RenderUtil.drawGradient(PAD, 49, FIELD, 122, -1, hue, -1, hue);
            RenderUtil.drawGradient(PAD, 49, FIELD, 122, 0, 0, 0xFF000000, 0xFF000000);
            float sx = PAD + hsv[1] * FIELD;
            float sy = 49 + (1 - hsv[2]) * 122;
            RenderUtil.drawBorder(sx - 3, sy - 3, 7, 7, 0xFF111111);
            RenderUtil.drawBorder(sx - 2, sy - 2, 5, 5, -1);
            for (int step = 0; step < 6; step++) {
                int c1 = Color.HSBtoRGB(step / 6f, 1, 1), c2 = Color.HSBtoRGB((step + 1) / 6f, 1, 1);
                RenderUtil.drawGradient(PAD + step * FIELD / 6, 182, FIELD / 6, 9, c1, c2, c1, c2);
            }
            knob(PAD + hsv[0] * FIELD, 182);
            RenderUtil.drawCheckerboard(PAD, 201, FIELD, 9);
            int rgb = setting.getColor() & 0xFFFFFF;
            RenderUtil.drawGradient(PAD, 201, FIELD, 9, rgb, rgb | 0xFF000000, rgb, rgb | 0xFF000000);
            knob(PAD + alpha * FIELD, 201);
            RenderUtil.drawRect(PAD, 220, 143, 22, 0xFF10141B);
            RenderUtil.drawBorder(PAD, 220, 143, 22, !error.isEmpty() ? 0xFFE98080 : hexFocused ? VisualTheme.accent() : 0xFF343D4B);
            CustomFont.drawString(CustomFont.light(16),
                    hex + (hexFocused && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : ""),
                    18,
                    227,
                    VisualTheme.TEXT,
                    1);
            RenderUtil.drawCheckerboard(164, 220, 42, 22);
            RenderUtil.drawRect(164, 220, 42, 22, setting.getColor());
            RenderUtil.drawRect(PAD, 251, 92, 21, 0xFF292F3B);
            RenderUtil.drawRect(114, 251, 92, 21, VisualTheme.accent() | 0xFF000000);
            CustomFont.drawCenteredString(CustomFont.light(16), "Cancel", 58, 258, VisualTheme.TEXT, 1);
            CustomFont.drawCenteredString(CustomFont.light(16), "Apply", 160, 258, 0xFF11151D, 1);
            if (!error.isEmpty()) CustomFont.drawString(CustomFont.light(12), error, PAD, 242, 0xFFE98080, 1);
        } finally {
            GL11.glPopMatrix();
            GL11.glScissor(SCISSOR.get(0), SCISSOR.get(1), SCISSOR.get(2), SCISSOR.get(3));
            if (clipped) GL11.glEnable(GL11.GL_SCISSOR_TEST);
        }
    }

    private static void knob(float x, float y) {
        RenderUtil.drawRect(x - 2, y - 2, 4, 13, 0xFF151922);
        RenderUtil.drawRect(x - 1, y - 1, 2, 11, -1);
    }

    public static void drag(float mouseX, float mouseY) {
        mouseX = (mouseX - left) / scale;
        mouseY = (mouseY - top) / scale;
        float fraction = Math.max(0, Math.min(1, (mouseX - PAD) / FIELD));
        if (dragging == 0) {
            hsv[1] = fraction;
            hsv[2] = 1 - Math.max(0, Math.min(1, (mouseY - 49) / 122));
        } else if (dragging == 1) hsv[0] = fraction;
        else if (dragging == 2) alpha = fraction;
        else return;
        preview();
    }

    public static boolean click(float x, float y, int button) {
        if (!isOpen()) return false;
        if (button != 0) return true;
        float screenX = x, screenY = y;
        x = (x - left) / scale;
        y = (y - top) / scale;
        if (!RenderUtil.contains(x, y, 0, 0, WIDTH, HEIGHT)) { close(true); return true; }
        if (RenderUtil.contains(x, y, WIDTH - 30, 3, 27, 28)) { close(false); return true; }
        if (RenderUtil.contains(x, y, PAD, 251, 92, 21)) { close(false); return true; }
        if (RenderUtil.contains(x, y, 114, 251, 92, 21)) { close(true); return true; }
        hexFocused = RenderUtil.contains(x, y, PAD, 220, 143, 22);
        if (hexFocused) { selectAll = true; return true; }
        if (RenderUtil.contains(x, y, PAD, 49, FIELD, 122)) dragging = 0;
        else if (RenderUtil.contains(x, y, PAD, 178, FIELD, 17)) dragging = 1;
        else if (RenderUtil.contains(x, y, PAD, 197, FIELD, 17)) dragging = 2;
        if (dragging >= 0) drag(screenX, screenY);
        return true;
    }

    public static boolean mouseInput() {
        if (!isOpen()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        float x = Mouse.getEventX() * owner.width / (float) mc.displayWidth;
        float y = owner.height - Mouse.getEventY() * owner.height / (float) mc.displayHeight - 1;
        int button = Mouse.getEventButton();
        if (button == 0 && !Mouse.getEventButtonState()) { dragging = -1; return true; }
        if (button >= 0 && Mouse.getEventButtonState()) return click(x, y, button);
        if (dragging >= 0) drag(x, y);
        return true;
    }

    public static boolean keyboardInput() {
        if (!isOpen()) return false;
        return !Keyboard.getEventKeyState() || key(Keyboard.getEventCharacter(), Keyboard.getEventKey());
    }

    public static boolean key(char character, int code) {
        if (!isOpen()) return false;
        if (code == Keyboard.KEY_ESCAPE) close(false);
        else if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) close(true);
        else if (hexFocused) {
            if (code == Keyboard.KEY_A && GuiScreen.isCtrlKeyDown()) selectAll = true;
            else if (code == Keyboard.KEY_V && GuiScreen.isCtrlKeyDown()) {
                hex = GuiScreen.getClipboardString().trim();
                selectAll = false;
            } else if (code == Keyboard.KEY_BACK || code == Keyboard.KEY_DELETE) {
                hex = selectAll ? "" : hex.isEmpty() ? "" : hex.substring(0, hex.length() - 1);
                selectAll = false;
            } else if (character == '#' || Character.digit(character, 16) >= 0) {
                if (selectAll) hex = "";
                selectAll = false;
                if (hex.length() < 9) hex += character;
            }
        }
        return true;
    }

    private static void close(boolean save) {
        if (setting == null) return;
        if (save) {
            if (hexFocused) {
                try { setting.fromHex(hex); }
                catch (IllegalArgumentException invalid) { error = "Use RRGGBB or AARRGGBB"; return; }
            }
            if (Windy.INSTANCE.getConfigManager() != null) Windy.INSTANCE.getConfigManager().saveConfig();
        } else setting.setColor(original);
        setting = null;
        owner = null;
        dragging = -1;
        hexFocused = selectAll = false;
    }
}
