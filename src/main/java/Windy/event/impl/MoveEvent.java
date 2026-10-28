package Windy.event.impl;

import Windy.event.Event;
import Windy.util.movement.MovementUtils;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MoveEvent extends Event {

    private double x;
    private double y;
    private double z;

    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public void setSpeed(double speed) {
        MovementUtils.setSpeed(1, speed);
    }

}
