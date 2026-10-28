package Windy.ui.clickgui.DropDowns;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.impl.ui.ClickGUI;
import Windy.module.impl.ui.Visuals;
import Windy.setting.Setting;
import Windy.setting.impl.*;
import Windy.ui.modern.*;
import Windy.ui.modern.ColorPopup;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;


public final class DropdownPanel {
    private static final float WIDTH = 148, HEADER = 27;
    private final Category category;
    private final List<Module> modules = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Map<Module, Float> enabled = new IdentityHashMap<>();
    private final Animation body = new Animation(0);
    private final Animation contentOpacity = new Animation(1);
    private final Map<Module, Float> hovers = new IdentityHashMap<>();
    private float positionX, positionY, viewportLimit = 300, viewportHeight;
    private float scroll, targetScroll, contentHeight;
    private float dragX, dragY;
    private boolean dragging, expanded = true;
    private Module expandedModule, binding;
    private NumberSetting sliding;
    private ModeSetting expandedMode;
    private long lastFrame;

    public DropdownPanel(Category category, float x, float y) {
        this.category = category;
        positionX = x;
        positionY = y;
        for (Module module : Windy.INSTANCE.getModuleManager().getModules().values()) {
            if (module.getCategory() == category) modules.add(module);
        }
        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public static DropdownPanel themes(float x, float y) {
        return new DropdownPanel(null, x, y);
    }

    public void setViewportLimit(float height) { viewportLimit = Math.max(60, height); }


    public void cancelInteraction() {
        dragging = false;
        sliding = null;
        binding = null;
        lastFrame = 0;
        hovers.clear();
    }


    public void prepareForOpen() {
        cancelInteraction();
        positionX = Math.max(DropdownScreen.minPanelX(), Math.min(DropdownScreen.maxPanelX(WIDTH), positionX));
        positionY = Math.max(DropdownScreen.minPanelY(), Math.min(DropdownScreen.maxPanelY(HEADER), positionY));
        layout();
        scroll = targetScroll;
        viewportHeight = expanded ? Math.min(contentHeight, viewportLimit) : 0;
        body.snap(viewportHeight);
        contentOpacity.snap(expanded ? 1 : 0);
    }

    private void layout() {
        rows.clear();
        float y = 0;
        if (category == null) {
            for (int i = 0; i < ThemePalette.NAMES.length; i++) {
                rows.add(Row.theme(i, y));
                y += 43;
            }
            rows.add(new Row(null, Visuals.accent, y, 22));
            rows.add(new Row(null, Visuals.secondary, y + 22, 22));
            y += 44;
        } else {
            for (Module module : modules) {
                rows.add(new Row(module, null, y, 22));
                y += 22;
                if (module != expandedModule) continue;
                for (Setting setting : module.getSettingList()) {
                    if (!setting.isVisible()) continue;
                    float height = setting instanceof NumberSetting ? 31 : 22;
                    rows.add(new Row(module, setting, y, height));
                    y += height;
                    if (setting == expandedMode) {
                        for (String choice : expandedMode.getModes()) {
                            rows.add(Row.choice(module, expandedMode, choice, y));
                            y += 20;
                        }
                    }
                }
            }
        }
        contentHeight = y;
        targetScroll = Math.max(0, Math.min(targetScroll, contentHeight - viewportLimit));
    }

    public void draw(int mouseX, int mouseY, float alpha) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : (now - lastFrame) / 1_000_000_000f;
        lastFrame = now;
        if (ColorPopup.isOpen() || !Mouse.isButtonDown(0)) { dragging = false; sliding = null; }
        if (dragging) {
            positionX = Math.max(DropdownScreen.minPanelX(), Math.min(DropdownScreen.maxPanelX(WIDTH), mouseX - dragX));
            positionY = Math.max(DropdownScreen.minPanelY(), Math.min(DropdownScreen.maxPanelY(HEADER), mouseY - dragY));
        }
        if (sliding != null) slide(mouseX, sliding);
        layout();
        scroll = AnimationUtil.approach(scroll, targetScroll, dt, 18);
        body.animateTo(expanded ? Math.min(contentHeight, viewportLimit) : 0, 180);
        viewportHeight = body.get();
        contentOpacity.animateTo(expanded ? 1 : 0, 180);
        int accent = VisualTheme.color(positionY * .002f) | 0xFF000000;
        RenderUtil.drawRect(positionX, positionY, WIDTH, HEADER, surface(0xFF202631, alpha));
        RenderUtil.drawRect(positionX + 10, positionY + HEADER - 1, WIDTH - 20, 1, AnimationUtil.multiplyAlpha(accent, alpha * .6f));
        text(category == null ? "Themes" : category.name(), positionX + 10, positionY + 9, 20, VisualTheme.TEXT, alpha);
        text(expanded ? "-" : "+", positionX + WIDTH - 17, positionY + 9, 18, VisualTheme.MUTED, alpha);
        if (viewportHeight <= .1f) return;
        alpha *= contentOpacity.get();

        DropdownScreen.clip(positionX + 1, positionY + HEADER, WIDTH - 2, viewportHeight);
        try {
            for (Row row : rows) {
                float y = positionY + HEADER + row.y - scroll;
                if (y + row.height < positionY + HEADER || y > positionY + HEADER + viewportHeight) continue;
                if (row.themeIndex >= 0) drawTheme(row, y, mouseX, mouseY, alpha);
                else if (row.choice != null) drawChoice(row, y, mouseX, mouseY, alpha);
                else if (row.setting == null) drawModule(row, y, mouseX, mouseY, alpha, dt);
                else drawSetting(row, y, alpha);
            }
            if (contentHeight > viewportLimit && expandedModule == null) {
                float track = viewportHeight;
                float thumb = Math.max(12, track * track / contentHeight);
                RenderUtil.drawRect(positionX + WIDTH - 3,
                        positionY + HEADER + (track - thumb) * scroll / Math.max(1, contentHeight - viewportLimit),
                        1,
                        thumb,
                        AnimationUtil.multiplyAlpha(accent, alpha * .6f));
            }
        } finally { GL11.glDisable(GL11.GL_SCISSOR_TEST); }
    }

