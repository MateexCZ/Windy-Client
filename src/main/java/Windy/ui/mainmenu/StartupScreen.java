package Windy.ui.mainmenu;

import Windy.font.CustomFont;
import Windy.ui.modern.SoftBackdrop;
import Windy.util.render.RenderUtil;
import Windy.util.render.ClientLogo;
import net.minecraft.client.Minecraft;


public final class StartupScreen {
    public static final int LOGO_COLOR = 0xFFB3DFFF;
    public static final float MENU_ICON_WIDTH = 38;

    private StartupScreen() { }

    public static void drawLoading(int width, int height) {
        SoftBackdrop.draw(width, height);
        drawLogo(width / 2f, logoTop(width, height), logoScale(width), LOGO_COLOR);
        drawProgress(width, height, 0, 1);
    }

    public static float logoScale(int width) {
        float pixelsPerUnit = Minecraft.getMinecraft().displayWidth / (float) Math.max(1,width);
        float imageWidth = Math.min(144, Math.min(width - 64, ClientLogo.nativeWidth() / pixelsPerUnit));
        return Math.max(.25f, imageWidth / MENU_ICON_WIDTH);
    }

    public static float logoTop(int width, int height) {
        return height / 2f - ClientLogo.height(MENU_ICON_WIDTH * logoScale(width)) / 2f - 12;
    }

    public static float menuLogoX() {
        float groupWidth = MENU_ICON_WIDTH - CustomFont.comfortaa(48).getWidth("Windy");
        return 192 - groupWidth / 2;
    }

    public static float menuLogoTop() {
        return 43 + (CustomFont.comfortaa(48).getHeight("Windy") - ClientLogo.height(MENU_ICON_WIDTH)) / 2;
    }

    public static void drawLogo(float centerX, float top, float scale, int color) {
        float width = MENU_ICON_WIDTH * scale;
        ClientLogo.draw(centerX - width / 2, top, width, color, 1);
    }

    public static void drawProgress(int width, int height, float progress, float opacity) {
        float barWidth = Math.min(300, width - 64f);
        float left = (width - barWidth) / 2f;
        float top = height / 2f + 56;
        RenderUtil.drawRoundedRect(left, top, barWidth, 4, 2, tint(0x35445D, .7f * opacity));
        float fill = barWidth * Math.max(0, Math.min(1, progress));
        if (fill > .01f) {
            RenderUtil.drawRoundedRect(left - 2, top - 3, fill + 4, 10, 5,
                    tint(0x73CFFF, .08f * opacity));
            RenderUtil.drawRoundedRect(left, top, fill, 4, 2, tint(0x90D6FF, opacity));
        }
    }

    private static int tint(int rgb, float opacity) {
        return (Math.round(Math.max(0, Math.min(1, opacity)) * 255) << 24) | rgb;
    }


    public static final class Timeline {
        public static final long FILL = 1_650_000_000L;
        public static final long HOLD = 180_000_000L;
        public static final long MOVE = 1_100_000_000L;
        private final long startedAt;

        public Timeline(long startedAt) {
            this.startedAt = startedAt;
        }

        public float progress(long now) {
            return smooth((now - startedAt) / (double) FILL);
        }

        public float move(long now) {
            return smooth((now - startedAt - FILL - HOLD) / (double) MOVE);
        }

        public float menuOpacity(long now) {
            return smooth((now - startedAt - FILL - HOLD - 350_000_000L) / 750_000_000.0);
        }

        public float barOpacity(long now) {
            return 1 - smooth((now - startedAt - FILL - HOLD) / 420_000_000.0);
        }

        public boolean isFinished(long now) {
            return now - startedAt >= FILL + HOLD + MOVE;
        }

        private static float smooth(double value) {
            double t = Math.max(0, Math.min(1, value));
            return (float) (t * t * t * (t * (t * 6 - 15) + 10));
        }
    }
}
