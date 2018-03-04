package lightingtheway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MovementRecorder {

    final Logger mLogger = LoggerFactory.getLogger("MovementRecorder");

    private double mDroneXpos = 0, mDroneYpos = 0;     // in meters
    // Drone base angle 'forward' is considered 0 degrees,
    private double mDroneCurrentAngle = 0;        // in degrees
    private String mDroneCurSrc = "", mDroneCurDest = "";
    private float mPathDistTraveled = 0, mPathDistToTravel = 0;

    private double mUserXpos = 0, mUserYpos = 0;

    public MovementRecorder(){
    }

    private double incrementDroneX(double dx){
        double dx_inc = dx*Math.cos(Math.toRadians(mDroneCurrentAngle));
        double dy_inc = dx*Math.sin(Math.toRadians(mDroneCurrentAngle));
        mDroneXpos += dx_inc;
        mDroneYpos += dy_inc;
        mPathDistTraveled += Math.sqrt(dx_inc*dx_inc + dy_inc*dy_inc);
        return 0;
    }

    private double incrementDroneY(double dy){
        double inc = dy*Math.sin(Math.toRadians(mDroneCurrentAngle));
        mDroneYpos += inc;
        return inc;
    }

    private void incrementDroneAngle(double dA) { mDroneCurrentAngle += dA; }

    public void setCurrentDroneAngle(double angle) { mDroneCurrentAngle = angle; }

    public void setDroneCurrentSrcDest(String src,String dest, float dist) {
        mDroneCurSrc = src;
        mDroneCurDest = dest;
        mPathDistToTravel = dist;
        mPathDistTraveled = 0;
    }

    public void incrementDroneAll(double dx, double dy, double dA) {
        incrementDroneAngle(dA);
        // currently we only use goForward, as such only dx is relevant
        incrementDroneX(dx);
        //double deltaY = incrementDroneY(dy);

        //mLogger.debug(toString());
    }

    public String toString(){
        return "xPos: " + mDroneXpos + "; yPos: " + mDroneYpos + "; curAngle(degrees): " + mDroneCurrentAngle;
    }
}
