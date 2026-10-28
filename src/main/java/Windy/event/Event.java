package Windy.event;

import lombok.Getter;
import lombok.Setter;
import me.zero.alpine.event.CancellableEvent;

@Setter
@Getter
public class Event extends CancellableEvent {
    private boolean cancelled;
    private Type type;

    public void cancel() {
        this.cancelled = true;
    }

    public boolean isPre() {
        return this.type == Type.PRE;
    }

    public boolean isPost() {
        return this.type == Type.POST;
    }

    public enum Type {
        PRE, POST
    }


}