    private void drawModule(Row row, float y, int mx, int my, float alpha, float dt) {
        Module module = row.module;
        float value = AnimationUtil.approach(enabled.getOrDefault(module, module.isToggled() ? 1f : 0f),
                module.isToggled() ? 1 : 0,
                dt,
                15);
        enabled.put(module, value);
        int accent = VisualTheme.color(y * .002f) | 0xFF000000;
        RenderUtil.drawRect(positionX + 1, y, WIDTH - 2, row.height, surface(0xFF151A23, alpha));
        boolean hovered = !ColorPopup.isOpen() && RenderUtil.contains(mx, my, positionX + 1, y, WIDTH - 2, row.height);
        float hover = AnimationUtil.approach(hovers.getOrDefault(module, 0f), hovered ? 1 : 0, dt, 18);
        hovers.put(module, hover);
        RenderUtil.drawRect(positionX + 1, y, WIDTH - 2, row.height, AnimationUtil.multiplyAlpha(0xFF242B37, alpha * hover * .65f));
        if (value > .01f) {
            RenderUtil.drawRect(positionX + 1, y, WIDTH - 2, row.height, AnimationUtil.multiplyAlpha(accent, alpha * value * .10f));
            RenderUtil.drawRect(positionX + 1, y + 5, 2, 12, AnimationUtil.multiplyAlpha(accent, alpha * value));
        }
        String label = binding == module ? "Press a key..." : module.getName();
        text(CustomFont.trimToWidth(CustomFont.light(18), label, WIDTH - 35),
                positionX + 10,
                y + 7,
                18,
                VisualTheme.mix(0xFFD0D5DF, accent, value),
                alpha);
        text(module == expandedModule ? "-" : "+", positionX + WIDTH - 17, y + 7, 16, VisualTheme.MUTED, alpha);
    }

