package lightingtheway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovementRecorder {

    final Logger mLogger = LoggerFactory.getLogger("MovementRecorder");

    private double mXpos = 0, mYpos = 0;     // in meters
    // base angle 'forward' is considered 0 degrees,
    private double mCurrentAngle = 0;        // in degrees

    public MovementRecorder(){
    }

    public void incrementX(double dx){ mXpos += dx; }

    public void incrementY(double dy){ mYpos += dy; }

    public void incrementAngle(double dA) { mCurrentAngle += dA; }

    public void incrementAll(double dx, double dy, double dA) {
        //incrementX(dx*Math.cos(Math.toRadians(mCurrentAngle)));
        //incrementY(dy*Math.sin(Math.toRadians(mCurrentAngle)));
        incrementX(dx);
        incrementY(dy);
        incrementAngle(dA);
        mLogger.debug(toString());
    }

    public String toString(){
        return "xPos: " + mXpos + "; yPos: " + mYpos + "; curAngle(degrees): " + mCurrentAngle;
    }
}
