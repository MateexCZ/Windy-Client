package Windy.event.impl;

import Windy.event.Event;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Event3D extends Event {

        private float ticks;

        public Event3D(float ticks) {
            this.ticks = ticks;
        }


}
