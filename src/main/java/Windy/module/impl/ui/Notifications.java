package Windy.module.impl.ui;

import Windy.font.CustomFont;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.BooleanSetting;
import Windy.setting.impl.NumberSetting;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import net.minecraft.client.gui.ScaledResolution;

@ModuleInfo(name = "Notifications", description = "Success, warning and error messages", category = Category.UI, enabled = true)
public final class Notifications extends Module {
    public enum Type {
        SUCCESS(0xFF76D9A4, "+"), WARNING(0xFFFFC56E, "!"), ERROR(0xFFFF7E91, "x");
        final int color;
        final String icon;
        Type(int color, String icon) { this.color = color; this.icon = icon; }
    }

    private static final Deque<Notice> pending = new ArrayDeque<>();
    private static Notifications instance;
    private final NumberSetting duration = new NumberSetting("Duration", 4, 1, 10, .5);
    private final NumberSetting count = new NumberSetting("Maximum Visible", 4, 1, 6, 1);
    private final BooleanSetting moduleChanges = new BooleanSetting("Module Toggles", true);

    public Notifications() {
        instance = this;
        addSettings(duration, count, moduleChanges);
    }

    public static void success(String title, String message) { post(Type.SUCCESS, title, message); }
    public static void warning(String title, String message) { post(Type.WARNING, title, message); }
    public static void error(String title, String message) { post(Type.ERROR, title, message); }
    public static void fail(String title, String message) { error(title, message); }

    public static void moduleChanged(String name, boolean enabled) {
        if (instance != null && instance.moduleChanges.isState()) {
            post(enabled ? Type.SUCCESS : Type.WARNING, name, enabled ? "Enabled" : "Disabled");
        }
    }

    public static synchronized void post(Type type, String title, String message) {
        if (instance == null || !instance.isToggled()) return;
        if (pending.size() >= 12) pending.removeFirst();
        pending.addLast(new Notice(type, title == null ? "" : title, message == null ? "" : message,
                System.nanoTime(), (long) (instance.duration.getVal() * 1_000_000_000L)));
    }

    @Override public void on2D(ScaledResolution sr) {
        synchronized (Notifications.class) {
            long now = System.nanoTime();
            pending.removeIf(notice -> now - notice.created > notice.duration);
            float y = sr.getScaledHeight() - 56;
            int visible = 0;
            Iterator<Notice> iterator = pending.descendingIterator();
            while (iterator.hasNext() && visible++ < count.getVal()) {
                Notice notice = iterator.next();
                float age = (now - notice.created) / 1_000_000_000f;
                float remaining = (notice.duration - now + notice.created) / 1_000_000_000f;
                float opacity = Math.min(1, Math.min(age / .22f, remaining / .25f));
                opacity = 1 - (float) Math.pow(1 - Math.max(0, opacity), 3);
                float width = 150;
                float x = sr.getScaledWidth() - width - 10 + (1 - opacity) * 30;
                RenderUtil.drawPanel(x, y, width, 44, opacity);
                RenderUtil.drawRoundedRect(x + 10, y + 11, 20, 20, 6, AnimationUtil.multiplyAlpha(notice.type.color, opacity * .2f));
                CustomFont.drawCenteredString(CustomFont.rounded(18), notice.type.icon, x + 20, y + 17, notice.type.color, opacity);
                CustomFont.drawString(CustomFont.rounded(18),
                        CustomFont.trimToWidth(CustomFont.rounded(18), notice.title, 183),
                        x + 39,
                        y + 10,
                        VisualTheme.TEXT,
                        opacity);
                CustomFont.drawString(CustomFont.rounded(16),
                        CustomFont.trimToWidth(CustomFont.rounded(16), notice.message, 183),
                        x + 39,
                        y + 25,
                        VisualTheme.MUTED,
                        opacity);
                float progress = Math.max(0, 1 - (float) (now - notice.created) / notice.duration);
                RenderUtil.drawRoundedRect(x + 9, y + 41, 142 * progress, 1, 0, AnimationUtil.multiplyAlpha(notice.type.color, opacity));
                y -= 50;
            }
        }
    }

    @Override public void onDisable() {
        synchronized (Notifications.class) { pending.clear(); }
    }

    private static final class Notice {
        final Type type;
        final String title, message;
        final long created, duration;
        Notice(Type type, String title, String message, long created, long duration) {
            this.type = type; this.title = title; this.message = message;
            this.created = created; this.duration = duration;
        }
    }
}
