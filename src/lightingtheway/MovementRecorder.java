package lightingtheway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovementRecorder {

    final Logger mLogger = LoggerFactory.getLogger("MovementRecorder");

    private double mDroneXpos = 0, mDroneYpos = 0;     // in meters
    // Drone base angle 'forward' is considered 0 degrees,
    private double mDroneCurrentAngle = 0;        // in degrees

    private double mUserXpos = 0, mUserYpos = 0;

    public MovementRecorder(){
    }

    public void incrementDroneX(double dx){ mDroneXpos += dx*-1*Math.sin(Math.toRadians(mDroneCurrentAngle)); }

    public void incrementDroneY(double dy){ mDroneYpos += dy*Math.cos(Math.toRadians(mDroneCurrentAngle)); }

    public void incrementDroneAngle(double dA) { mDroneCurrentAngle += dA; }

    public void setCurrentDroneAngle(double angle) { mDroneCurrentAngle = angle; }

    public void incrementDroneAll(double dx, double dy, double dA) {
        incrementDroneAngle(dA);
        incrementDroneX(dx);
        incrementDroneY(dy);
        mLogger.debug(toString());
    }

    public String toString(){
        return "xPos: " + mDroneXpos + "; yPos: " + mDroneYpos + "; curAngle(degrees): " + mDroneCurrentAngle;
    }
}
