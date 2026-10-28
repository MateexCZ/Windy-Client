package Windy.event.impl;

import Windy.event.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MotionEvent extends Event {

    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;

    public MotionEvent(Event.Type type, double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.setType(type);
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }


}