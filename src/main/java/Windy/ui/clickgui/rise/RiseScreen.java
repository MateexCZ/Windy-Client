

package Windy.ui.clickgui.rise;

import Windy.Windy;
import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.impl.ui.Visuals;
import Windy.setting.Setting;
import Windy.setting.impl.*;
import Windy.setting.impl.ColorSetting;
import Windy.ui.modern.ColorPopup;
import Windy.ui.modern.ThemePalette;
import Windy.util.animation.AnimationUtil.Animation;
import Windy.util.animation.AnimationUtil;
import Windy.util.blur.GaussianBlur;
import Windy.util.render.ColorUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;





public final class RiseScreen extends GuiScreen {
    private static final int BACKGROUND = 0xFE171A21;
    private static final int SECONDARY = 0xFF121419;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8D909A;

    private static final String[] THEMES = {
            "Blend", "Water", "Bubblegum", "Pastel", "Aqua", "Magic"
    };
    private static final int[][] COLORS = {
            {0xFF4794FD, 0xFF47FDA0},
            {0xFF0CE8C7, 0xFF0CA3E8},
            {0xFFF391D8, 0xFF98A5F3},
            {0xFFF39BB2, 0xFFCFC4F3},
            {0xFFB9FAFF, 0xFF4FC7C8},
            {0xFF4A00E0, 0xFF8E2DE2}
    };


    private static final int WINDOW_WIDTH = 460;
    private static final int WINDOW_HEIGHT = 380;
    private static final int SIDEBAR_WIDTH = 112;
    private static final int CONTENT_TOP = 46;
    private static final int CONTENT_BOTTOM = WINDOW_HEIGHT - 19;
    private static int theme;
    private static Category selected = Category.Combat;
    private static boolean backgroundBlur = true;
    private final List<Card> cards = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    private final Map<Setting, Animation> values = new IdentityHashMap<>();
    private Animation opening = new Animation(0);
    private Animation categoryFade = new Animation(0);
    private Animation selection = new Animation(68);
    private float baseScale;
    private float scale;
    private float ox;
    private float oy;
    private float posX;
    private float posY;
    private float dragX;
    private float dragY;
    private float scroll;
    private float targetScroll;
    private float maxScroll;
    private float dt;
    private boolean positioned;
    private boolean dragging;
    private boolean closing;
    private boolean searching;
    private boolean themes;
    private String query = "";
    private KeybindSetting binding;
    private NumberSetting sliding;
    private float sliderX;
    private float sliderW;
    private long lastFrame;

    @Override
    public void initGui() {
        cards.clear();
        List<Module> modules = new ArrayList<>(Windy.INSTANCE.getModuleManager().getModules().values());
        modules.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        for (Module m : modules) cards.add(new Card(m));
        opening = new Animation(0);
        opening.animateTo(1, 300);
        categoryFade = new Animation(0);
        categoryFade.animateTo(1, 300);
        closing = false;
        lastFrame = System.nanoTime();
        Keyboard.enableRepeatEvents(true);
        layout();
    }

    private void layout() {
        baseScale = Math.max(0.1F, Math.min(1.15F, Math.min((width - 18.0F) / WINDOW_WIDTH, (height - 18.0F) / WINDOW_HEIGHT)));
        if (!positioned) {
            posX = (width - WINDOW_WIDTH * baseScale) / 2;
            posY = (height - WINDOW_HEIGHT * baseScale) / 2;
            positioned = true;
        }
        posX = Math.max(0, Math.min(width - WINDOW_WIDTH * baseScale, posX));
        posY = Math.max(0, Math.min(height - WINDOW_HEIGHT * baseScale, posY));
        float zoom = 0.88F + 0.12F * opening.get();
        scale = baseScale * zoom;
        ox = posX + (WINDOW_WIDTH * baseScale - WINDOW_WIDTH * scale) / 2;
        oy = posY + (WINDOW_HEIGHT * baseScale - WINDOW_HEIGHT * scale) / 2;
    }

