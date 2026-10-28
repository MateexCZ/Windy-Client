package Windy.ui.auth;

import Windy.font.CustomFont;
import Windy.ui.modern.ModernScreen;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

public final class GuiLogin extends ModernScreen {
    private String username = "";
    private String status = "Choose a name for an Cracked account.";

    public GuiLogin() {
        super(360, 230);
    }

    @Override
    protected void renderMenu(float mouseX, float mouseY, float opacity) {
        RenderUtil.drawPanel(0, 0, 360, 230, opacity);
        CustomFont.drawString(CustomFont.light(28), "Cracked account", 24, 24, VisualTheme.TEXT, opacity);
        CustomFont.drawString(CustomFont.light(16), "Username", 24, 68, VisualTheme.MUTED, opacity);
        RenderUtil.drawRoundedRect(24, 88, 312, 31, 6, AnimationUtil.multiplyAlpha(0xFF253143, opacity));
        String cursor = (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "";
        CustomFont.drawString(CustomFont.light(20), username + cursor, 34, 99, VisualTheme.TEXT, opacity);
        CustomFont.drawString(CustomFont.light(16),
                CustomFont.trimToWidth(CustomFont.light(16), status, 310),
                24,
                135,
                VisualTheme.MUTED,
                opacity);
        button("cancel", "Back", 24, 177, 151, 30, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new AltManagerGui())));
        button("add", "Add account", 185, 177, 151, 30, mouseX, mouseY,
                validName(), true, this::addProfile);
    }

    private boolean validName() {
        return username.matches("[A-Za-z0-9_]{1,16}");
    }

    private void addProfile() {
        if (!validName()) {
            status = "Use 1-16 letters, numbers or underscores.";
            return;
        }
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
        SessionChanger.getInstance().setUserOffline(username, uuid);
        AltSaver.addAlt(username, uuid, "0", true, false);
        navigate(() -> mc.displayGuiScreen(new AltManagerGui()));
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        if (isTransitioning()) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            navigate(() -> mc.displayGuiScreen(new AltManagerGui()));
        } else if (keyCode == Keyboard.KEY_RETURN) {
            addProfile();
        } else if (keyCode == Keyboard.KEY_BACK && !username.isEmpty()) {
            username = username.substring(0, username.length() - 1);
        } else if (ChatAllowedCharacters.isAllowedCharacter(character) && username.length() < 16) {
            username += character;
        }
    }
}
