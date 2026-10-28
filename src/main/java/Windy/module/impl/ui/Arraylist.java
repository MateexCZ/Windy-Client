package Windy.module.impl.ui;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.font.fontmanager.TTFFontRenderer;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.Setting;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.ColorSetting;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import Windy.ui.modern.HudEditor;
import Windy.util.animation.AnimationUtil;
import Windy.util.blur.GaussianBlur;
import Windy.util.render.ColorUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.gui.ScaledResolution;

@ModuleInfo(
        name = "Arraylist",
        category = Category.UI,
        description = "Displays modules on screen",
        enabled = true
)
public class Arraylist extends Module {
    private static final TTFFontRenderer FONT = CustomFont.rounded(18);

    public static final ModeSetting arraylistRect = new ModeSetting("ArrayList Rect",  "None", "Top", "Side");
    public static final ModeSetting arraylistColor = new ModeSetting("ArrayList Color",
            "Gray-White",
            "Rainbow",
            "Tenacity",
            "Theme",
            "Custom",
            "Gradient");
    public static final ModeSetting arraylistAlign = new ModeSetting("ArrayList Align",  "Right", "Left");

    public static final BooleanSetting blurbg = new BooleanSetting("Blured background", false);

    private final List<ModuleInfoData> layoutCache = new ArrayList<>();
    private final List<ModuleInfoData> visibleRows = new ArrayList<>();
    private final Map<Module, ModuleInfoData> layoutByModule = new HashMap<>();
    private long nextLayoutCheck;
    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final BooleanSetting suffixes = new BooleanSetting("Show Suffix", true);
    private final BooleanSetting background = new BooleanSetting("Background", true);
    private final BooleanSetting hideRender = new BooleanSetting("Hide Visual Modules", false);
    private final NumberSetting listOpacity = new NumberSetting("Background Opacity", .65, 0, 1, .05);
    private final ColorSetting custom = new ColorSetting("List Color", VisualTheme.DEFAULT_ACCENT);
    private final ColorSetting secondary = new ColorSetting("Gradient Color", 0xFFE0AAED);
    private final ColorSetting backgroundColor = new ColorSetting("Background Color", 0xFF0C0A10);
    private final HudEditor.Anchor listPosition = new HudEditor.Anchor("Arraylist", 1, .01);
    private final HudEditor.Anchor watermarkPosition = new HudEditor.Anchor("Watermark", .01, .01);
    private boolean cachedSuffixes;

    public Arraylist() {
        addSettings(arraylistRect, arraylistColor, arraylistAlign, blurbg, watermark, suffixes,
                background, hideRender, listOpacity, custom, secondary, backgroundColor,
                listPosition.x, listPosition.y, watermarkPosition.x, watermarkPosition.y);
    }

    @Override
    public void on2D(ScaledResolution sr) {
        drawArrayList(sr);
    }

    private void refreshLayoutCache() {
        long now = System.nanoTime();
        if (now < nextLayoutCheck) {
            return;
        }
        nextLayoutCheck = now + 100_000_000L;
        boolean changed = layoutByModule.keySet().removeIf(module ->
                !Windy.INSTANCE.getModuleManager().getModules().containsKey(module.getClass()));
        for (Module module : Windy.INSTANCE.getModuleManager().getModules().values()) {
            if (module.getName().equalsIgnoreCase("clickgui") || module == this) {
                continue;
            }
            String suffix = suffixes.isState() ? getModuleSuffix(module) : null;
            ModuleInfoData previous = layoutByModule.get(module);
            if (previous != null && Objects.equals(previous.suffix, suffix)) {
                continue;
            }
            float nameWidth = FONT.getWidth(module.getName());
            float spaceWidth = suffix == null || suffix.isEmpty() ? 0 : FONT.getWidth(" ");
            float suffixWidth = suffix == null ? 0 : FONT.getWidth(suffix);
            float rowHeight = FONT.getHeight() + 3.5f;
            layoutByModule.put(module, new ModuleInfoData(module, nameWidth + spaceWidth + suffixWidth + 10,
                    rowHeight, module.getName(), suffix, nameWidth, spaceWidth));
            changed = true;
        }
        if (changed) {
            layoutCache.clear();
            layoutCache.addAll(layoutByModule.values());
            layoutCache.sort(Comparator.comparingDouble((ModuleInfoData row) -> row.rectWidth).reversed());
        }
    }

