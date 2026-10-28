package Windy.module.impl.ui;

import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.NumberSetting;
import Windy.ui.modern.HudEditor;
import Windy.util.animation.AnimationUtil;
import Windy.util.blur.GaussianBlur;
import Windy.util.render.RenderUtil;
import Windy.util.render.ClientLogo;
import Windy.util.render.VisualTheme;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "Widgets", description = "Drag enabled HUD widgets while chat is open", category = Category.UI, enabled = true)
public final class Widgets extends Module {
    private final BooleanSetting clientLogo = new BooleanSetting("Client Logo", true);
    private final BooleanSetting logoTheme = new BooleanSetting("Logo Theme Color", true);
    private final BooleanSetting logoLabel = new BooleanSetting("Logo Label", true);
    private final ColorSetting logoColor = new ColorSetting("Logo Color", 0xFFFFFFFF);
    private final HudEditor.Anchor logoPosition = new HudEditor.Anchor("Client Logo", .02, .08);
    private final BooleanSetting keys = new BooleanSetting("Keystrokes", true);
    private final BooleanSetting fps = new BooleanSetting("FPS", false);
    private final BooleanSetting ping = new BooleanSetting("Ping", false);
    private final BooleanSetting cps = new BooleanSetting("CPS", false);
    private final BooleanSetting speed = new BooleanSetting("Speed", false);
    private final BooleanSetting coordinates = new BooleanSetting("Coordinates", false);
    private final BooleanSetting blur = new BooleanSetting("Glass Blur", false);
    private final NumberSetting opacity = new NumberSetting("Glass Opacity", 0.65, 0.1, 1, 0.05);
    private final HudEditor.Anchor[] positions = {new HudEditor.Anchor("Keystrokes", 0.02, 0.24), new HudEditor.Anchor("FPS", 0.02, 0.46), new HudEditor.Anchor("Ping", 0.02, 0.52), new HudEditor.Anchor("CPS", 0.02, 0.58), new HudEditor.Anchor("Speed", 0.02, 0.64), new HudEditor.Anchor("Coordinates", 0.02, 0.7), new HudEditor.Anchor("Armor", 0.02, 0.76), new HudEditor.Anchor("Liquid Bar", 0.5, 0.015)};
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();
    private boolean leftDown;
    private boolean rightDown;
    private final float[] keyBrightness = new float[7];
    private long lastFrame;
    private final String[] labels = new String[8];
    private String clock = "";
    private long nextClockUpdate;
    
    public Widgets() {
        addSettings(keys, fps, ping, cps, speed, coordinates, blur, opacity,
                clientLogo, logoTheme, logoLabel, logoColor, logoPosition.x, logoPosition.y);
        logoTheme.addParent(clientLogo, s -> s.isState());
        logoLabel.addParent(clientLogo, s -> s.isState());
        logoColor.addParent(logoTheme, s -> clientLogo.isState() && !s.isState());
        for (HudEditor.Anchor anchor : positions) addSettings(anchor.x, anchor.y);
    }
    
