package Windy.ui.auth;

import Windy.font.CustomFont;
import Windy.ui.mainmenu.MainMenu;
import Windy.ui.modern.ModernScreen;
import Windy.util.animation.AnimationUtil;
import Windy.util.render.ColorUtil;
import Windy.util.render.RenderUtil;
import Windy.util.render.SkinUtil;
import Windy.util.render.VisualTheme;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;


public final class AltManagerGui extends ModernScreen {
    private static final int LIST_X = 18;
    private static final int LIST_Y = 100;
    private static final int LIST_WIDTH = 348;
    private static final int LIST_HEIGHT = 266;
    private static final int ROW_HEIGHT = 52;

    private final List<AltSaver.AltAccount> visibleAccounts = new ArrayList<>();
    private AltSaver.AltAccount selectedAccount;
    private String query = "";
    private String status = "Choose an account to continue.";
    private String cachedQuery;
    private int cachedAccountCount = -1;
    private boolean searchFocused;
    private boolean confirmRemove;
    private float scroll;
    private float targetScroll;

    public AltManagerGui() {
        super(620, 390);
    }

    @Override
    public void initGui() {
        super.initGui();

        rebuildVisibleAccounts();
    }

    @Override
    protected void renderMenu(float mouseX, float mouseY, float opacity) {
        rebuildVisibleAccounts();
        clampScroll();
        scroll = AnimationUtil.approach(scroll, targetScroll, frameSeconds, 18f);
        RenderUtil.drawPanel(0, 0, 620, 390, opacity);
        CustomFont.drawString(CustomFont.light(28), "Accounts", 20, 20, VisualTheme.TEXT, opacity);
        CustomFont.drawString(CustomFont.light(16), "Your profiles, in one place.", 20, 44, VisualTheme.MUTED, opacity);
        button("back", "Back", 540, 19, 62, 28, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new MainMenu())));

        RenderUtil.drawRoundedRect(LIST_X, 67, LIST_WIDTH, 25, 5,
                AnimationUtil.multiplyAlpha(searchFocused ? 0xFF253143 : 0xFF1D2531, opacity));
        String search = query.isEmpty() && !searchFocused ? "Search accounts..." : query;
        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            search += "|";
        }
        CustomFont.drawString(CustomFont.light(16),
                CustomFont.trimToWidth(CustomFont.light(16), search, LIST_WIDTH - 20),
                28,
                75,
                VisualTheme.MUTED,
                opacity);
        renderAccountList(mouseX, mouseY, opacity);
        renderSelectedAccount(mouseX, mouseY, opacity);
    }

    private void renderAccountList(float mouseX, float mouseY, float opacity) {
        RenderUtil.scissor(LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT, scale, originX, originY);
        int firstVisible = Math.max(0, (int) (scroll / ROW_HEIGHT));
        int lastVisible = Math.min(visibleAccounts.size(), firstVisible + LIST_HEIGHT / ROW_HEIGHT + 2);

        for (int index = firstVisible; index < lastVisible; index++) {
            AltSaver.AltAccount account = visibleAccounts.get(index);
            float rowY = LIST_Y + index * ROW_HEIGHT - scroll;
            boolean hovered = RenderUtil.contains(mouseX, mouseY, LIST_X, rowY, LIST_WIDTH - 5, ROW_HEIGHT - 6);
            boolean selected = account == selectedAccount;
            int color = selected ? VisualTheme.mix(0xFF202834, VisualTheme.accent(), .18f) : hovered ? 0xFF242E3B : 0xFF1A222E;
            RenderUtil.drawRoundedRect(LIST_X, rowY, LIST_WIDTH - 5, ROW_HEIGHT - 6, 6, AnimationUtil.multiplyAlpha(color, opacity));
            SkinUtil.drawSkinHead(account.username, account.uuid, LIST_X + 10, Math.round(rowY + 9), 28, opacity);
            CustomFont.drawString(CustomFont.light(18),
                    CustomFont.trimToWidth(CustomFont.light(18), cleanName(account), LIST_WIDTH - 75),
                    LIST_X + 49,
                    rowY + 10,
                    VisualTheme.TEXT,
                    opacity);
            CustomFont.drawString(CustomFont.light(16),
                    account.isMicrosoft ? "Microsoft" : "Cracked",
                    LIST_X + 49,
                    rowY + 28,
                    VisualTheme.MUTED,
                    opacity);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (visibleAccounts.isEmpty()) {
            CustomFont.drawCenteredString(CustomFont.light(18),
                    query.isEmpty() ? "No saved accounts yet" : "No matching accounts",
                    LIST_X + LIST_WIDTH / 2f,
                    LIST_Y + 90,
                    VisualTheme.MUTED,
                    opacity);
        }
        float maximum = maximumScroll();
        if (maximum > 0) {
            float thumb = Math.max(24, LIST_HEIGHT * LIST_HEIGHT / (float) (visibleAccounts.size() * ROW_HEIGHT));
            float thumbY = LIST_Y + (LIST_HEIGHT - thumb) * scroll / maximum;
            RenderUtil.drawRoundedRect(LIST_X + LIST_WIDTH - 2,
                    thumbY,
                    2,
                    thumb,
                    1,
                    AnimationUtil.multiplyAlpha(VisualTheme.MUTED, opacity * .55f));
        }
    }

    private void renderSelectedAccount(float mouseX, float mouseY, float opacity) {
        int centerX = 489;
        AltSaver.AltAccount current = selectedAccount;
        if (current != null) {
            SkinUtil.drawSkinHead(current.username, current.uuid, centerX - 24, 78, 48, opacity);
            CustomFont.drawCenteredString(CustomFont.light(20),
                    CustomFont.trimToWidth(CustomFont.light(20), cleanName(current), 200),
                    centerX,
                    139,
                    VisualTheme.TEXT,
                    opacity);
            CustomFont.drawCenteredString(CustomFont.light(16),
                    current.isMicrosoft ? "Microsoft profile" : "Cracked profile",
                    centerX,
                    160,
                    VisualTheme.MUTED,
                    opacity);
        } else {
            CustomFont.drawCenteredString(CustomFont.light(20), "Select a profile", centerX, 121, VisualTheme.TEXT, opacity);
        }
        button("use", "Use selected", 385, 191, 207, 32, mouseX, mouseY, current != null, true, this::useSelectedAccount);
        button("remove", confirmRemove ? "Confirm remove" : "Remove", 385, 232, 207, 29,
                mouseX, mouseY, current != null, false, this::removeSelectedAccount);
        CustomFont.drawString(CustomFont.light(16),
                CustomFont.trimToWidth(CustomFont.light(16), status, 207),
                385,
                278,
                VisualTheme.MUTED,
                opacity);
        button("microsoft", "Add Microsoft", 385, 301, 207, 29, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new PCIYWB())));
        button("offline", "Add Cracked", 385, 339, 207, 29, mouseX, mouseY, true, false,
                () -> navigate(() -> mc.displayGuiScreen(new GuiLogin())));
    }

    private void rebuildVisibleAccounts() {
        if (query.equals(cachedQuery) && cachedAccountCount == AltSaver.accounts.size()) {
            return;
        }
        cachedQuery = query;
        cachedAccountCount = AltSaver.accounts.size();
        visibleAccounts.clear();
        String normalized = query.toLowerCase(Locale.ROOT);
        for (AltSaver.AltAccount account : AltSaver.accounts) {
            if (cleanName(account).toLowerCase(Locale.ROOT).contains(normalized)) {
                visibleAccounts.add(account);
            }
        }
        if (selectedAccount != null && !AltSaver.accounts.contains(selectedAccount)) {
            selectedAccount = null;
        }
        clampScroll();
    }

    private float maximumScroll() {
        return Math.max(0, visibleAccounts.size() * ROW_HEIGHT - LIST_HEIGHT);
    }

    private void clampScroll() {
        targetScroll = Math.max(0, Math.min(maximumScroll(), targetScroll));
        scroll = Math.max(0, Math.min(maximumScroll(), scroll));
    }

    private static String cleanName(AltSaver.AltAccount account) {
        return account.username == null ? "Unknown" : ColorUtil.stripColor(account.username);
    }

    private void useSelectedAccount() {
        if (selectedAccount == null) {
            return;
        }
        AltSaver.AltAccount account = selectedAccount;
        String token = account.token == null ? "0" : account.token;
        Session session = new Session(cleanName(account), account.uuid, token, account.isMicrosoft ? "mojang" : "legacy");
        SessionChanger.getInstance().setSession(session);
        status = "Active: " + cleanName(account);
        confirmRemove = false;
    }

    private void removeSelectedAccount() {
        if (selectedAccount == null) {
            return;
        }
        if (!confirmRemove) {
            confirmRemove = true;
            status = "Click again to remove this profile.";
            return;
        }
        AltSaver.removeAlt(selectedAccount);
        selectedAccount = null;
        confirmRemove = false;
        cachedAccountCount = -1;
        status = "Profile removed.";
        rebuildVisibleAccounts();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isTransitioning()) {
            return;
        }
        float localX = toLocalX(mouseX);
        float localY = toLocalY(mouseY);
        searchFocused = RenderUtil.contains(localX, localY, LIST_X, 67, LIST_WIDTH, 25);
        if (mouseButton == 0 && RenderUtil.contains(localX, localY, LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            int row = (int) ((localY - LIST_Y + scroll) / ROW_HEIGHT);
            float withinRow = (localY - LIST_Y + scroll) % ROW_HEIGHT;
            if (row >= 0 && row < visibleAccounts.size() && withinRow < ROW_HEIGHT - 6) {
                selectedAccount = visibleAccounts.get(row);
                confirmRemove = false;
                status = "Ready to use " + cleanName(selectedAccount);
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        if (isTransitioning()) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (searchFocused) {
                searchFocused = false;
            } else {
                navigate(() -> mc.displayGuiScreen(new MainMenu()));
            }
            return;
        }
        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            } else if (ChatAllowedCharacters.isAllowedCharacter(character) && query.length() < 48) {
                query += character;
            }
            targetScroll = 0;
        } else if (keyCode == Keyboard.KEY_RETURN) {
            useSelectedAccount();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        float localX = toLocalX(Mouse.getEventX() * width / mc.displayWidth);
        float localY = toLocalY(height - Mouse.getEventY() * height / mc.displayHeight - 1);
        if (!isTransitioning() && wheel != 0 && RenderUtil.contains(localX, localY, LIST_X, LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            targetScroll += wheel > 0 ? -36 : 36;
            clampScroll();
        }
    }
}
