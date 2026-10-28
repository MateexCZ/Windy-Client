package Windy.event.impl;

import Windy.event.Event;
import lombok.Getter;

@Getter
public class TickEvent extends Event {

    private final int ticks;

    public TickEvent(int ticks) {
        this.ticks = ticks;
    }

}
