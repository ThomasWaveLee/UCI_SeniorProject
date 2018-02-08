package lightingtheway;

public class MovementRecorder {

    private double mXpos = 0, mYpos = 0;     // in meters
    // base angle 'forward' is considered 0 degrees,
    private double mCurrentAngle = 0;        // in degrees

    public MovementRecorder(){
    }

    public void incrementX(double dx){ mXpos += dx; }

    public void incrementY(double dy){ mYpos += dy; }

    public void incrementAngle(double dA) { mCurrentAngle += dA; }

    public void incrementAll(float dx, float dy, float dA) {
        incrementX(dx*Math.sin(Math.toRadians(mCurrentAngle)));
        incrementY(dy*Math.cos(Math.toRadians(mCurrentAngle)));
        incrementAngle(dA);
    }
}
