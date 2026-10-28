package Windy.ui.mainmenu;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.ui.auth.AltManagerGui;
import Windy.ui.modern.ModernScreen;
import Windy.util.render.RenderUtil;
import Windy.util.render.ClientLogo;
import Windy.util.render.VisualTheme;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;

public final class MainMenu extends ModernScreen {
    private static boolean startupAnimationPending;
    private boolean playStartupAnimation;
    private StartupScreen.Timeline startup;
    private long startupFrame;

    public static void prepareStartupAnimation(boolean enabled) {
        startupAnimationPending = enabled;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (startupAnimationPending) {
            playStartupAnimation = true;
            startupAnimationPending = false;

            CustomFont.comfortaa(16);
        }
    }

    @Override
    protected void renderScreenOverlay() {
        if (startup == null) return;
        float move = startup.move(startupFrame);
        StartupScreen.drawProgress(width, height, startup.progress(startupFrame), startup.barOpacity(startupFrame));
        float centerX = width / 2f + (originX + (StartupScreen.menuLogoX() + StartupScreen.MENU_ICON_WIDTH / 2) * scale - width / 2f) * move;
        float fromY = StartupScreen.logoTop(width, height);
        float top = fromY + (originY + StartupScreen.menuLogoTop() * scale - fromY) * move;
        float fromScale = StartupScreen.logoScale(width);
        float logoScale = fromScale + (scale - fromScale) * move;
        StartupScreen.drawLogo(centerX, top, logoScale,
                VisualTheme.mix(StartupScreen.LOGO_COLOR, VisualTheme.accent(), move));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        if (playStartupAnimation || startup != null) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        if (playStartupAnimation || startup != null) return;
        super.keyTyped(typedChar, keyCode);
    }

    public MainMenu() {
        super(380, 360);
    }

    @Override
    protected void renderMenu(float mouseX, float mouseY, float opacity) {
        startupFrame = System.nanoTime();
        if (playStartupAnimation) {
            startup = new StartupScreen.Timeline(startupFrame);
            playStartupAnimation = false;
        }
        if (startup != null && startup.isFinished(startupFrame)) startup = null;
        if (startup == null) {
            ClientLogo.draw(StartupScreen.menuLogoX(), StartupScreen.menuLogoTop(),
                    StartupScreen.MENU_ICON_WIDTH, VisualTheme.accent(), opacity);
        } else {
            opacity *= startup.menuOpacity(startupFrame);
        }

        this.opacity = opacity;
        CustomFont.drawString(CustomFont.comfortaa(48), "Windy",
                StartupScreen.MENU_ICON_WIDTH + (StartupScreen.menuLogoX() / 2) - 15, 46, VisualTheme.TEXT, opacity);
        CustomFont.drawCenteredString(CustomFont.comfortaa(18), "version " + Windy.INSTANCE.getClientVersion(), 190, 74, VisualTheme.MUTED, opacity);
        RenderUtil.drawPanel(38, 99, 304, 212, opacity);

        button("singleplayer", "Singleplayer", 57, 119, 266, 34, mouseX, mouseY, true, true,
                () -> navigate(() -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        button("multiplayer", "Multiplayer", 57, 162, 266, 34, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        button("accounts", "Accounts", 57, 205, 266, 34, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new AltManagerGui())));
        button("settings", "Settings", 57, 258, 128, 31, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        button("exit", "Exit", 195, 258, 128, 31, mouseX, mouseY, true, false,
                () -> navigate(mc::shutdown));

        CustomFont.drawCenteredString(CustomFont.comfortaa(16),
                "Playing as " + mc.getSession().getUsername(),
                190,
                330,
                VisualTheme.MUTED,
                opacity * .8f);
    }
}
