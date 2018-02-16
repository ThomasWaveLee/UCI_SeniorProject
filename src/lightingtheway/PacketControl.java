package lightingtheway;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.PriorityQueue;

import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.HoverPacket;
import se.bitcraze.crazyfliecontrol.controller.Controls;


public class PacketControl {

    final Logger mLogger = LoggerFactory.getLogger("PacketControl");

    private enum STATE {
        GROUNDED, LIFTOFF, CALIBRATING, FLYING, LANDING
    }

    public STATE mState = STATE.GROUNDED;

    public final static int absMaxThrust = 65535;
    // period in ms at which to send packet to crazyflie
    public final static int gCommandRate = 50;
    // base height to hover at
    public final static float gBaseHeight = 0.1f;

    // velocities and z distance
    private float mVx = 0;
    private float mVy = 0;
    private float mYawRate = 0;
    private float mZdistance = gBaseHeight;

    private float mRoll;
    private float mPitch;
    private float mYaw;
    private float mThrust;
    private float mMaxThrust;
    private float mMinThrust;
    private boolean mIsFlying = false;

    // crazyflie settings/connection
    private Crazyflie mCrazyFlie;
    private Controls mControls;

    private PriorityQueue<CfCommand> mQueue = new PriorityQueue<>(20);

    private MovementRecorder mMovementRecorder = new MovementRecorder();

    // Thread to run when set to auto flight
    private Thread mPacketSender;

    private float mLiftOffThrustControlFactor = .60f;
    private float mLiftOffThrust = mLiftOffThrustControlFactor * absMaxThrust;
    private float mHoverThrustControlFactor = .56f;
    private float mHoverThrust = mHoverThrustControlFactor * absMaxThrust;

    public PacketControl(){
        mState = STATE.GROUNDED;
        mThrust = 0;
        mRoll = 0;
        mPitch = 0;
        mYaw = 0;
        mThrust = mLiftOffThrust;
    }

    public PacketControl(Crazyflie cf, Controls control){
        mState = STATE.GROUNDED;
        mThrust = 0;
        mRoll = 0;
        mPitch = 0;
        mYaw = 0;
        mThrust = mLiftOffThrust;
        mCrazyFlie = cf;
        mMaxThrust = control.getMaxThrust();
        mMinThrust = control.getMinThrust();
        mControls = control;
    }

    public void setCF(Crazyflie  cf){
        mCrazyFlie = cf;
    }

    public void setControl(Controls control){
        if (control == null)
            return;
        mMaxThrust = control.getMaxThrust();
        mMinThrust = control.getMinThrust();
        mControls = control;
    }

    // Direction for Commands to send
    public enum Direction{
        LEFT,RIGHT,FORWARD,BACK,UP,TURN_LEFT, TURN_RIGHT
    }



    // internal class to hold HoverPacket and time info
    public class CfCommand implements Comparable{
        private static final int gMovePriority = 2;
        // time in ms
        private float mTime;
        private float mVx = 0, mVy = 0, mYawrate = 0;
        public float mRemainingTime;
        // used for priority queue to allow overrides
        public int mPriority;
        public HoverPacket mHoverPacket;

        public CfCommand(){}

        public CfCommand(Direction dir,float time, float velocity, float yawRate){
            mPriority = gMovePriority;
            mTime = time;
            mRemainingTime = mTime;

            switch(dir) {
                //  NOTE: The crazyflie sees "forward" as +x and "right" as +y
                case FORWARD:
                    mHoverPacket = new HoverPacket(velocity,0,0,mZdistance);
                    mVx = velocity;
                    break;
                case BACK:
                    mHoverPacket = new HoverPacket(-velocity,0,0,mZdistance);
                    mVx = -velocity;
                    break;
                case RIGHT:
                    mHoverPacket = new HoverPacket(0,velocity,0,mZdistance);
                    mVy = velocity;
                    break;
                case LEFT:
                    mHoverPacket = new HoverPacket(0,-velocity,0,mZdistance);
                    mVy = -velocity;
                    break;
                case TURN_LEFT:
                    mHoverPacket = new HoverPacket(0,0,-yawRate,mZdistance);
                    break;
                case TURN_RIGHT:
                    mHoverPacket = new HoverPacket(0,0,yawRate,mZdistance);
                    break;
                case UP:
                default:
                    mHoverPacket = new HoverPacket(0,0,0,mZdistance);
                    break;

            }
        }

