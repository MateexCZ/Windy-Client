package Windy.ui.modern;

import java.util.List;


public final class HudSnap {
    public static final float DISTANCE = 6;
    private static final float MARGIN = 4;

    private HudSnap() { }

    public static final class Bounds {
        public final float x, y, width, height;

        public Bounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static final class Result {
        public final float x, y, verticalGuide, horizontalGuide;

        private Result(Axis horizontal, Axis vertical) {
            x = horizontal.position;
            y = vertical.position;
            verticalGuide = horizontal.guide;
            horizontalGuide = vertical.guide;
        }
    }

    public static Result align(float x, float y, float width, float height,
                               float screenWidth, float screenHeight, List<Bounds> others,
                               boolean snapping) {

        Axis horizontal = new Axis(x, width, Math.max(0, screenWidth - width));
        Axis vertical = new Axis(y, height, Math.max(0, screenHeight - height - 16));
        if (snapping) {
            horizontal.offer(MARGIN, 0);
            horizontal.offer(screenWidth / 2, .5f);
            horizontal.offer(screenWidth - MARGIN, 1);
            vertical.offer(MARGIN, 0);
            vertical.offer(screenHeight / 2, .5f);
            vertical.offer(screenHeight - 20, 1);
            for (Bounds other : others) {
                horizontal.alignWith(other.x, other.width);
                vertical.alignWith(other.y, other.height);
            }
        }
        return new Result(horizontal, vertical);
    }

    private static final class Axis {
        private final float raw, size, limit;
        private float position, guide = Float.NaN, best = DISTANCE + .001f;

        private Axis(float raw, float size, float limit) {
            this.raw = Math.max(0, Math.min(limit, raw));
            this.position = this.raw;
            this.size = size;
            this.limit = limit;
        }

        private void offer(float line, float fraction) {
            float candidate = line - size * fraction;
            float distance = Math.abs(candidate - raw);
            if (candidate >= 0 && candidate <= limit && distance < best) {
                position = candidate;
                guide = line;
                best = distance;
            }
        }

        private void alignWith(float start, float extent) {
            offer(start, 0);
            offer(start + extent / 2, .5f);
            offer(start + extent, 1);

            offer(start, 1);
            offer(start + extent, 0);
        }
    }
}
