package Windy.module.impl.player;

import Windy.event.impl.network.PacketSendEvent;
import Windy.module.Category;
import Windy.module.Module;
import Windy.module.ModuleInfo;
import Windy.setting.impl.ModeSetting;
import Windy.setting.impl.NumberSetting;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.world.World;

@ModuleInfo(name = "Disabler", description = "Server-specific sprint or transaction packet handling", category = Category.Player)
public final class Disabler extends Module {
    private final ModeSetting mode = new ModeSetting("Mode", "Suppress Sprint", "Delay Transactions");
    private final NumberSetting delay = new NumberSetting("Delay ms", 350, 100, 1000, 50);
    private final Deque<Queued> pending = new ArrayDeque<>();
    private NetHandlerPlayClient connection;
    private World world;

    public Disabler() {
        addSettings(mode, delay);
        delay.addParent(mode, setting -> setting.getCurrMode().equals("Delay Transactions"));
    }
    private boolean syncConnection() {
        NetHandlerPlayClient current = mc.getNetHandler();
        if (connection != current || world != mc.theWorld) {
            pending.clear();
            connection = current;
            world = mc.theWorld;
        }
        return connection != null && world != null && mc.thePlayer != null;
    }
    @Override public synchronized void onEnable() { pending.clear(); connection = null; world = null; syncConnection(); }
    @Override public synchronized void onPacketSendEvent(PacketSendEvent event) {
        if (event.isCancelled() || !syncConnection()) return;
        if (mode.getCurrMode().equals("Suppress Sprint") && event.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction.Action action = ((C0BPacketEntityAction) event.getPacket()).getAction();
            if (action == C0BPacketEntityAction.Action.START_SPRINTING || action == C0BPacketEntityAction.Action.STOP_SPRINTING)
                event.setCancelled(true);
        } else if (mode.getCurrMode().equals("Delay Transactions") && event.getPacket() instanceof C0FPacketConfirmTransaction) {
            if (pending.size() >= 100) flush();
            pending.addLast(new Queued(event.getPacket(), System.nanoTime()));
            event.setCancelled(true);
        }
    }
    @Override public synchronized void onUpdate() {
        if (!syncConnection()) return;
        long now = System.nanoTime();
        long wait = (long) (delay.getVal() * 1_000_000L);
        while (!pending.isEmpty() && now - pending.peekFirst().time >= wait)
            connection.addToSendQueueNoEvent(pending.removeFirst().packet);
    }
    private void flush() {
        if (connection == null) { pending.clear(); return; }
        while (!pending.isEmpty()) connection.addToSendQueueNoEvent(pending.removeFirst().packet);
    }
    @Override public synchronized void onDisable() {
        if (syncConnection()) {
            flush();
            if (mode.getCurrMode().equals("Suppress Sprint")) {
                C0BPacketEntityAction.Action action = mc.thePlayer.isSprinting()
                        ? C0BPacketEntityAction.Action.START_SPRINTING : C0BPacketEntityAction.Action.STOP_SPRINTING;
                connection.addToSendQueueNoEvent(new C0BPacketEntityAction(mc.thePlayer, action));
            }
        }
        pending.clear(); connection = null; world = null;
    }
    private static final class Queued {
        final Packet<?> packet;
        final long time;
        Queued(Packet<?> packet, long time) { this.packet = packet; this.time = time; }
    }
}
