package Windy.util.rotation;

public class PortRotation {
    public float yaw;
    public float pitch;

    public PortRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public PortRotation plus(PortRotation rotation) {
        return new PortRotation(yaw + rotation.yaw, pitch + rotation.pitch);
    }

    public PortRotation plus(float num) {
        return new PortRotation(yaw + num, pitch + num);
    }

    @Override
    public String toString() {
        return "PortRotation(" + yaw + ", " + pitch + ")";
    }
}