    private void drawArrayList(ScaledResolution resolution) {
        refreshLayoutCache();
        visibleRows.clear();
        boolean leftAligned = arraylistAlign.getCurrMode().equalsIgnoreCase("Left");
        float widest = 0;
        float totalHeight = 0;
        for (ModuleInfoData row : layoutCache) {
            if (hideRender.isState() && row.module.getCategory() == Category.UI) continue;
            if (row.module.isToggled()) {
                widest = Math.max(widest, row.rectWidth + 4);
                totalHeight += row.itemHeight;
            }
        }
        float anchorX = listPosition.left(resolution, widest);
        float anchorY = listPosition.top(resolution, totalHeight);
        listPosition.mark(anchorX, anchorY, widest, totalHeight);
        float y = 0;
        for (ModuleInfoData row : layoutCache) {
            if (hideRender.isState() && row.module.getCategory() == Category.UI) continue;
            row.module.anim.animateTo(row.module.isToggled() ? 0 : 150);
            float animation = row.module.anim.get();
            if (!row.module.isToggled() && animation >= 149f) {
                continue;
            }
            row.x = leftAligned ? 3 - animation : widest - row.rectWidth + animation;
            row.y = y;
            y += row.itemHeight;
            visibleRows.add(row);
        }
        if (visibleRows.isEmpty()) {
            return;
        }
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(anchorX, anchorY, 0);
        if (blurbg.isState() && net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled()) {
            GaussianBlur.startBlur();
            for (ModuleInfoData row : visibleRows) {
                RenderUtil.drawRect(row.x - 2, row.y, row.rectWidth - 5, row.itemHeight, -1);
            }
            org.lwjgl.opengl.GL11.glPopMatrix();
            GaussianBlur.endBlur(20, 2);
            org.lwjgl.opengl.GL11.glPushMatrix();
            org.lwjgl.opengl.GL11.glTranslatef(anchorX, anchorY, 0);
        }
        int index = 0;
        for (ModuleInfoData row : visibleRows) {
            int fill = AnimationUtil.multiplyAlpha(backgroundColor.getColor(), (float) listOpacity.getVal());
            if (background.isState()) RenderUtil.drawRect(row.x - 2, row.y, row.rectWidth - 5, row.itemHeight, fill);
            float textY = row.y + (row.itemHeight - FONT.getHeight()) / 2 + 2;
            Color color = getColorByOffset(textY, 1f, .008f, index);
            FONT.drawString(row.name, row.x, textY, color.getRGB());
            if (row.suffix != null && !row.suffix.isEmpty()) {
                FONT.drawString(row.suffix, row.x + row.nameWidth + row.spaceWidth, textY, 0xFFAAAAAA);
            }
            if (arraylistRect.getCurrMode().equals("Top") && index == 0) {
                RenderUtil.drawRect(row.x - 2, row.y - 1, row.rectWidth - 5, 1, color.getRGB());
            } else if (arraylistRect.getCurrMode().equals("Side")) {
                float barX = leftAligned ? row.x - 3 : row.x + row.rectWidth - 7;
                RenderUtil.drawRect(barX, row.y, 2, row.itemHeight, color.getRGB());
            }
            index++;
        }
        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    private void drawHorizontalBar(float x, float y, float width, float height, float speed, float step) {
        int left = getColorByOffset(x, speed, step, 0).getRGB();
        int right = getColorByOffset(x + width, speed, step, 0).getRGB();
        RenderUtil.drawHorizontalGradient(Math.round(x), Math.round(y), Math.round(width), Math.round(height), left, right);
    }

    public Color getColorByOffset(float offset, float speed, float step, int index) {
        if (arraylistColor.getCurrMode().equals("Theme")) return new Color(VisualTheme.color(index * .045f), true);
        if (arraylistColor.getCurrMode().equals("Custom")) return custom.asColor();
        if (arraylistColor.getCurrMode().equals("Gradient")) {
            return ColorUtil.interpolateColorsBackAndForth(15, index * 10, custom.asColor(), secondary.asColor(), false);
        }
        if (arraylistColor.getCurrMode().equalsIgnoreCase("Rainbow")) {
            double timeProgress = ((System.currentTimeMillis() % 6000L) / 6000.0);
            float hue = (float) ((timeProgress - (index * 0.04f) + (offset * 0.002f)) % 1.0);
            if (hue < 0) hue += 1f;
            return Color.getHSBColor(hue, 0.85f, 1.0f);
        } else if (arraylistColor.getCurrMode().equalsIgnoreCase("Tenacity")) {
            Color color1 = new Color(130, 170, 255);
            Color color2 = new Color(240, 120, 255);
            return ColorUtil.interpolateColorsBackAndForth(15, index * 10, color1, color2, false);
        } else {
            double timeProgress = (System.currentTimeMillis() % 4000L) / 4000.0;
            float progress = (float) ((timeProgress + (index * 0.04f) + (offset * 0.002f)) % 1.0);
            if (progress < 0) progress += 1.0f;

            progress = progress > 0.5f ? 2.0f - (progress * 2.0f) : progress * 2.0f;

            int red = (int) (180 + (255 - 180) * progress);
            int green = (int) (180 + (255 - 180) * progress);
            int blue = (int) (180 + (255 - 180) * progress);

            return new Color(red, green, blue);
        }
    }

    private String getModuleSuffix(Module mod) {
        if (mod == null || mod.getSettingList() == null) return null;

        for (Setting setting : mod.getSettingList()) {
            if (setting instanceof ModeSetting) {
                return ((ModeSetting) setting).getCurrMode();
            }
        }
        return null;
    }

    private static class ModuleInfoData {
        private float x;
        private float y;
        private final Module module;
        private final float rectWidth;
        private final float itemHeight;
        private final String name;
        private final String suffix;
        private final float nameWidth;
        private final float spaceWidth;

        public ModuleInfoData(Module module,
                float rectWidth,
                float itemHeight,
                String name,
                String suffix,
                float nameWidth,
                float spaceWidth) {
            this.module = module;
            this.rectWidth = rectWidth;
            this.itemHeight = itemHeight;
            this.name = name;
            this.suffix = suffix;
            this.nameWidth = nameWidth;
            this.spaceWidth = spaceWidth;
        }
    }
}
