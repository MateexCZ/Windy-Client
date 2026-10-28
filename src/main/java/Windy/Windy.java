package Windy;

import Windy.config.ConfigManager;
import Windy.event.impl.EventKey;
import Windy.module.Module;
import Windy.module.ModuleManager;
import Windy.module.impl.ui.ClickGUI;
import Windy.ui.auth.AltSaver;
import Windy.viamcp.impl.ViaMCP;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.Getter;
import me.zero.alpine.bus.EventBus;
import me.zero.alpine.bus.EventManager;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import me.zero.alpine.listener.Subscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

@Getter
public enum Windy implements Subscriber {
    INSTANCE;

    private String clientName = "Windy Client";
    private String clientVersion = "B4";
    public static String clientCredits = "Ty pijes colu zero jo? Tak ses pica? Kyselina fosforecna";


    public GuiScreen guiMainMenu;


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final ExecutorService onlineConfigExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService onlineScriptExecutor = Executors.newSingleThreadExecutor();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final FontRenderer fr = mc.fontRendererObj;

    private ModuleManager moduleManager;
    private ConfigManager configManager;

    public static final EventBus BUS = EventManager.builder()
            .setName("root/Windy")
            .setSuperListeners()
            .build();


    public void init() {
        BUS.subscribe(this);
        Display.setTitle(clientName + " " + clientVersion);


        moduleManager = new ModuleManager();
        configManager = new ConfigManager();


        configManager.loadConfig();

        ViaMCP.create();
        ViaMCP.INSTANCE.initAsyncSlider();

        AltSaver.loadAlts();

    }


    public void shutdown() {
        BUS.unsubscribe(this);
        configManager.saveConfig();
        AltSaver.saveAlts();
    }

    public boolean isToggled(Class<? extends Module> c) {
        Module m = INSTANCE.getModuleManager().getModule(c);
        return m != null && m.isToggled();
    }

    @Subscribe
    private final Listener<EventKey> keyListener = new Listener<>(e -> {
        if (e.getKey() == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(ClickGUI.createScreen());
            return;
        }
        if (mc.currentScreen == null && e.getKey() != Keyboard.KEY_NONE) {
            for (Module module : moduleManager.getModules().values()) {
                if (module.getKey() == e.getKey()) {
                    module.toggle();
                }
            }
        }
    });
}
