package Windy.event.impl;

import Windy.event.Event;

public class NametagRenderEvent extends Event {
    private final net.minecraft.entity.player.EntityPlayer player;
    public NametagRenderEvent() { this(null); }
    public NametagRenderEvent(net.minecraft.entity.player.EntityPlayer player) { this.player = player; }
    public net.minecraft.entity.player.EntityPlayer getPlayer() { return player; }
}
