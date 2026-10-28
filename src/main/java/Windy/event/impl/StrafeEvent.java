package Windy.event.impl;

import Windy.event.Event;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StrafeEvent extends Event {
    private float forward;
    private float strafe;
    private float friction;


    public StrafeEvent(float forward, float strafe, float friction) {
        this.forward = forward;
        this.strafe = strafe;
        this.friction = friction;
    }
}
