package Windy.ui.clickgui;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.ui.clickgui.components.Header;
import Windy.ui.clickgui.components.Sidebar;
import Windy.ui.clickgui.components.module.ModuleCard;
import Windy.ui.modern.ColorPopup;
import Windy.ui.modern.ThemesPanel;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class ClickGUIScreen extends GuiScreen {

    private static final int PANEL_W = 560;
    private static final int PANEL_H = 350;
    private static final int SIDEBAR_W = 125;

    private final Sidebar sidebar = new Sidebar();
    private final Header header = new Header();
    private static final List<ModuleCard> cardComponents = new ArrayList<>();

    private final Animation openAnim = new Animation(220, Easing.EASE_OUT_CUBIC, 0f);

    private int panelX, panelY;
    private long lastFrame;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    private static boolean memoryInitialized = false;

    public ClickGUIScreen() {
        if (cardComponents.isEmpty()) {
            for (Module m : Windy.INSTANCE.getModuleManager().getModules().values()) {
                ModuleCard card = new ModuleCard(m);
                if (m.getName().equalsIgnoreCase("Scaffold")) {
                    card.setExpanded(true);
                }
                cardComponents.add(card);
            }
        }

        if (!memoryInitialized) {
            sidebar.setSelectedCategoryIndex(0);
            memoryInitialized = true;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        openAnim.snap(0f);
        openAnim.animateTo(1f);

        Minecraft mcInst = mc != null ? mc : Windy.INSTANCE.getMc();
        ScaledResolution sr = new ScaledResolution(mcInst);

        panelX = (sr.getScaledWidth() - PANEL_W) / 2;
        panelY = (sr.getScaledHeight() - PANEL_H) / 2;

        sidebar.init(panelY);
        header.setFocused(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float alpha = openAnim.get();

        RenderUtil.drawRect(0, 0, width, height, AnimationUtil.multiplyAlpha(0x90000000, alpha));
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000f;
        lastFrame = now;
        scrollOffset = AnimationUtil.smoothExp(scrollOffset, targetScrollOffset, 18f, Math.min(.1f, dt));

        RenderUtil.drawRoundedRect(panelX, panelY, PANEL_W, PANEL_H, 10, AnimationUtil.multiplyAlpha(0xF70B0C0E, alpha));

        sidebar.draw(panelX, panelY, SIDEBAR_W, PANEL_H, mouseX, mouseY, alpha);
        if (isThemes()) CustomFont.drawString(CustomFont.light(20), "Themes", panelX + SIDEBAR_W + 14, panelY + 17, -1, alpha);
        else header.draw(panelX + SIDEBAR_W + 14, panelY + 14, PANEL_W - SIDEBAR_W - 28, 22, alpha);

        if (isThemes()) ThemesPanel.draw(panelX + SIDEBAR_W + 14, panelY + 48,
                PANEL_W - SIDEBAR_W - 28, PANEL_H - 58, mouseX, mouseY, alpha);
        else drawModuleList(mouseX, mouseY, alpha);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawModuleList(int mouseX, int mouseY, float alpha) {
        int contentX = panelX + SIDEBAR_W + 14;
        int contentY = panelY + 44;
        int contentW = PANEL_W - SIDEBAR_W - 28;
        int contentH = PANEL_H - 54;
        int maxAllowedY = contentY + contentH;

        pushScissor(contentX, contentY, contentW, contentH);

        int curY = contentY - (int) scrollOffset;
        int totalContentH = 0;

        for (ModuleCard card : getFilteredCards()) {
            int cardH = card.draw(contentX, curY, contentW, mouseX, mouseY, alpha, contentY, maxAllowedY);
            curY += cardH + 6;
            totalContentH += cardH + 6;
        }

        popScissor();

        int maxScroll = Math.max(0, totalContentH - contentH);
        if (targetScrollOffset > maxScroll) targetScrollOffset = maxScroll;
        if (targetScrollOffset < 0) targetScrollOffset = 0;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int searchX = panelX + SIDEBAR_W + 14;
        int searchY = panelY + 14;
        int searchW = PANEL_W - SIDEBAR_W - 28;
        int searchH = 22;

        if (!isThemes() && header.mouseClicked(mouseX, mouseY, mouseButton, searchX, searchY, searchW, searchH)) {
            clearAllBindings();
            return;
        }

        if (sidebar.mouseClicked(panelX, panelY, SIDEBAR_W, mouseX, mouseY, mouseButton)) {
            targetScrollOffset = 0;
            header.setFocused(false);
            clearAllBindings();
            return;
        }

        if (isThemes()) {
            ThemesPanel.click(panelX + SIDEBAR_W + 14, panelY + 48,
                    PANEL_W - SIDEBAR_W - 28, PANEL_H - 58, mouseX, mouseY, mouseButton);
            return;
        }
        int contentX = panelX + SIDEBAR_W + 14;
        int contentY = panelY + 44;
        int contentW = PANEL_W - SIDEBAR_W - 28;
        int contentH = PANEL_H - 54;
        int curY = contentY - (int) scrollOffset;

        boolean insideContent = mouseX >= contentX && mouseX <= contentX + contentW && mouseY >= contentY && mouseY <= contentY + contentH;

        if (insideContent) {
            for (ModuleCard card : getFilteredCards()) {
                if (card.mouseClicked(contentX, curY, contentW, mouseX, mouseY, mouseButton)) {
                    return;
                }
                curY += card.getHeight() + 6;
            }
        }

        clearAllBindings();
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for (ModuleCard card : cardComponents) {
            card.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!isThemes() && header.keyTyped(typedChar, keyCode)) {
            return;
        }

        for (ModuleCard card : cardComponents) {
            card.keyTyped(typedChar, keyCode);
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (ColorPopup.isOpen()) { super.handleMouseInput(); return; }
        int dWheel = Mouse.getEventDWheel();
        if (!isThemes() && dWheel != 0) {
            targetScrollOffset += (dWheel > 0) ? -32f : 32f;
        }
        super.handleMouseInput();
    }

    private boolean isThemes() { return sidebar.getSelectedCategoryIndex() == Category.values().length; }

    private List<ModuleCard> getFilteredCards() {
        List<ModuleCard> filtered = new ArrayList<>();
        int selectedCat = sidebar.getSelectedCategoryIndex();
        if (isThemes()) return filtered;
        String query = header.getSearchQuery().toLowerCase(Locale.ROOT);

        for (ModuleCard card : cardComponents) {
            Module m = card.getModule();

            if (selectedCat != -1 && Category.values()[selectedCat] != m.getCategory()) {
                continue;
            }

            if (!query.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(query)
                    && (m.getDescription() == null || !m.getDescription().toLowerCase(Locale.ROOT).contains(query))) {
                continue;
            }

            filtered.add(card);
        }
        return filtered;
    }

    private void clearAllBindings() {
        for (ModuleCard card : cardComponents) {
            card.clearBindings();
        }
    }

    private void pushScissor(double x, double y, double w, double h) {
        ScaledResolution sr = new ScaledResolution(mc != null ? mc : Minecraft.getMinecraft());
        int factor = sr.getScaleFactor();

        int sx = (int) (x * factor);
        int sy = (int) (mc.displayHeight - ((y + h) * factor));
        int sw = (int) (w * factor);
        int sh = (int) (h * factor);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, sy, Math.max(0, sw), Math.max(0, sh));
    }

    private void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