        public CfCommand(Direction dir,float time, float velocity, float yawRate, int priority){
            this(dir,time,velocity,yawRate);
            mPriority = priority;
        }

        public int compareTo(Object o){
            if (o instanceof CfCommand){
                int p = ((CfCommand)o).mPriority;
                if (p > this.mPriority) return -1;
                if (p < this.mPriority) return 1;
                return 0;
            }
            return 0;
        }

        public boolean equals(Object o){
            if (o instanceof CfCommand){
                return this.mPriority == ((CfCommand)o).mPriority;
            }
            return false;
        }

        public String toString(){
            return mHoverPacket + "; Time: " + mTime + "; Priority: " + mPriority;
        }
    }
    // End of internal class


    // methods that input commands into queue to be sent to crazyflie
    public boolean goRight(float time){
        return addCommand(new CfCommand(Direction.RIGHT,time,mVy,0));
    }

    public boolean goRight(float dist, float vx){
        float time = toMS(dist/vx);
        return addCommand(new CfCommand(Direction.RIGHT,time,vx,0));
    }

    public boolean goLeft(float time){
        return addCommand(new CfCommand(Direction.LEFT,time,mVy,0));
    }

    public boolean goLeft(float dist, float vx){
        float time = dist/vx * 1000;
        return addCommand(new CfCommand(Direction.LEFT,time,vx,0));
    }

    public boolean goForward(float time){
        return addCommand(new CfCommand(Direction.FORWARD,time,mVx,0));
    }

    public boolean goForward(float dist, float vx){
        float time = toMS(dist/vx);
        return addCommand(new CfCommand(Direction.FORWARD,time,vx,0));
    }

    public boolean goBack(float time){
        return addCommand(new CfCommand(Direction.BACK,time,mVx,0));
    }

    public boolean goBack(float dist, float vx){
        float time = toMS(dist/vx);
        return addCommand(new CfCommand(Direction.BACK,time,vx,0));
    }

    public boolean turnLeft(float time){
        return addCommand(new CfCommand(Direction.TURN_LEFT,time,0,mYawRate));
    }

    public boolean turnLeft(float angle, float yawrate){
        float time = toMS(angle/yawrate);
        return addCommand(new CfCommand(Direction.TURN_LEFT,time,0,yawrate));
    }

    public boolean turnRight(float time){
        return addCommand(new CfCommand(Direction.TURN_RIGHT,time,0,mYawRate));
    }

    public boolean turnRight(float angle, float yawrate){
        float time = toMS(angle/yawrate);
        return addCommand(new CfCommand(Direction.TURN_RIGHT,time,0,yawrate));
    }
    // end of methods to send commands to crazyflie

    public float toMS(float sec){
        return sec * 1000;
    }

    public boolean addCommand(CfCommand cmnd){
        return mQueue.offer(cmnd);
    }

    public CfCommand getNextCommand(){
        return mQueue.poll();
    }

