package Windy.util.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;


public final class SkinUtil {
    private static final int CACHE_LIMIT = 128;
    private static final Map<UUID, CachedSkin> CACHE = new LinkedHashMap<UUID, CachedSkin>(32, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, CachedSkin> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private static final Map<String, UUID> IDENTITIES = new LinkedHashMap<String, UUID>(32, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, UUID> eldest) {
            return size() > CACHE_LIMIT;
        }
    };
    private static final ThreadPoolExecutor LOOKUPS = new ThreadPoolExecutor(
            0, 2, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), task -> {
        Thread thread = new Thread(task, "Windy skin lookup");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardPolicy());

    private SkinUtil() { }

    public static void drawSkinHead(String username, int x, int y, int size) {
        Minecraft mc = Minecraft.getMinecraft();
        String uuid = username != null && username.equalsIgnoreCase(mc.getSession().getUsername())
                ? mc.getSession().getPlayerID() : null;
        drawSkinHead(username, uuid, x, y, size, 1f);
    }

    public static void drawSkinHead(String username, String uuidText, int x, int y, int size, float opacity) {
        Minecraft mc = Minecraft.getMinecraft();
        String identity = uuidText == null ? "offline:" + username : uuidText;
        UUID uuid = IDENTITIES.get(identity);
        if (uuid == null) {
            uuid = resolveUuid(username, uuidText);
            IDENTITIES.put(identity, uuid);
        }
        CachedSkin cached = CACHE.get(uuid);
        if (cached == null) {
            cached = new CachedSkin(DefaultPlayerSkin.getDefaultSkin(uuid));
            CACHE.put(uuid, cached);
            if (uuidText != null && uuid.version() != 3 && mc.getSessionService() != null && mc.getSkinManager() != null) {
                requestSkin(uuid, username, cached);
            }
        }

        mc.getTextureManager().bindTexture(cached.location);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1f, 1f, 1f, opacity);

        Gui.drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, size, size, 64, 64);
        Gui.drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, size, size, 64, 64);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableBlend();
    }

    private static void requestSkin(UUID uuid, String username, CachedSkin target) {
        Minecraft mc = Minecraft.getMinecraft();
        LOOKUPS.execute(() -> {
            try {
                GameProfile profile = mc.getSessionService().fillProfileProperties(new GameProfile(uuid, username), true);
                mc.addScheduledTask(() -> mc.getSkinManager().loadProfileTextures(profile,
                        (type, location, texture) -> {
                            if (type == MinecraftProfileTexture.Type.SKIN) {
                                target.location = location;
                            }
                        }, false));
            } catch (Exception ignored) {

            }
        });
    }

    private static UUID resolveUuid(String username, String uuidText) {
        if (uuidText != null) {
            String normalized = uuidText.replace("-", "");
            if (normalized.matches("[0-9a-fA-F]{32}")) {
                String dashed = normalized.substring(0, 8) + "-" + normalized.substring(8, 12) + "-"
                        + normalized.substring(12, 16) + "-" + normalized.substring(16, 20) + "-" + normalized.substring(20);
                return UUID.fromString(dashed);
            }
        }
        String name = username == null || username.isEmpty() ? "Steve" : username;
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static final class CachedSkin {
        private ResourceLocation location;

        private CachedSkin(ResourceLocation location) {
            this.location = location;
        }
    }
}
