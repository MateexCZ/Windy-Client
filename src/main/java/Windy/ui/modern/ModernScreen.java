package Windy.ui.modern;

import Windy.font.CustomFont;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;


public abstract class ModernScreen extends GuiScreen {
    protected final int designWidth;
    protected final int designHeight;
    protected float scale;
    protected float originX;
    protected float originY;
    protected float frameSeconds;
    protected float opacity;

    private final Animation visibility = new Animation(0);
    private final Map<String, Animation> hoverAnimations = new HashMap<>();
    private final List<ButtonArea> buttons = new ArrayList<>();
    private long lastFrame;
    private Runnable pendingNavigation;

    protected ModernScreen(int designWidth, int designHeight) {
        this.designWidth = designWidth;
        this.designHeight = designHeight;
    }

    @Override
    public void initGui() {
        visibility.snap(0);
        visibility.animateTo(1, 260);
        pendingNavigation = null;
        lastFrame = System.nanoTime();
        Keyboard.enableRepeatEvents(true);
        updateLayout();
    }

    @Override
    public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        frameSeconds = Math.min(.05f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        opacity = visibility.get();

        if (pendingNavigation != null && opacity < .001f) {
            Runnable navigation = pendingNavigation;
            pendingNavigation = null;
            navigation.run();
            return;
        }

        updateLayout();
        SoftBackdrop.draw(width, height);
        buttons.clear();
        GL11.glPushMatrix();
        GL11.glTranslatef(originX, originY, 0);
        GL11.glScalef(scale, scale, 1);
        renderMenu(toLocalX(mouseX), toLocalY(mouseY), opacity);
        GL11.glPopMatrix();
        renderScreenOverlay();
        GL11.glColor4f(1, 1, 1, 1);
    }

    protected abstract void renderMenu(float mouseX, float mouseY, float opacity);

    protected void renderScreenOverlay() {
    }

    protected void updateLayout() {
        scale = Math.max(.1f, Math.min(1f, Math.min((width - 24f) / designWidth, (height - 24f) / designHeight)));
        originX = (width - designWidth * scale) / 2f;
        originY = (height - designHeight * scale) / 2f + (1f - visibility.get()) * 8f;
    }

    protected final float toLocalX(int mouseX) {
        return (mouseX - originX) / scale;
    }

    protected final float toLocalY(int mouseY) {
        return (mouseY - originY) / scale;
    }

    protected final void navigate(Runnable action) {
        if (pendingNavigation == null) {
            pendingNavigation = action;
            visibility.animateTo(0, 130);
        }
    }

    protected final boolean isTransitioning() {
        return pendingNavigation != null;
    }

    protected void button(String id, String label, float x, float y, float buttonWidth, float buttonHeight,
                          float mouseX, float mouseY, boolean enabled, boolean primary, Runnable action) {
        Animation hover = hoverAnimations.computeIfAbsent(id, key -> new Animation(0));
        boolean hovered = enabled && RenderUtil.contains(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
        hover.animateTo(hovered ? 1 : 0, 150);
        float active = hover.get();
        int base = primary ? VisualTheme.mix(0xFF202834, VisualTheme.accent(), .18f) : 0xFF202834;
        RenderUtil.drawRoundedRect(x,
                y,
                buttonWidth,
                buttonHeight,
                6,
                AnimationUtil.multiplyAlpha(base, opacity * (enabled ? .85f : .35f)));
        RenderUtil.drawRoundedRect(x,
                y,
                buttonWidth,
                buttonHeight,
                6,
                AnimationUtil.multiplyAlpha(VisualTheme.accent(), opacity * active * .12f));
        CustomFont.drawCenteredString(CustomFont.light(18),
                label,
                x + buttonWidth / 2,
                y + buttonHeight / 2 - 4,
                primary ? VisualTheme.mix(-1, VisualTheme.accent(), .25f) : VisualTheme.TEXT,
                opacity * (enabled ? 1f : .35f));
        if (enabled) {
            buttons.add(new ButtonArea(x, y, buttonWidth, buttonHeight, action));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton != 0 || isTransitioning()) {
            return;
        }
        float localX = toLocalX(mouseX);
        float localY = toLocalY(mouseY);
        for (ButtonArea button : buttons) {
            if (RenderUtil.contains(localX, localY, button.x, button.y, button.width, button.height)) {
                button.action.run();
                return;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class ButtonArea {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final Runnable action;

        private ButtonArea(float x, float y, float width, float height, Runnable action) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.action = action;
        }
    }
}