    private int accent(float y) {
        return VisualTheme.color(y / 500f);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long now = System.nanoTime();
        dt = Math.min(0.05F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        if (closing && opening.get() < 0.001F) {
            mc.displayGuiScreen(null);
            return;
        }
        if (dragging) {
            posX = mouseX - dragX;
            posY = mouseY - dragY;
        }
        layout();
        float a = opening.get();
        float mx = (mouseX - ox) / scale;
        float my = (mouseY - oy) / scale;
        if (sliding != null) updateSlider(mx);
        if (backgroundBlur && mc.theWorld != null && net.minecraft.client.renderer.OpenGlHelper.isFramebufferEnabled()) {
            GaussianBlur.startBlur();
            RenderUtil.drawRoundedRect(ox, oy, WINDOW_WIDTH * scale, WINDOW_HEIGHT * scale, 10, 0xFFFFFFFF);
            GaussianBlur.endBlur(Math.max(1, 12 * a), 2);
        }
        RenderUtil.drawRect(0, 0, width, height, AnimationUtil.multiplyAlpha(0x50000000, a));
        GL11.glPushMatrix();
        GL11.glTranslatef(ox, oy, 0);
        GL11.glScalef(scale, scale, 1);
        actions.clear();
        for (int i = 12; i > 0; i--) RenderUtil.drawRoundedRect(-i, -i, WINDOW_WIDTH + 2 * i, WINDOW_HEIGHT + 2 * i, 10 + i, AnimationUtil.multiplyAlpha(0x08000000, a));
        RenderUtil.drawRoundedRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, 10, AnimationUtil.multiplyAlpha(BACKGROUND, a));
        RenderUtil.drawRoundedRect(0, 0, SIDEBAR_WIDTH, WINDOW_HEIGHT, 10, AnimationUtil.multiplyAlpha(SECONDARY, a));
        RenderUtil.scissor(1, 1, SIDEBAR_WIDTH - 1, WINDOW_HEIGHT - 2, scale, ox, oy);
        for (int i = 7; i > 0; i--) RenderUtil.drawCircle(SIDEBAR_WIDTH - 12, WINDOW_HEIGHT * 0.53F, i * 24, AnimationUtil.multiplyAlpha((VisualTheme.accent() & 16777215) | 0x03000000, a));
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        CustomFont.drawString(CustomFont.rounded(48), "Windy", 16, 14, accent(0), a);
        int active = themes ? Category.values().length + 1 : selected == null ? Category.values().length : selected.ordinal();
        selection.animateTo(57 + active * 28, 300);
        RenderUtil.drawRoundedRect(10,
                selection.get(),
                SIDEBAR_WIDTH - 20,
                24,
                5,
                AnimationUtil.multiplyAlpha(ColorUtil.interpolateColor(SECONDARY, accent(60), 0.15F), a));
        int i = 0;
        for (Category c : Category.values()) {
            final Category pick = c;
            nav(c.name(), 57 + i++ * 28, selected == c && !themes, a, ()->switchPage(pick, false));
        }
        nav("Search", 57 + Category.values().length * 28, selected == null && !themes, a, ()->{
            switchPage(null, false);
            searching = true;
        });
        nav("Themes", 57 + (Category.values().length + 1) * 28, themes, a, ()->switchPage(null, true));
        CustomFont.drawString(CustomFont.rounded(16), "BACKGROUND BLUR", 15, WINDOW_HEIGHT - 77, MUTED, a);
        CustomFont.drawString(CustomFont.rounded(18),
                backgroundBlur ? "Enabled" : "Disabled",
                15,
                WINDOW_HEIGHT - 60,
                backgroundBlur ? accent(WINDOW_HEIGHT) : MUTED,
                a);
        action(10, WINDOW_HEIGHT - 80, SIDEBAR_WIDTH - 20, 38, (b)->{
            if (b == 0) backgroundBlur = !backgroundBlur;
        });
        CustomFont.drawString(CustomFont.rounded(16),
                CustomFont.trimToWidth(CustomFont.rounded(16), mc.getSession().getUsername(), SIDEBAR_WIDTH - 25),
                14,
                WINDOW_HEIGHT - 32,
                TEXT,
                a * 0.75F);
        CustomFont.drawString(CustomFont.rounded(16), "Windy / rise ahh menu", 14, WINDOW_HEIGHT - 18, MUTED, a * 0.65F);
        CustomFont.drawString(CustomFont.rounded(20),
                themes ? "Themes" : selected == null ? "Search" : selected.name(),
                SIDEBAR_WIDTH + 12,
                16,
                TEXT,
                a);
        if (!themes) {
            RenderUtil.drawRoundedRect(SIDEBAR_WIDTH + 108,
                    10,
                    WINDOW_WIDTH - SIDEBAR_WIDTH - 142,
                    24,
                    5,
                    AnimationUtil.multiplyAlpha(0xFF121419, a));
            String s = query.isEmpty() && !searching ? "Type to search..." : query + (searching && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), s, WINDOW_WIDTH - SIDEBAR_WIDTH - 160),
                    SIDEBAR_WIDTH + 116,
                    18,
                    query.isEmpty() ? MUTED : TEXT,
                    a);
            action(SIDEBAR_WIDTH + 108, 10, WINDOW_WIDTH - SIDEBAR_WIDTH - 142, 24, (b)->{
                if (b == 0) {
                    searching = true;
                    binding = null;
                }
            });
        }
        CustomFont.drawString(CustomFont.rounded(18), "x", WINDOW_WIDTH - 22, 18, MUTED, a);
        action(WINDOW_WIDTH - 29, 9, 22, 26, (b)->{
            if (b == 0) close();
        });
        scroll += (targetScroll - scroll) * (1 - (float)Math.exp(-17 * dt));
        RenderUtil.scissor(SIDEBAR_WIDTH + 5, CONTENT_TOP, WINDOW_WIDTH - SIDEBAR_WIDTH - 10, CONTENT_BOTTOM - CONTENT_TOP, scale, ox, oy);
        if (themes) drawThemes(a * categoryFade.get()); else drawModules(mx, my, a * categoryFade.get());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (!themes && maxScroll > 0) {
            float track = CONTENT_BOTTOM - CONTENT_TOP;
            float thumb = Math.max(22, track * track / (track + maxScroll));
            RenderUtil.drawRoundedRect(WINDOW_WIDTH - 5,
                    CONTENT_TOP + (track - thumb) * scroll / maxScroll,
                    2,
                    thumb,
                    1,
                    AnimationUtil.multiplyAlpha(0xFF656B7B, a * 0.6F));
        }
        CustomFont.drawString(CustomFont.rounded(16),
                binding != null ? "Press key / Esc cancels / Delete clears" : "LMB: toggle  /  RMB: settings  /  MMB: keybind",
                SIDEBAR_WIDTH + 12,
                WINDOW_HEIGHT - 12,
                MUTED,
                a * 0.65F);
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private void nav(String label, int y, boolean active, float a, Runnable run) {
        CustomFont.drawString(CustomFont.rounded(18), label, 22, y + 8, active ? accent(y) : MUTED, a);
        action(10, y, SIDEBAR_WIDTH - 20, 24, (b)->{
            if (b == 0) run.run();
        });
    }

    private void switchPage(Category c, boolean isThemes) {
        selected = c;
        themes = isThemes;
        scroll = targetScroll = 0;
        binding = null;
        sliding = null;
        categoryFade = new Animation(0);
        categoryFade.animateTo(1, 260);
    }

    private void drawThemes(float a) {
        float cw = (WINDOW_WIDTH - SIDEBAR_WIDTH - 34) / 2.0F;
        for (int i = 0; i < THEMES.length; i++) {
            final int index = i;
            float x = SIDEBAR_WIDTH + 12 + (i % 2) * (cw + 10);
            float y = CONTENT_TOP + (i / 2) * 83;
            RenderUtil.drawRoundedRect(x, y, cw, 72, 6, AnimationUtil.multiplyAlpha(0xFF11141A, a));
            RenderUtil.drawHorizontalGradient(Math.round(x + 8), Math.round(y + 9),
                    Math.round(cw - 16), 23, AnimationUtil.multiplyAlpha(VisualTheme.preview(COLORS[i][0], COLORS[i][1], 0),
                            a), AnimationUtil.multiplyAlpha(VisualTheme.preview(COLORS[i][0], COLORS[i][1], .5f),
                            a));
            CustomFont.drawString(CustomFont.rounded(20),
                    THEMES[i],
                    x + 9,
                    y + 44,
                    (Visuals.accent.getColor() == COLORS[i][0] && Visuals.secondary.getColor() == COLORS[i][1]) ? COLORS[i][0] : TEXT,
                    a);
            if ((Visuals.accent.getColor() == COLORS[i][0] && Visuals.secondary.getColor() == COLORS[i][1])) CustomFont.drawString(CustomFont.rounded(16), "Selected", x + cw - 49, y + 47, MUTED, a);
            contentAction(x, y, cw, 72, (b)->{
                if (b == 0) { theme = index; ThemePalette.apply(COLORS[index][0], COLORS[index][1]); }
            }, CONTENT_TOP, CONTENT_BOTTOM);
        }
    }

    private void drawModules(float mx, float my, float a) {
        float y = CONTENT_TOP - scroll;
        int count = 0;
        for (Card c : cards) {
            Module m = c.module;
            if (selected != null && selected != m.getCategory()) continue;
            if (!query.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) continue;
            float wanted = 38;
            if (c.expanded) for (Setting s : m.getSettingList()) if (s.isVisible()) wanted += rowHeight(s);
            c.height.animateTo(wanted, Math.min(450, (int)wanted * 3));
            float ch = c.height.get();
            if (y + ch >= CONTENT_TOP && y < CONTENT_BOTTOM) drawCard(c, SIDEBAR_WIDTH + 12, y, WINDOW_WIDTH - SIDEBAR_WIDTH - 24, ch, mx, my, a);
            y += ch + 7;
            count++;
        }
        maxScroll = Math.max(0, y + scroll - CONTENT_BOTTOM);
        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        if (count == 0) CustomFont.drawString(CustomFont.rounded(18), "No modules found", SIDEBAR_WIDTH + 20, CONTENT_TOP + 15, MUTED, a);
    }

    private void drawCard(Card c, float x, float y, float w, float h, float mx, float my, float a) {
        Module m = c.module;
        c.hover.animateTo(RenderUtil.contains(mx, my, x, y, w, 38) ? 1 : 0, 100);
        c.enabled.animateTo(m.isToggled() ? 1 : 0, 220);
        RenderUtil.drawRoundedRect(x,
                y,
                w,
                h,
                6,
                AnimationUtil.multiplyAlpha(ColorUtil.interpolateColor(0xFF12151B, 0xFF0E1117, c.hover.get()), a));
        CustomFont.drawString(CustomFont.rounded(20),
                CustomFont.trimToWidth(CustomFont.rounded(20), m.getName(), w - 48),
                x + 8,
                y + 8,
                ColorUtil.interpolateColor(0xFFC8C9CE, accent(y), c.enabled.get()),
                a);
        CustomFont.drawString(CustomFont.rounded(16),
                CustomFont.trimToWidth(CustomFont.rounded(16), m.getDescription(), w - 20),
                x + 8,
                y + 25,
                0xFF737783,
                a);
        CustomFont.drawString(CustomFont.rounded(18), c.expanded ? "-" : "+", x + w - 17, y + 11, MUTED, a);
        contentAction(x, y, w, 38, (b)->{
            if (b == 0) m.toggle();
            if (b == 1) {
                c.expanded = !c.expanded;
                sliding = null;
                binding = null;
            }
            if (b == 2) {
                binding = m.getKeybind();
                searching = false;
            }
        }, CONTENT_TOP, CONTENT_BOTTOM);
        if (h <= 38.5F) return;
        float top = Math.max(CONTENT_TOP, y + 38);
        float bottom = Math.min(CONTENT_BOTTOM, y + h - 1);
        RenderUtil.scissor(x, top, w, Math.max(0, bottom - top), scale, ox, oy);
        float sy = y + 43;
        for (Setting s : m.getSettingList()) if (s.isVisible()) {
            drawSetting(s,
                    x + 9,
                    sy,
                    w - 18,
                    a * Math.min(1, (h - 38) / 25.0F),
                    c.expanded && Math.abs(c.height.get() - targetHeight(c)) < 1,
                    top,
                    bottom,
                    mx);
            sy += rowHeight(s);
        }
        RenderUtil.scissor(SIDEBAR_WIDTH + 5, CONTENT_TOP, WINDOW_WIDTH - SIDEBAR_WIDTH - 10, CONTENT_BOTTOM - CONTENT_TOP, scale, ox, oy);
    }

    private float targetHeight(Card c) {
        float h = 38;
        for (Setting s : c.module.getSettingList()) if (s.isVisible()) h += rowHeight(s);
        return h;
    }

    private float rowHeight(Setting s) {
        return s instanceof NumberSetting ? 30 : 24;
    }

    private void drawSetting(Setting s,
            float x,
            float y,
            float w,
            float a,
            boolean interactive,
            float clipTop,
            float clipBottom,
            float mx) {
        if (s instanceof ColorSetting) {
            ColorSetting color = (ColorSetting) s;
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), s.getName(), w - 36),
                    x,
                    y + 1,
                    TEXT,
                    a);
            RenderUtil.drawRoundedRect(x + w - 24, y, 24, 12, 4, AnimationUtil.multiplyAlpha(color.getColor(), a));
            if (interactive) contentAction(x, y - 2, w, 21, button -> {
                if (button == 0) ColorPopup.open(color);
            }, clipTop, clipBottom);
        } else if (s instanceof BooleanSetting) {
            BooleanSetting b = (BooleanSetting)s;
            Animation t = values.computeIfAbsent(s, (k)->new Animation(b.isState() ? 1 : 0));
            t.animateTo(b.isState() ? 1 : 0, 180);
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), s.getName(), w - 24),
                    x,
                    y + 1,
                    0xFFDCDDE2,
                    a);
            RenderUtil.drawCircle(x + w - 8, y + 5, 4, AnimationUtil.multiplyAlpha(0xFF303643, a));
            if (t.get() > 0.01) RenderUtil.drawCircle(x + w - 8, y + 5, 4 * t.get(), AnimationUtil.multiplyAlpha(accent(y), a));
            if (interactive) contentAction(x, y - 2, w, 21, (k)->{
                if (k == 0) b.toggle();
            }, clipTop, clipBottom);
        } else if (s instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting)s;
            float valueW = Math.min(w * 0.55F, CustomFont.rounded(16).getWidth(mode.getCurrMode()));
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), s.getName() + ":", w - valueW - 12),
                    x,
                    y + 1,
                    0xFFDCDDE2,
                    a);
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), mode.getCurrMode(), w * 0.55F),
                    x + w - valueW,
                    y + 1,
                    accent(y),
                    a);
            if (interactive) contentAction(x, y - 2, w, 21, (k)->{
                if (k != 0 && k != 1) return;
                int n = mode.getModes().size();
                int i = (mode.getModes().indexOf(mode.getCurrMode()) + (k == 0 ? 1 : 0xFFFFFFFF) + n) % n;
                mode.setModeIndex(i);
                mode.setCurrMode(mode.getModes().get(i));
            }, clipTop, clipBottom);
        } else if (s instanceof NumberSetting) {
            NumberSetting n = (NumberSetting)s;
            String v = BigDecimal.valueOf(n.getVal()).setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            CustomFont.drawString(CustomFont.rounded(16),
                    CustomFont.trimToWidth(CustomFont.rounded(16), n.getName(), w - 55),
                    x,
                    y,
                    0xFFDCDDE2,
                    a);
            CustomFont.drawString(CustomFont.rounded(16), v, x + w - CustomFont.rounded(16).getWidth(v), y, MUTED, a);
            double low = Math.min(n.getMinVal(), n.getMaxVal());
            double high = Math.max(n.getMinVal(), n.getMaxVal());
            float f = high == low ? 0 : (float)Math.max(0, Math.min(1, (n.getVal() - low) / (high - low)));
            RenderUtil.drawRoundedRect(x, y + 16, w, 2, 1, AnimationUtil.multiplyAlpha(0xFF323846, a));
            RenderUtil.drawRoundedRect(x, y + 16, Math.max(1, w * f), 2, 1, AnimationUtil.multiplyAlpha(accent(y), a));
            RenderUtil.drawCircle(x + w * f, y + 17, 2.7F, AnimationUtil.multiplyAlpha(accent(y), a));
            if (interactive) contentAction(x - 2, y + 10, w + 4, 15, (k)->{
                if (k == 0) {
                    sliding = n;
                    sliderX = x;
                    sliderW = w;
                    updateSlider(mx);
                }
            }, clipTop, clipBottom);
        } else if (s instanceof KeybindSetting) {
            KeybindSetting k = (KeybindSetting)s;
            String key = binding == k ? "Press a key..." : k.getCode() == 0 ? "None" : Keyboard.getKeyName(k.getCode());
            CustomFont.drawString(CustomFont.rounded(16), "Keybind:", x, y + 1, MUTED, a);
            CustomFont.drawString(CustomFont.rounded(16), key == null ? "None" : key, x + 55, y + 1, binding == k ? accent(y) : TEXT, a);
            if (interactive) contentAction(x, y - 2, w, 21, (b)->{
                if (b == 0) {
                    binding = k;
                    searching = false;
                }
            }, clipTop, clipBottom);
        }
    }

    private void updateSlider(float mx) {
        double lo = Math.min(sliding.getMinVal(), sliding.getMaxVal());
        double hi = Math.max(sliding.getMinVal(), sliding.getMaxVal());
        double raw = lo + Math.max(0, Math.min(1, (mx - sliderX) / sliderW)) * (hi - lo);
        double step = Math.abs(sliding.getIncrement());
        double value = Math.max(lo, Math.min(hi, step > 0 ? lo + Math.round((raw - lo) / step) * step : raw));
        double omin = sliding.getMinVal();
        double omax = sliding.getMaxVal();
        try {
            sliding.setMinVal(lo);
            sliding.setMaxVal(hi);
            sliding.setVal(value);
        } finally {
            sliding.setMinVal(omin);
            sliding.setMaxVal(omax);
        }
    }

    private void close() {
        closing = true;
        opening.animateTo(0, 140);
        sliding = null;
        dragging = false;
    }

    private void action(float x, float y, float w, float h, Click click) {
        actions.add(new Action(x, y, w, h, click));
    }

    private void contentAction(float x, float y, float w, float h, Click click, float top, float bottom) {
        float yy = Math.max(top, y);
        float end = Math.min(bottom, y + h);
        if (end > yy) action(x, yy, w, end - yy, click);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int b) throws IOException {
        if (closing) return;
        float mx = (mouseX - ox) / scale;
        float my = (mouseY - oy) / scale;
        searching = false;
        binding = null;
        for (int i = actions.size() - 1; i >= 0; i--) {
            Action a = actions.get(i);
            if (RenderUtil.contains(mx, my, a.x, a.y, a.w, a.h)) {
                a.click.run(b);
                return;
            }
        }
        if (b == 0 && RenderUtil.contains(mx, my, 0, 0, WINDOW_WIDTH, 42)) {
            dragging = true;
            dragX = mouseX - posX;
            dragY = mouseY - posY;
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        sliding = null;
        dragging = false;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (binding != null) {
            if (key != Keyboard.KEY_ESCAPE) binding.setCode(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? 0 : key);
            binding = null;
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            if (searching) searching = false; else close();
            return;
        }
        if (key == Keyboard.KEY_RSHIFT && !searching) {
            close();
            return;
        }
        if (isCtrlKeyDown() && key == Keyboard.KEY_F) {
            switchPage(null, false);
            searching = true;
            return;
        }
        if (searching) {
            if (key == Keyboard.KEY_BACK && !query.isEmpty()) query = query.substring(0, query.length() - 1); else if (ChatAllowedCharacters.isAllowedCharacter(c) && query.length() < 48) query += c;
            scroll = targetScroll = 0;
            sliding = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (ColorPopup.isOpen()) { super.handleMouseInput(); return; }
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || closing || sliding != null || themes) return;
        float mx = (Mouse.getEventX() * width / (float)mc.displayWidth - ox) / scale;
        float my = (height - Mouse.getEventY() * height / (float)mc.displayHeight - 1 - oy) / scale;
        if (RenderUtil.contains(mx, my, SIDEBAR_WIDTH, CONTENT_TOP, WINDOW_WIDTH - SIDEBAR_WIDTH, CONTENT_BOTTOM - CONTENT_TOP)) targetScroll = Math.max(0, Math.min(maxScroll, targetScroll + (wheel > 0 ? -35 : 35)));
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        sliding = null;
        binding = null;
        dragging = false;
        Windy.INSTANCE.getConfigManager().saveConfig();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class Card {
        final Module module;
        boolean expanded;
        final Animation height = new Animation(38);
        final Animation hover = new Animation(0);
        final Animation enabled;

        Card(Module m) {
            module = m;
            enabled = new Animation(m.isToggled() ? 1 : 0);
        }
    }

    private interface Click {

        void run(int b);
    }

    private static final class Action {
        final float x;
        final float y;
        final float w;
        final float h;
        final Click click;

        Action(float x, float y, float w, float h, Click c) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            click = c;
        }
    }
}