    // if crayzflie is GROUNDED then it will start the packet sender thread
    public void liftOff(){
        if (mState == STATE.GROUNDED){
            mLogger.debug("[PacketControl]: liftOff Called!");
            mPacketSender = new Thread(new Runnable() {
                @Override
                public void run() {
                    // init for compiler
                    CfCommand currentCommand = new CfCommand();
                    float timeLeft = -1;
                    while (mCrazyFlie != null) {
                        // If not grounded then look at queue
                        if (mState != STATE.GROUNDED) {
                            // if we have no time left on current packet then look at nxt packet
                            if (timeLeft < 0) {
                                currentCommand = getNextCommand();
                                // if nothing in queue then hover w/ default settings
                                if (currentCommand == null) {
                                    mCrazyFlie.sendPacket(new HoverPacket(0, 0, 0, mZdistance));
                                } else {
                                    currentCommand.mHoverPacket = new HoverPacket(currentCommand.mHoverPacket,mZdistance);
                                    mCrazyFlie.sendPacket(currentCommand.mHoverPacket);
                                    timeLeft = currentCommand.mTime;
                                }
                            } else {
                                // send pckt and update remaining time
                                mCrazyFlie.sendPacket(currentCommand.mHoverPacket);
                                currentCommand.mRemainingTime -= gCommandRate;
                                timeLeft = currentCommand.mRemainingTime;

                                // update app-side drone position
                                // movementRecorder holds position data in meters
                                //      need to divide by 1000 since gCommandRate is in ms and mVx is in m/s
                                mMovementRecorder.incrementAll(currentCommand.mVx*gCommandRate/1000.0,
                                        currentCommand.mVy*gCommandRate/1000.0,
                                        currentCommand.mYawrate*gCommandRate/1000.0);

                               // mLogger.debug(currentCommand + " , TimeLeft: " + timeLeft);
                            }
                        }
                        try {
                            Thread.sleep(gCommandRate);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            });
            /*
            mState = STATE.LIFTOFF;
            mThrust = mLiftOffThrust / 2.0f;
            mCrazyFlie.setParamValue("kalman.resetEstimation",1);
            try {
                Thread.sleep(100);
            } catch (Exception e) {}
            mCrazyFlie.setParamValue("kalman.resetEstimation",0);
            try {
                Thread.sleep(2000);
            } catch (Exception e) {}
            */
            mPacketSender.start();
            mState = STATE.FLYING;

        } else {
            mLogger.debug("[PacketControl]: Not Grounded!");
        }
    }

    // the the crazyflie isnt GROUNDED it will stop the packet sender thread and land
    public void land(){
        if (mState != STATE.GROUNDED){
            mLogger.debug("[PacketControl]: land Called!");
            mPacketSender.interrupt();
            mPacketSender = null;

            mYaw = 0;
            mPitch = 0;
            mRoll = 0;
            mThrust = 0.0f;
            mVx = 0;
            mVy = 0;
            mZdistance = gBaseHeight;
            mYawRate = 0;
            mQueue.clear();
            mCrazyFlie.sendPacket(new HoverPacket(0,0,0,gBaseHeight));
            try{
                Thread.sleep(gCommandRate*5);
            } catch (Exception e) {}

            mCrazyFlie.sendPacket(new CommanderPacket(0, 0, 0, (char) (0.0f), mControls.isXmode()));
            mState = STATE.GROUNDED;
        } else {
            mLogger.debug("[PacketControl]: Not Airborn!");
        }
    }

    // setter/utility functions
    public void incrementAll(int roll,int pitch, int yaw, float thrust){
        mRoll += roll;
        mPitch += pitch;
        mYaw += yaw;
        mThrust += thrust;
    }

    public void incrementZDistance(float dZ) {
        mZdistance += dZ;
        mLogger.debug("ZDistance Set to: " + mZdistance);
    }

    public void incrementVx(float dX){
        mVx += dX;
        mLogger.debug("Vx Set to: " + mVx);
    }

    public void incrementVy(float dY){
        mVy += dY;
        mLogger.debug("Vy Set to: " + mVy);
    }

    public void incrementYawRate(float dY){
        mYawRate += dY;
        mLogger.debug("YawRate Set to: " + mYawRate);
    }

    public void incrementLiftOffFactor(float change){
        mLiftOffThrustControlFactor += change;
        mLogger.debug("LiftOffThrustControlFactor Set to: " + mLiftOffThrustControlFactor);
    }

    public void incrementHoverFactor(float change){
        mHoverThrustControlFactor += change;
        mLogger.debug("HoverThrustControlFactor Set to: " + mHoverThrustControlFactor);
    }

    public void setRoll(int roll){
        mRoll= roll;
    }

    public void setPitch(int pitch){
        mPitch= pitch;
    }

    public void setYaw(int yaw){
        mYaw = yaw;
    }

    public void incrementThrust(int thrust){
        if(mThrust + thrust <= absMaxThrust*mMaxThrust) {
            mThrust += thrust;
        }
    }


    /**  Setter functions **/
    public void setMaxThrust(float thrust){
         mMaxThrust = thrust;
    }

    public void setMinThrust(float thrust){
        mMinThrust = thrust;
    }

    public String toString(){
        return "Yaw: " + mYaw + "; Pitch " + mPitch + "; Roll: " + mRoll + "; Thrust: " + mThrust;
    }
}