    private void drawSetting(Row row, float y, float alpha) {
        Setting setting = row.setting;
        int accent = VisualTheme.color(y * .002f) | 0xFF000000;
        RenderUtil.drawRect(positionX + 5, y, WIDTH - 10, row.height, surface(0xFF10151D, alpha));
        String value = "";
        if (setting instanceof BooleanSetting) value = ((BooleanSetting) setting).isState() ? "On" : "Off";
        else if (setting instanceof ModeSetting) value = ((ModeSetting) setting).getCurrMode();
        else if (setting instanceof KeybindSetting) value = binding == row.module ? "..." : Keyboard.getKeyName(row.module.getKey());
        else if (setting instanceof NumberSetting) value = String.format(Locale.ROOT,
                "%.2f",
                ((NumberSetting) setting).getVal()).replaceAll("0+$",
                "").replaceAll("\\.$",
                "");
        if (value == null) value = "None";
        float valueWidth = Math.min(64, CustomFont.light(16).getWidth(value));
        text(CustomFont.trimToWidth(CustomFont.light(16), setting.getName(), WIDTH - valueWidth - 30),
                positionX + 12,
                y + 7,
                16,
                VisualTheme.MUTED,
                alpha);
        if (setting instanceof ColorSetting) {
            RenderUtil.drawCheckerboard(positionX + WIDTH - 31, y + 6, 18, 10);
            RenderUtil.drawRect(positionX + WIDTH - 31,
                    y + 6,
                    18,
                    10,
                    AnimationUtil.multiplyAlpha(((ColorSetting) setting).getColor(), alpha));
        } else text(CustomFont.trimToWidth(CustomFont.light(16), value, 64), positionX + WIDTH - valueWidth - 12, y + 7, 16, accent, alpha);
        if (setting instanceof NumberSetting) {
            NumberSetting number = (NumberSetting) setting;
            float fraction = (float) ((number.getVal() - number.getMinVal()) / Math.max(.0001, number.getMaxVal() - number.getMinVal()));
            RenderUtil.drawRect(positionX + 12, y + 23, WIDTH - 24, 2, AnimationUtil.multiplyAlpha(0xFF364050, alpha));
            RenderUtil.drawRect(positionX + 12, y + 23, (WIDTH - 24) * fraction, 2, AnimationUtil.multiplyAlpha(accent, alpha));
        }
    }


    private static int surface(int color, float fade) {
        return AnimationUtil.multiplyAlpha(color | 0xFF000000, fade * (float) ClickGUI.dropdownOpacity.getVal());
    }

    private static void text(String value, float x, float y, int size, int color, float opacity) {

        boolean alphaTest = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        GlStateManager.disableAlpha();
        try { CustomFont.light(size).drawStringWithShadow(value, x, y, AnimationUtil.multiplyAlpha(color, opacity)); }
        finally { if (alphaTest) GlStateManager.enableAlpha(); }
    }

    private void drawChoice(Row row, float y, int mouseX, int mouseY, float alpha) {
        boolean selected = row.choice.equals(((ModeSetting) row.setting).getCurrMode());
        boolean hovered = RenderUtil.contains(mouseX, mouseY, positionX + 8, y, WIDTH - 16, row.height);
        RenderUtil.drawRect(positionX + 8, y, WIDTH - 16, row.height, surface(hovered ? 0xFF232B38 : 0xFF10151D, alpha));
        int color = selected ? VisualTheme.accent() | 0xFF000000 : VisualTheme.MUTED;
        if (selected) RenderUtil.drawRect(positionX + 12, y + 8, 3, 3, AnimationUtil.multiplyAlpha(color, alpha));
        text(CustomFont.trimToWidth(CustomFont.light(16), row.choice, WIDTH - 37), positionX + 21, y + 6, 16, color, alpha);
    }

    private void drawTheme(Row row, float y, int mouseX, int mouseY, float alpha) {
        int index = row.themeIndex;
        int primary = AnimationUtil.multiplyAlpha(VisualTheme.preview(ThemePalette.COLORS[index][0], ThemePalette.COLORS[index][1], 0),
                alpha);
        int secondary = AnimationUtil.multiplyAlpha(VisualTheme.preview(ThemePalette.COLORS[index][0], ThemePalette.COLORS[index][1], .5f),
                alpha);
        boolean selected = ThemePalette.selected() == index;
        boolean hovered = RenderUtil.contains(mouseX, mouseY, positionX, y, WIDTH, row.height);
        RenderUtil.drawRect(positionX + 1, y, WIDTH - 2, row.height, surface(hovered ? 0xFF232B38 : 0xFF151A23, alpha));
        RenderUtil.drawGradient(positionX + 10, y + 6, WIDTH - 20, 10, primary, secondary, primary, secondary);
        text(ThemePalette.NAMES[index], positionX + 10, y + 24, 16,
                selected ? VisualTheme.accent() | 0xFF000000 : VisualTheme.TEXT, alpha);
        if (selected) RenderUtil.drawRect(positionX + WIDTH - 15, y + 27, 4, 4, AnimationUtil.multiplyAlpha(VisualTheme.accent(), alpha));
    }

