package Windy.ui.auth;

import Windy.font.CustomFont;
import Windy.ui.modern.ModernScreen;
import Windy.util.auth.MicrosoftAuth;
import Windy.util.render.RenderUtil;
import Windy.util.render.VisualTheme;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;


class PCIYWB extends ModernScreen {
    private volatile String status = "Complete sign-in in your browser.";
    private volatile boolean cancelled;
    private boolean started;
    private ExecutorService executor;
    private CompletableFuture<?> task;

    PCIYWB() {
        super(380, 210);
    }

    @Override
    public void initGui() {
        super.initGui();
        if (started) {
            return;
        }
        started = true;
        executor = Executors.newSingleThreadExecutor(action -> {
            Thread thread = new Thread(action, "Windy Microsoft sign-in");
            thread.setDaemon(true);
            return thread;
        });
        task = MicrosoftAuth.acquireMSAuthCode(executor)
                .thenComposeAsync(code -> MicrosoftAuth.acquireMSAccessToken(code, executor), executor)
                .thenComposeAsync(token -> MicrosoftAuth.acquireXboxAccessToken(token, executor), executor)
                .thenComposeAsync(token -> MicrosoftAuth.acquireXboxXstsToken(token, executor), executor)
                .thenComposeAsync(data -> MicrosoftAuth.acquireMCAccessToken(data.get("Token"), data.get("uhs"), executor), executor)
                .thenComposeAsync(token -> MicrosoftAuth.login(token, executor), executor)
                .thenAccept(session -> Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (cancelled || mc.currentScreen != this) {
                        return;
                    }
                    SessionChanger.getInstance().setSession(session);
                    MicrosoftAuth.completeBrowserSignIn();
                    AltSaver.addAlt(session.getUsername(), session.getPlayerID(), session.getToken(), false, true);
                    mc.displayGuiScreen(new AltManagerGui());
                }))
                .exceptionally(error -> {
                    if (!cancelled) {
                        MicrosoftAuth.failBrowserSignIn();
                        status = "Sign-in failed. Go back and try again.";
                    }
                    return null;
                });
    }

    @Override
    protected void renderMenu(float mouseX, float mouseY, float opacity) {
        RenderUtil.drawPanel(0, 0, 380, 210, opacity);
        CustomFont.drawString(CustomFont.light(28), "Microsoft sign-in", 24, 24, VisualTheme.TEXT, opacity);
        CustomFont.drawString(CustomFont.light(16), "A browser window opens for authentication.", 24, 70, VisualTheme.MUTED, opacity);
        CustomFont.drawString(CustomFont.light(18),
                CustomFont.trimToWidth(CustomFont.light(18), status, 330),
                24,
                101,
                VisualTheme.TEXT,
                opacity);
        button("cancel", "Back", 24, 155, 332, 31, mouseX, mouseY, true, false, this::cancel);
    }

    private void cancel() {
        cancelled = true;
        stopTask();
        navigate(() -> mc.displayGuiScreen(new AltManagerGui()));
    }

    private void stopTask() {
        MicrosoftAuth.cancelBrowserSignIn();
        if (task != null) {
            task.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    protected void keyTyped(char character, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancel();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        cancelled = true;
        stopTask();
    }
}
