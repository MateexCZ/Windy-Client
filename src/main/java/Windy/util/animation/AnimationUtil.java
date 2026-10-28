package Windy.util.animation;

import Windy.util.render.ColorUtil;

public class AnimationUtil {

    public enum Easing {
        EASE_OUT_CUBIC,
        EASE_OUT_QUART,
        EASE_OUT_EXPO,
        LINEAR
    }


    public static class Animation {
        private final long defaultDuration;
        private final Easing easing;
        private long duration;
        private float value;
        private float target;
        private long startedAt;

        public Animation(long duration, Easing easing, float initialValue) {
            this.defaultDuration = Math.max(0, duration);
            this.duration = this.defaultDuration;
            this.easing = easing;
            value = target = initialValue;
            startedAt = System.nanoTime();
        }

        public Animation(float initialValue) {
            this(260, Easing.EASE_OUT_EXPO, initialValue);
        }

        public void animateTo(float target) {
            animateTo(target, defaultDuration);
        }

        public void animateTo(float target, long durationMillis) {
            if (this.target == target) return;
            value = get();
            this.target = target;
            duration = Math.max(0, durationMillis);
            startedAt = System.nanoTime();
        }

        public void snap(float value) {
            this.value = this.target = value;
            startedAt = System.nanoTime();
            duration = 0;
        }

        public float get() {
            float progress = duration == 0 ? 1 : Math.max(0,
                    Math.min(1, (System.nanoTime() - startedAt) / (duration * 1_000_000f)));
            return value + (target - value) * ease(progress, easing);
        }
    }


    public static float ease(float progress, Easing easing) {
        float t = Math.max(0, Math.min(1, progress));
        float remaining = 1 - t;
        switch (easing) {
            case EASE_OUT_CUBIC: return 1 - remaining * remaining * remaining;
            case EASE_OUT_QUART: return 1 - remaining * remaining * remaining * remaining;
            case EASE_OUT_EXPO: return t >= 1 ? 1 : 1 - (float) Math.pow(2, -10 * t);
            default: return t;
        }
    }


    public static float approach(float current, float target, float deltaSeconds, float speed) {
        float delta = Math.max(0, Math.min(.1f, deltaSeconds));
        return current + (target - current) * (1 - (float) Math.exp(-speed * delta));
    }

    public static int multiplyAlpha(int color, float opacity) {
        int alpha = Math.round((color >>> 24) * Math.max(0, Math.min(1, opacity)));
        return (color & 0xFFFFFF) | (alpha << 24);
    }


    public static float smoothExp(float current, float target, float speed, float deltaSeconds) {
        return approach(current, target, deltaSeconds, speed);
    }

    public static int lerpColor(int first, int second, float ratio) {
        return Windy.util.render.ColorUtil.interpolateColor(first, second, ratio);
    }
}
