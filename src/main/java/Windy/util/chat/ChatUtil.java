package Windy.util.chat;

import Windy.module.impl.ui.Notifications;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;


public final class ChatUtil {
    private ChatUtil() { }

    public static void info(String message) { local("\u00a7b", message); }
    public static void warning(String message) { local("\u00a7e", message); }
    public static void error(String message) { local("\u00a7c", message); }
    public static void urgent(String message) {
        local("\u00a7c\u00a7l", message);
        Notifications.warning("Windy", message);
    }

    private static void local(String color, String message) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (mc.thePlayer != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText("\u00a78[\u00a7bWindy\u00a78] "
                        + color + String.valueOf(message) + "\u00a7r"));
            }
        });
    }
}