    @Override
    public void on2D(ScaledResolution sr) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo) return;
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        boolean left = Mouse.isButtonDown(0);
        boolean right = Mouse.isButtonDown(1);
        if (left && !leftDown) leftClicks.addLast(now);
        if (right && !rightDown) rightClicks.addLast(now);
        leftDown = left;
        rightDown = right;
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000000000L) leftClicks.removeFirst();
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000000000L) rightClicks.removeFirst();
        NetworkPlayerInfo info = mc.getNetHandler() == null ? null : mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        String pingText = (mc.isSingleplayer() ? "0" : info == null ? "--" : Integer.toString(info.getResponseTime())) + " ms";
        java.util.Arrays.fill(labels, null);
        if (fps.isState()) labels[1] = Minecraft.getDebugFPS() + " FPS";
        if (ping.isState()) labels[2] = pingText;
        if (cps.isState()) labels[3] = leftClicks.size() + " | " + rightClicks.size() + " CPS";
        if (speed.isState()) {
            double dx = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double dz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            labels[4] = String.format(java.util.Locale.ROOT, "%.1f blocks/s", Math.hypot(dx, dz) * 20);
        }
        if (coordinates.isState()) labels[5] = String.format(java.util.Locale.ROOT, "XYZ  %.0f / %.0f / %.0f", mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        if (blur.isState() && net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled()) blurWidgets(sr);
        if (keys.isState()) drawKeys(sr, dt);
        if (clientLogo.isState()) drawClientLogo(sr);
        for (int index = 1; index < labels.length; index++) {
            if (labels[index] != null) pill(sr, index, labels[index]);
        }
    }
    



    private void blurWidgets(ScaledResolution sr) {
        boolean any = false;
        for (String label : labels) if (label != null) any = true;
        if (!any) return;
        GaussianBlur.startBlur();
        for (int index = 1; index < labels.length; index++) {
            if (labels[index] == null) continue;
            float width = CustomFont.rounded(18).getWidth(labels[index]) + 28;
            RenderUtil.drawRoundedRect(positions[index].left(sr, width), positions[index].top(sr, 27), width, 27, 9, -1);
        }
        GaussianBlur.endBlur(8, 2);
    }
    
    private void glass(float x, float y, float width, float height) {
        float alpha = (float)opacity.getVal();
        RenderUtil.drawRoundedRect(x, y, width, height, 6, AnimationUtil.multiplyAlpha(0xFF16202C, alpha));
        RenderUtil.drawRoundedRect(x + 8, y, width - 16, 1, 0, AnimationUtil.multiplyAlpha(0xFFA4B8CF, alpha * 0.35F));
        RenderUtil.drawRoundedRect(x, y + 7, 2, height - 14, 1, VisualTheme.accent());
    }
    
    private void pill(ScaledResolution sr, int index, String text) {
        float width = CustomFont.rounded(18).getWidth(text) + 28;
        float height = 27;
        HudEditor.Anchor anchor = positions[index];
        float x = anchor.left(sr, width);
        float y = anchor.top(sr, height);
        anchor.mark(x, y, width, height);
        glass(x, y, width, height);
        RenderUtil.drawRoundedRect(x + 8, y + 11, 4, 4, 2, VisualTheme.accent());
        CustomFont.drawString(CustomFont.rounded(18), text, x + 18, y + 9, VisualTheme.TEXT, 1);
    }
    
    private void drawClientLogo(ScaledResolution sr) {
        float imageWidth = 75F;
        float imageHeight = ClientLogo.height(imageWidth);
        String label = "Windy";
        String version = Windy.Windy.INSTANCE.getClientVersion();
        float width = Math.max(imageWidth, logoLabel.isState() ? CustomFont.rounded(18).getWidth(label) : 0);
        float height = imageHeight + (logoLabel.isState() ? 15 : 0);
        float x = logoPosition.left(sr, width), y = logoPosition.top(sr, height);
        logoPosition.mark(x, y, width, height);
        int color = logoTheme.isState() ? VisualTheme.accent() : logoColor.getColor();
        ClientLogo.draw(x + (width - imageWidth) / 2, y, imageWidth, color, 1);
        if (logoLabel.isState()) {
            CustomFont.drawStringWithShadow(CustomFont.LIGHT_52, label,
                    x , y + imageHeight - 4, color);

            CustomFont.drawStringWithShadow(CustomFont.LIGHT_22, version,
                    x + width - 8, y + imageHeight + 16, color);
        }

    }

    private void drawKeys(ScaledResolution sr, float dt) {
        HudEditor.Anchor anchor = positions[0];
        float x = anchor.left(sr, 80);
        float y = anchor.top(sr, 101);
        anchor.mark(x, y, 80, 101);
        KeyBinding[] bindings = {mc.gameSettings.keyBindForward, mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindBack, mc.gameSettings.keyBindRight, mc.gameSettings.keyBindJump};
        String[] labels = {"W", "A", "S", "D", "SPACE", "LMB", "RMB"};
        float[] offsetsX = {27, 0, 27, 54, 0, 0, 41};
        float[] offsetsY = {0, 27, 27, 27, 54, 78, 78};
        for (int i = 0; i < 7; i++) {
            boolean down = i < 5 ? bindings[i].isKeyDown() : Mouse.isButtonDown(i - 5);
            keyBrightness[i] += ((down ? 1 : 0) - keyBrightness[i]) * (1 - (float)Math.exp(-18 * dt));
            float width = i == 4 ? 80 : i > 4 ? 39 : 26;
            float height = i >= 4 ? 22 : 26;
            float kx = x + offsetsX[i];
            float ky = y + offsetsY[i];
            RenderUtil.drawRoundedRect(kx, ky, width, height, 5, 0xB0141922);
            RenderUtil.drawRoundedRect(kx,
                    ky,
                    width,
                    height,
                    5,
                    AnimationUtil.multiplyAlpha(VisualTheme.accent(), keyBrightness[i] * 0.7F));
            CustomFont.drawCenteredString(CustomFont.rounded(16), labels[i], kx + width / 2, ky + height / 2 - 4, VisualTheme.TEXT, 1);
        }
    }
    
    @Override
    public void onDisable() {
        leftClicks.clear();
        rightClicks.clear();
        lastFrame = 0;
    }
}
