package Windy.ui.clickgui.DropDowns;

import Windy.Windy;
import Windy.module.Category;
import Windy.module.impl.ui.ClickGUI;
import Windy.ui.modern.ColorPopup;
import Windy.ui.modern.SoftBackdrop;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public final class DropdownScreen extends GuiScreen {
    private final List<DropdownPanel> panels = new ArrayList<>();
    private static float scale = 1, originX = 10, originY = 30;
    private static int screenWidth, screenHeight;
    private static final int OPEN_DURATION_MS = 240, CLOSE_DURATION_MS = 180;
    private final Animation visibility = new Animation(0);
    private boolean initialized, closing;
    private long closeStartedAt;

    @Override public void initGui() {
        boolean opening = !initialized;
        if (opening) {
            initialized = true;
            closing = false;
            visibility.snap(0);
            visibility.animateTo(1, OPEN_DURATION_MS);
        }
        screenWidth = width;
        screenHeight = height;
        float layoutWidth = (Category.values().length + 1) * 158 - 10;
        scale = Math.max(.1f, Math.min(1, (width - 20) / layoutWidth));
        originX = (width - layoutWidth * scale) / 2;
        originY = 29;
        if (panels.isEmpty()) {
            int column = 0;
            for (Category category : Category.values()) panels.add(new DropdownPanel(category, column++ * 158, 0));
            panels.add(DropdownPanel.themes(column * 158, 0));
        }
        for (DropdownPanel panel : panels) {
            panel.setViewportLimit(Math.min(340, (height - originY - 28) / scale - 27));
            if (opening) panel.prepareForOpen();
        }
    }

    public static float minPanelX() { return -originX / scale; }
    public static float maxPanelX(float panelWidth) { return Math.max(minPanelX(), (screenWidth - originX) / scale - panelWidth); }
    public static float minPanelY() { return -originY / scale; }
    public static float maxPanelY(float headerHeight) { return Math.max(minPanelY(), (screenHeight - originY) / scale - headerHeight); }

    private int localX(int x) { return Math.round((x - originX) / scale); }
    private int localY(int y) { return Math.round((y - originY) / scale); }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        if (closing && System.nanoTime() - closeStartedAt >= CLOSE_DURATION_MS * 1_000_000L) {
            mc.displayGuiScreen(null);
            return;
        }
        float opacity = visibility.get();
        if (mc.theWorld == null) SoftBackdrop.draw(width, height);
        float background = (float) ClickGUI.backgroundOpacity.getVal() * opacity;
        RenderUtil.drawRect(0, 0, width, height, AnimationUtil.multiplyAlpha(0xFF000000, background));
        GlStateManager.pushMatrix();
        GlStateManager.translate(originX, originY, 0);
        GlStateManager.scale(scale, scale, 1);
        try { for (DropdownPanel panel : panels) panel.draw(localX(x), localY(y), opacity); }
        finally { GlStateManager.popMatrix(); GL11.glDisable(GL11.GL_SCISSOR_TEST); }
        super.drawScreen(x, y, partialTicks);
    }


    public static void clip(float x, float y, float width, float height) {
        RenderUtil.scissor(x, y, width, height, scale, originX, originY);
    }

    @Override protected void mouseClicked(int x, int y, int button) throws IOException {
        if (closing) return;
        if (ColorPopup.isOpen()) { ColorPopup.click(x, y, button); return; }
        for (int i = panels.size() - 1; i >= 0; i--) if (panels.get(i).mouseClicked(localX(x), localY(y), button)) break;
    }

    @Override protected void mouseReleased(int x, int y, int button) {
        for (DropdownPanel panel : panels) panel.mouseReleased(localX(x), localY(y), button);
        save();
    }

    @Override protected void keyTyped(char character, int key) throws IOException {
        if (closing) return;
        if (ColorPopup.isOpen()) { ColorPopup.key(character, key); return; }
        for (DropdownPanel panel : panels) if (panel.keyTyped(character, key)) { save(); return; }
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RSHIFT) beginClose();
    }

    @Override public void handleMouseInput() throws IOException {
        if (closing) return;
        if (!ColorPopup.isOpen() && Mouse.getEventDWheel() != 0) {
            int x = Mouse.getEventX() * width / mc.displayWidth;
            int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            for (DropdownPanel panel : panels) panel.handleScroll(Mouse.getEventDWheel(), localX(x), localY(y));
        }
        super.handleMouseInput();
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        closeStartedAt = System.nanoTime();
        visibility.animateTo(0, CLOSE_DURATION_MS);
        for (DropdownPanel panel : panels) panel.mouseReleased(0, 0, 0);
    }

    private static void save() {
        if (Windy.INSTANCE.getConfigManager() != null) Windy.INSTANCE.getConfigManager().saveConfig();
    }
    @Override public void onGuiClosed() {
        for (DropdownPanel panel : panels) panel.cancelInteraction();
        if (ColorPopup.isOpen()) ColorPopup.key((char) 0, Keyboard.KEY_ESCAPE);
        initialized = false;
        save();
    }
    @Override public boolean doesGuiPauseGame() { return false; }
}
