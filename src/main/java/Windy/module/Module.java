package Windy.module;

import Windy.Windy;
import Windy.event.impl.*;
import Windy.event.impl.network.PacketReceiveEvent;
import Windy.event.impl.network.PacketSendEvent;
import Windy.module.impl.ui.Notifications;
import Windy.setting.Setting;
import Windy.setting.impl.KeybindSetting;
import Windy.util.animation.AnimationUtil.*;
import Windy.util.animation.AnimationUtil.Animation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import me.zero.alpine.listener.Subscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.commons.lang3.Validate;
import org.lwjgl.input.Keyboard;

@Getter
public abstract class Module implements Subscriber {
    private final String name, description;
    private final Category category;
    private final boolean enabledByDefault;
    private final List<Setting> settingList = new ArrayList<>();
    private boolean toggled;


    public final Animation anim = new Animation(300, Easing.EASE_OUT_QUART, 0f);

    protected static final Minecraft mc = Windy.INSTANCE.getMc();
    protected final FontRenderer fr = Windy.INSTANCE.getFr();

    private final KeybindSetting keybind = new KeybindSetting(Keyboard.KEY_NONE);

    public Module() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        Validate.notNull(info, "Confused annotation exception");

        this.name = info.name();
        this.description = info.description();
        this.category = info.category();
        this.enabledByDefault = info.enabled();

        if (enabledByDefault) toggle();
        addSettings(keybind);
    }

    public void toggle() {
        setEnabled(!toggled);
    }

    public void setEnabled(boolean state) {
        if (this.toggled == state) return;
        this.toggled = state;
        if (mc.theWorld != null) Notifications.moduleChanged(name, state);

        onToggle();

        if (state) {
            Windy.BUS.subscribe(this);
            Windy.BUS.subscribe(eventKeyListener);
            Windy.BUS.subscribe(eventUpdateListener);
            Windy.BUS.subscribe(event2DListener);
            Windy.BUS.subscribe(event3DListener);
            Windy.BUS.subscribe(eventMoveListener);
            Windy.BUS.subscribe(eventMoveInputListener);
            Windy.BUS.subscribe(eventStrafeListener);
            Windy.BUS.subscribe(eventSafeWalkListener);
            Windy.BUS.subscribe(eventNametagListener);
            Windy.BUS.subscribe(eventBlockPlaceable);
            Windy.BUS.subscribe(eventMotionListener);
            Windy.BUS.subscribe(eventPacketRecieveListener);
            Windy.BUS.subscribe(eventPacketSendListener);
            onEnable();
        } else {
            Windy.BUS.unsubscribe(this);
            Windy.BUS.unsubscribe(eventKeyListener);
            Windy.BUS.unsubscribe(eventUpdateListener);
            Windy.BUS.unsubscribe(event2DListener);
            Windy.BUS.unsubscribe(event3DListener);
            Windy.BUS.unsubscribe(eventMoveListener);
            Windy.BUS.unsubscribe(eventMoveInputListener);
            Windy.BUS.unsubscribe(eventStrafeListener);
            Windy.BUS.unsubscribe(eventNametagListener);
            Windy.BUS.unsubscribe(eventSafeWalkListener);
            Windy.BUS.unsubscribe(eventMotionListener);
            Windy.BUS.unsubscribe(eventBlockPlaceable);
            Windy.BUS.unsubscribe(eventPacketRecieveListener);
            Windy.BUS.unsubscribe(eventPacketSendListener);
            onDisable();
        }
    }

    protected void addSettings(Setting... settings) {
        settingList.addAll(Arrays.asList(settings));
    }


    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onUpdate() {
    }

    public void onBlockPlaceableEvent() {
    }

    public void onStrafe(StrafeEvent e) {
    }

    public void onSafeWalk(SafeWalkEvent e) {
    }


    public void onMoveInput(MoveInputEvent e) {
    }

    public void onMotionEvent(MotionEvent e) {
    }

    public void onNametagRenderEvent(NametagRenderEvent e) {
    }

    public void onMoveEvent(MoveEvent e) {
    }

    public void onPacketReceiveEvent(PacketReceiveEvent e) {
    }

    public void onPacketSendEvent(PacketSendEvent e) {
    }

    public void on2D(ScaledResolution sr) {
    }

    public void on3D(float ticks) {
    }


    public void onKey(int Key) {
    }

    public void onToggle() {
        if (mc.theWorld != null) {
            Windy.INSTANCE.getConfigManager().saveConfig();
        }
    }

    @Subscribe
    private final Listener<EventUpdate> eventUpdateListener = new Listener<>(e -> {
        if (toggled) onUpdate();
    });

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        if (toggled) on2D(e.getSr());
    });

    @Subscribe
    private final Listener<Event3D> event3DListener = new Listener<>(e -> {
        if (toggled) on3D(e.getTicks());
    });


    @Subscribe
    private final Listener<EventKey> eventKeyListener = new Listener<>(e -> {
        if (toggled) onKey(e.getKey());
    });

    @Subscribe
    private final Listener<NametagRenderEvent> eventNametagListener = new Listener<>(e -> {
        if (toggled) onNametagRenderEvent(e);
    });

    @Subscribe
    private final Listener<BlockPlaceableEvent> eventBlockPlaceable = new Listener<>(e -> {
        if (toggled) onBlockPlaceableEvent();
    });

    @Subscribe
    private final Listener<StrafeEvent> eventStrafeListener = new Listener<>(e -> {
        if (toggled) onStrafe(e);
    });

    @Subscribe
    private final Listener<SafeWalkEvent> eventSafeWalkListener = new Listener<>(e -> {
        if (toggled) onSafeWalk(e);
    });

    @Subscribe
    private final Listener<MoveInputEvent> eventMoveInputListener = new Listener<>(e -> {
        if (toggled) onMoveInput(e);
    });

    @Subscribe
    private final Listener<MotionEvent> eventMotionListener = new Listener<>(e -> {
        if (toggled) onMotionEvent(e);
    });

    @Subscribe
    private final Listener<MoveEvent> eventMoveListener = new Listener<>(e -> {
        if (toggled) onMoveEvent(e);
    });

    @Subscribe
    private final Listener<PacketReceiveEvent> eventPacketRecieveListener = new Listener<>(e -> {
        if (toggled) onPacketReceiveEvent(e);
    });

    @Subscribe
    private final Listener<PacketSendEvent> eventPacketSendListener = new Listener<>(e -> {
        if (toggled) onPacketSendEvent(e);
    });


    public void setKey(int code) {
        this.keybind.setCode(code);
    }

    public int getKey() {
        if (keybind != null) {
            return keybind.getCode();
        }
        return Keyboard.KEY_NONE;
    }
}

