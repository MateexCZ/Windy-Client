package Windy.module.impl.ui;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.module.impl.combat.KillAura;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.ui.modern.HudEditor;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.blur.GaussianBlur;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;

@ModuleInfo(name = "TargetHud", description = "Windy target card; drag its preview in chat", category = Category.UI)
public final class TargetHUD extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Windy", "Compact");
    private final BooleanSetting blur = new BooleanSetting("Blur", true);
    private final BooleanSetting linked = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Bar Color", VisualTheme.DEFAULT_ACCENT);
    private final NumberSetting scale = new NumberSetting("Scale", 1, .75, 1, .05);
    private final HudEditor.Anchor position = new HudEditor.Anchor("Target HUD", .56, .56);
    private final Animation visibility = new Animation(0);
    private EntityLivingBase shownTarget;
    private float health, delayedHealth;
    private long lastFrame, damagedAt;

    public TargetHUD() {
        color.addParent(linked, value -> !((BooleanSetting) value).isState());
        addSettings(mode, blur, linked, color, scale, position.x, position.y);
    }

    private EntityLivingBase resolveTarget() {
        if (mc.currentScreen instanceof GuiChat) return mc.thePlayer;
        Module aura = Windy.INSTANCE.getModuleManager().getModule(KillAura.class);
        return aura != null && aura.isToggled() ? KillAura.target : null;
    }

    @Override
    public void on2D(ScaledResolution sr) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo) { reset(); return; }
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000f;
        lastFrame = now;
        EntityLivingBase next = resolveTarget();
        visibility.animateTo(next == null ? 0 : 1, next == null ? 180 : 230);
        if (next != null && shownTarget != next) {
            shownTarget = next;
            health = delayedHealth = fraction(next);
        }
        float opacity = visibility.get();
        if (shownTarget == null || opacity < .005f) {
            if (next == null) shownTarget = null;
            return;
        }
        float desired = fraction(shownTarget);
        if (desired < health - .005f) damagedAt = now;
        health = AnimationUtil.approach(health, desired, dt, 14);
        delayedHealth = Math.max(health, delayedHealth);
        if (now - damagedAt > 250_000_000L) delayedHealth = AnimationUtil.approach(delayedHealth, health, dt, 4);
        float width = 160, height = 49;
        float zoom = (float) scale.getVal();
        float x = position.left(sr, width * zoom), y = position.top(sr, height * zoom);
        position.mark(x, y, width * zoom, height * zoom);
        if (blur.isState() && OpenGlHelper.framebufferSupported && mc.getFramebuffer() != null && opacity > .95f) {
            GaussianBlur.startBlur();
            RenderUtil.drawRoundedRect(x, y, width * zoom, height * zoom, 3, -1);
            GaussianBlur.endBlur(7, 2);
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + (1 - opacity) * 4, 0);
        GlStateManager.scale(zoom, zoom, 1);
        try { drawCard(width, height, opacity); }
        finally { GlStateManager.popMatrix(); }
    }

    private static float fraction(EntityLivingBase target) {
        return Math.max(0, Math.min(1, target.getHealth() / Math.max(1, target.getMaxHealth())));
    }

    private void drawCard(float width, float height, float opacity) {
        int accent = linked.isState() ? VisualTheme.accent() : color.getColor();
        RenderUtil.drawRoundedRect(0, 0, width, height, 3, AnimationUtil.multiplyAlpha(0xDD151B24, opacity));
        RenderUtil.drawBorder(0, 0, width, height, AnimationUtil.multiplyAlpha(0xFF354052, opacity * .7f));
        RenderUtil.drawRect(0, 0, 2, height, AnimationUtil.multiplyAlpha(accent, opacity));
        int faceSize = 29;
        float faceY = 10;
        ResourceLocation skin = shownTarget instanceof AbstractClientPlayer
                ? ((AbstractClientPlayer) shownTarget).getLocationSkin()
                : new ResourceLocation("textures/entity/steve.png");
        drawHead(skin, 10, (int) faceY, faceSize, opacity);
        float textX = 48;
        String name = StringUtils.stripControlCodes(shownTarget.getName());
        CustomFont.drawString(CustomFont.rounded(20),
                CustomFont.trimToWidth(CustomFont.rounded(20), name, width - textX - 10),
                textX,
                11,
                VisualTheme.TEXT,
                opacity);
        String info = String.format(Locale.ROOT,
                "%.1f HP  /  %.1fm",
                Math.max(0, shownTarget.getHealth()),
                mc.thePlayer.getDistanceToEntity(shownTarget));
        CustomFont.drawString(CustomFont.rounded(16), info, textX, 24, VisualTheme.MUTED, opacity);
        float barY = 35, barWidth = width - textX - 10;
        RenderUtil.drawRect(textX, barY, barWidth, 4, AnimationUtil.multiplyAlpha(0xFF303947, opacity));
        RenderUtil.drawRect(textX, barY, barWidth * delayedHealth, 4, AnimationUtil.multiplyAlpha(accent, opacity * .3f));
        int secondary = linked.isState() ? VisualTheme.secondary() : accent;
        RenderUtil.drawGradient(textX,
                barY,
                barWidth * health,
                4,
                AnimationUtil.multiplyAlpha(accent, opacity),
                AnimationUtil.multiplyAlpha(secondary, opacity),
                AnimationUtil.multiplyAlpha(accent, opacity),
                AnimationUtil.multiplyAlpha(secondary, opacity));
        if (shownTarget.getAbsorptionAmount() > 0) {
            float absorption = Math.min(1, shownTarget.getAbsorptionAmount() / Math.max(1, shownTarget.getMaxHealth()));
            RenderUtil.drawRect(textX, barY + 6, barWidth * absorption, 1, AnimationUtil.multiplyAlpha(0xFFE6CD8A, opacity));
        }
    }

    private static void drawHead(ResourceLocation skin, int x, int y, int size, float opacity) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(skin);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1, 1, 1, opacity);
        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
        GlStateManager.color(1, 1, 1, 1);
    }

    private void reset() {
        shownTarget = null;
        visibility.snap(0);
        lastFrame = 0;
    }

    @Override public void onDisable() { reset(); }

    public static void renderSteveModelTexture(double x,
            double y,
            float u,
            float v,
            int uw,
            int vh,
            int width,
            int height,
            float tw,
            float th) {
        drawHead(new ResourceLocation("textures/entity/steve.png"), (int) x, (int) y, width, 1);
    }

    public static void renderPlayerModelTexture(double x,
            double y,
            float u,
            float v,
            int uw,
            int vh,
            int width,
            int height,
            float tw,
            float th,
            AbstractClientPlayer target) {
        drawHead(target.getLocationSkin(), (int) x, (int) y, width, 1);
    }
}
