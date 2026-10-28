package Windy.event.impl;

import Windy.event.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SafeWalkEvent extends Event {
    private boolean safeWalk;


}
