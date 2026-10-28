package Windy.font.fontmanager;

import java.awt.Font;
import Windy.util.render.TextureSampling;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.renderer.GlStateManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;


public final class FontManager {
    private static final Logger LOGGER = LogManager.getLogger(FontManager.class);
    private final Map<String, TTFFontRenderer> fonts = new HashMap<>();
    private final Font light = load("Windy_font.otf");
    private final Font comfortaa = load("Windy_comforta.ttf");
    private Font rounded;

    public TTFFontRenderer getFont(String key) {
        String family = "Light";
        if (key != null && key.startsWith("Comf ")) family = "Comf";
        else if (key != null && key.startsWith("Rounded ")) family = "Rounded";
        int size = 18;
        if (key != null && (key.startsWith("Light ") || key.startsWith("Comf ") || key.startsWith("Rounded "))) {
            try { size = Integer.parseInt(key.substring(key.indexOf(' ') + 1)); }
            catch (NumberFormatException ignored) { }
        }
        size = Math.max(8, Math.min(128, size));
        String cacheKey = family + " " + size;
        TTFFontRenderer cached = fonts.get(cacheKey);
        if (cached == null) {
            Font base = family.equals("Comf") ? comfortaa : light;
            if (family.equals("Rounded")) {
                if (rounded == null) rounded = load("SF-Pro-Rounded-Regular.otf");
                base = rounded;
            }
            cached = create(base.deriveFont((float) size));
            fonts.put(cacheKey, cached);
        }
        return cached;
    }

    private Font load(String name) {
        String path = "/assets/minecraft/windy/Fonts/" + name;
        try (InputStream stream = FontManager.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing font resource: " + name);
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (Exception error) {

            LOGGER.warn("Cannot load Windy font {}; using SansSerif", name, error);
            return new Font("SansSerif", Font.PLAIN, 18);
        }
    }

    private TTFFontRenderer create(Font font) {
        ConcurrentLinkedQueue<TextureData> uploads = new ConcurrentLinkedQueue<>();
        ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "Windy font rasterizer");
            thread.setDaemon(true);
            return thread;
        });
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        TTFFontRenderer renderer = new TTFFontRenderer(worker, uploads, font);
        worker.shutdown();
        boolean interrupted = false;
        try {

            while (!worker.isTerminated() || !uploads.isEmpty()) {
                TextureData texture;
                while ((texture = uploads.poll()) != null) upload(texture);
                if (!worker.isTerminated()) {
                    try { worker.awaitTermination(1, TimeUnit.MILLISECONDS); }
                    catch (InterruptedException error) { interrupted = true; }
                }
            }
        } finally {
            GlStateManager.bindTexture(previousTexture);
            if (interrupted) Thread.currentThread().interrupt();
        }
        return renderer;
    }

    private void upload(TextureData texture) {
        GlStateManager.bindTexture(texture.getTextureId());
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, texture.getWidth(),
                texture.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, texture.getBuffer());
        TextureSampling.finishUpload();
    }
}