    public boolean mouseClicked(int mx, int my, int button) {
        if (RenderUtil.contains(mx, my, positionX, positionY, WIDTH, HEADER)) {
            if (button == 0) { dragging = true; dragX = mx - positionX; dragY = my - positionY; }
            else if (button == 1) { expanded = !expanded; sliding = null; }
            return true;
        }
        if (!expanded || !RenderUtil.contains(mx, my, positionX, positionY + HEADER, WIDTH, viewportHeight)) return false;
        for (Row row : rows) {
            float y = positionY + HEADER + row.y - scroll;
            if (!RenderUtil.contains(mx, my, positionX, y, WIDTH, row.height)) continue;
            if (row.themeIndex >= 0) {
                if (button == 0) ThemePalette.apply(row.themeIndex);
            } else if (row.choice != null) {
                if (button == 0) {
                    ModeSetting mode = (ModeSetting) row.setting;
                    mode.setCurrMode(row.choice);
                    mode.setModeIndex(mode.getModes().indexOf(row.choice));
                    expandedMode = null;
                }
            } else if (row.setting == null) {
                if (button == 0) row.module.toggle();
                else if (button == 1) {
                    expandedModule = expandedModule == row.module ? null : row.module;
                    expandedMode = null;
                    targetScroll = 0;
                }
                else if (button == 2) binding = row.module;
            } else if (row.setting instanceof ColorSetting) ColorPopup.open((ColorSetting) row.setting);
            else if (row.setting instanceof KeybindSetting) binding = row.module;
            else if (row.setting instanceof BooleanSetting && button == 0) {
                BooleanSetting value = (BooleanSetting) row.setting;
                value.setState(!value.isState());
            } else if (row.setting instanceof ModeSetting) {
                if (button == 0 || button == 1) {
                    ModeSetting value = (ModeSetting) row.setting;
                    expandedMode = expandedMode == value ? null : value;
                }
            } else if (row.setting instanceof NumberSetting && button == 0) { sliding = (NumberSetting) row.setting; slide(mx, sliding); }
            return true;
        }
        return true;
    }

    private void slide(float mx, NumberSetting number) {
        double fraction = Math.max(0, Math.min(1, (mx - positionX - 12) / (WIDTH - 24)));
        double value = number.getMinVal() + (number.getMaxVal() - number.getMinVal()) * fraction;
        double step = number.getIncrement();
        if (step > 0) value = number.getMinVal() + Math.round((value - number.getMinVal()) / step) * step;
        number.setVal(value);
    }

    public void mouseReleased(int x, int y, int button) { dragging = false; sliding = null; }

    public boolean keyTyped(char character, int key) {
        if (binding == null) {
            if (key == Keyboard.KEY_ESCAPE && expandedMode != null) { expandedMode = null; return true; }
            return false;
        }
        if (key != Keyboard.KEY_ESCAPE) binding.setKey(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key);
        binding = null;
        return true;
    }

    public void handleScroll(int wheel, int x, int y) {
        if (RenderUtil.contains(x, y, positionX, positionY + HEADER, WIDTH, viewportHeight)) targetScroll -= Math.signum(wheel) * 28;
    }

    private static final class Row {
        final Module module;
        final Setting setting;
        final float y, height;
        final String choice;
        final int themeIndex;

        Row(Module module, Setting setting, float y, float height) {
            this(module, setting, y, height, null, -1);
        }

        private Row(Module module, Setting setting, float y, float height, String choice, int themeIndex) {
            this.module = module;
            this.setting = setting;
            this.y = y;
            this.height = height;
            this.choice = choice;
            this.themeIndex = themeIndex;
        }

        static Row choice(Module module, ModeSetting setting, String choice, float y) {
            return new Row(module, setting, y, 20, choice, -1);
        }

        static Row theme(int index, float y) {
            return new Row(null, null, y, 43, null, index);
        }
    }
}
