package Windy.util.render;

import Windy.module.impl.ui.Visuals;
import Windy.util.animation.AnimationUtil;


public final class VisualTheme {
    public static final int TEXT = 0xFFE7EAF0;
    public static final int MUTED = 0xFF9199A8;
    public static final int DEFAULT_ACCENT = 0xFF99B8F5;
    private static final long START = System.nanoTime();
    private static final long TRANSITION = 280_000_000L;
    private static int fromPrimary, fromSecondary, toPrimary, toSecondary;
    private static long changedAt;
    private static boolean initialized;

    private VisualTheme() { }

    public static int accent() { return color(0); }

    public static int secondary() { return color(.5f); }


    public static int color(float phase) {
        long now = System.nanoTime();
        updatePalette(now);
        float transition = Math.min(1, (now - changedAt) / (float) TRANSITION);
        transition = transition * transition * (3 - 2 * transition);
        int primary = mix(fromPrimary, toPrimary, transition);
        int secondary = mix(fromSecondary, toSecondary, transition);
        return Visuals.themeFade.isState()
                ? blendAt(primary, secondary, now - START, Visuals.fadePeriod.getVal(), phase)
                : mix(primary, secondary, Math.max(0, Math.min(1, phase * 2)));
    }

    private static void updatePalette(long now) {
        int primary = Visuals.accent.getColor();
        int secondary = Visuals.secondary.getColor();
        if (!initialized) {
            fromPrimary = toPrimary = primary;
            fromSecondary = toSecondary = secondary;
            changedAt = now - TRANSITION;
            initialized = true;
        } else if (primary != toPrimary || secondary != toSecondary) {
            float t = Math.min(1, (now - changedAt) / (float) TRANSITION);
            t = t * t * (3 - 2 * t);
            fromPrimary = mix(fromPrimary, toPrimary, t);
            fromSecondary = mix(fromSecondary, toSecondary, t);
            toPrimary = primary;
            toSecondary = secondary;
            changedAt = now;
        }
    }


    public static int preview(int primary, int secondary, float phase) {
        return Visuals.themeFade.isState()
                ? blendAt(primary, secondary, System.nanoTime() - START, Visuals.fadePeriod.getVal(), phase)
                : mix(primary, secondary, Math.max(0, Math.min(1, phase * 2)));
    }


    public static int blendAt(int primary, int secondary, long elapsed, double seconds, float phase) {
        double period = Math.max(1, seconds) * 1_000_000_000.0;
        double cycle = (elapsed % (long) period) / period + phase;
        float amount = (float) (.5 - .5 * Math.cos(cycle * Math.PI * 2));
        return mix(primary, secondary, amount);
    }

    public static int mix(int first, int second, float amount) {
        return ColorUtil.interpolateColor(first, second, amount);
    }

    public static int withOpacity(int color, float opacity) {
        return AnimationUtil.multiplyAlpha(color, opacity);
    }
}
