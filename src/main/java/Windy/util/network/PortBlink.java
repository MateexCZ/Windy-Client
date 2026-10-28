package Windy.util.network;

import Windy.event.impl.network.PacketSendEvent;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;




public final class PortBlink {
    private static final Set<String> owners = new HashSet<>();
    private static final Deque<Packet<?>> packets = new ArrayDeque<>();
    private static java.lang.ref.WeakReference<Object> world = new java.lang.ref.WeakReference<>(null);
    
    private static void checkWorld() {
        Object current = Minecraft.getMinecraft().theWorld;
        if (world.get() != current) {
            packets.clear();
            owners.clear();
            world = new java.lang.ref.WeakReference<>(current);
        }
    }
    
    private PortBlink() {
    }
    
    public static synchronized void set(String owner, boolean active) {
        checkWorld();
        if (active) owners.add(owner); else owners.remove(owner);
        if (owners.isEmpty()) flush();
    }
    
    public static synchronized void capture(PacketSendEvent event) {
        checkWorld();
        if (owners.isEmpty() || event.isCancelled()) return;
        if (!(event.getPacket() instanceof net.minecraft.network.play.client.C03PacketPlayer) && !(event.getPacket() instanceof net.minecraft.network.play.client.C02PacketUseEntity) && !(event.getPacket() instanceof net.minecraft.network.play.client.C07PacketPlayerDigging) && !(event.getPacket() instanceof net.minecraft.network.play.client.C08PacketPlayerBlockPlacement) && !(event.getPacket() instanceof net.minecraft.network.play.client.C09PacketHeldItemChange)) return;
        if (packets.size() >= 256) flush();
        packets.addLast(event.getPacket());
        event.setCancelled(true);
    }
    
    public static synchronized void flush() {
        checkWorld();
        if (Minecraft.getMinecraft().getNetHandler() == null) {
            packets.clear();
            return;
        }
        while (!packets.isEmpty()) PacketUtils.sendPacketWithoutEvent(packets.removeFirst());
    }
}
