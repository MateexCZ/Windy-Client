package Windy.ui.modern;

import Windy.Windy;
import Windy.setting.impl.NumberSetting;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;


public final class HudEditor {
    private static final List<Anchor> anchors = new ArrayList<>();
    private static Anchor dragging;
    private static float offsetX, offsetY;
    private static final long ACTIVE_WINDOW = 250_000_000L;
    private static final List<HudSnap.Bounds> neighbours = new ArrayList<>();
    private HudEditor() { }

    public static final class Anchor {
        public final NumberSetting x;
        public final NumberSetting y;
        private float left, top, width, height;
        private long lastDraw;

        public Anchor(String name, double defaultX, double defaultY) {
            x = new NumberSetting(name + " X", defaultX, 0, 1, .001);
            y = new NumberSetting(name + " Y", defaultY, 0, 1, .001);

            x.addParent(x, setting -> false);
            y.addParent(y, setting -> false);
            anchors.add(this);
        }

        public float left(ScaledResolution sr, float width) {
            return (float) x.getVal() * Math.max(0, sr.getScaledWidth() - width);
        }

        public float top(ScaledResolution sr, float height) {
            return (float) y.getVal() * Math.max(0, sr.getScaledHeight() - height - 16);
        }

        public void mark(float left, float top, float width, float height) {
            this.left = left; this.top = top; this.width = width; this.height = height;
            lastDraw = System.nanoTime();
            if (Minecraft.getMinecraft().currentScreen instanceof GuiChat) {
                int color = AnimationUtil.multiplyAlpha(VisualTheme.accent(), dragging == this ? .38f : .16f);
                RenderUtil.drawRoundedRect(left - 2, top - 2, width + 4, height + 4, 5, color);
            }
        }
    }

    public static boolean click(int x, int y, int button) {
        if (button != 0 || !(Minecraft.getMinecraft().currentScreen instanceof GuiChat)) return false;
        long now = System.nanoTime();
        for (int i = anchors.size() - 1; i >= 0; i--) {
            Anchor anchor = anchors.get(i);
            if (anchor.width > 0 && anchor.height > 0 && now - anchor.lastDraw < ACTIVE_WINDOW &&
                    RenderUtil.contains(x, y, anchor.left, anchor.top, anchor.width, anchor.height)) {
                dragging = anchor;
                offsetX = x - anchor.left;
                offsetY = y - anchor.top;
                return true;
            }
        }
        return false;
    }


    public static void update(int mouseX, int mouseY) {
        if (dragging == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChat) || !Mouse.isButtonDown(0)) {
            finish();
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        long now = System.nanoTime();
        neighbours.clear();
        for (Anchor anchor : anchors) {
            if (anchor != dragging && anchor.width > 0 && anchor.height > 0
                    && now - anchor.lastDraw < ACTIVE_WINDOW) {
                neighbours.add(new HudSnap.Bounds(anchor.left, anchor.top, anchor.width, anchor.height));
            }
        }
        boolean snapping = !Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                && !Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        HudSnap.Result result = HudSnap.align(mouseX - offsetX, mouseY - offsetY,
                dragging.width, dragging.height, sr.getScaledWidth(), sr.getScaledHeight(),
                neighbours, snapping);
        dragging.x.setVal(result.x / Math.max(1, sr.getScaledWidth() - dragging.width));
        dragging.y.setVal(result.y / Math.max(1, sr.getScaledHeight() - dragging.height - 16));
        drawGuides(result, sr);
    }

    private static void drawGuides(HudSnap.Result result, ScaledResolution sr) {
        int color = AnimationUtil.multiplyAlpha(VisualTheme.accent(), .8f);
        if (!Float.isNaN(result.verticalGuide)) {
            RenderUtil.drawRect(result.verticalGuide, 0, .5f, sr.getScaledHeight() - 16, color);
        }
        if (!Float.isNaN(result.horizontalGuide)) {
            RenderUtil.drawRect(0, result.horizontalGuide, sr.getScaledWidth(), .5f, color);
        }
    }

    public static void finish() {
        if (dragging != null) {
            dragging = null;
            neighbours.clear();
            if (Windy.INSTANCE.getConfigManager() != null) Windy.INSTANCE.getConfigManager().saveConfig();
        }
    }
}
