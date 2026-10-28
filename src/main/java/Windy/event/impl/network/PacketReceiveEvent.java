package Windy.event.impl.network;

import Windy.event.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.Packet;

@Setter
@Getter
public class PacketReceiveEvent extends Event {

    private Packet<?> packet;

    public PacketReceiveEvent(Packet<?> packet) {
        this.packet = packet;
    }

}
