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

    private float mVx = 0;
    private float mVy = 0;
    private float mYawRate = 0;
    private float mZdistance = 0.1f;

    private float mRoll;
    private float mPitch;
    private float mYaw;
    private float mThrust;
    private float mMaxThrust;
    private float mMinThrust;
    private boolean mIsFlying = false;

    private Crazyflie mCrazyFlie;
    private Controls mControls;

    private PriorityQueue<CfCommand> mQueue = new PriorityQueue<>(20);

    public final static int absMaxThrust = 65535;
    public final static int commandRate = 200;

    private float mLiftOffThrustControlFactor = .60f;
    private float mLiftOffThrust = mLiftOffThrustControlFactor * absMaxThrust;
    private float mHoverThrustControlFactor = .56f;
    private float mHoverThrust = mHoverThrustControlFactor * absMaxThrust;



    private Thread mPacketSender = new Thread(new Runnable() {
        @Override
        public void run() {
            CfCommand currentCommand = new CfCommand();
            while (mCrazyFlie != null) {
                float timeLeft = -1;
                // If not grounded then look at queue
                if (mState != STATE.GROUNDED) {
                    // if we have no time left on current packet then look at nxt packet
                    if (timeLeft < 0) {
                        currentCommand = getNextCommand();
                        // if nothing in queue then hover w/ deafult settings
                        if (currentCommand == null) {
                            mCrazyFlie.sendPacket(new HoverPacket(mVy, mVx, mYawRate, mZdistance));
                        } else {
                            mCrazyFlie.sendPacket(currentCommand.mHoverPacket);
                            timeLeft = currentCommand.mTime;
                        }
                    } else {
                        mCrazyFlie.sendPacket(currentCommand.mHoverPacket);
                        timeLeft -= commandRate;
                    }
                }
                try {
                    Thread.sleep(commandRate);
                } catch (InterruptedException e) {
                    break;
                }

            }
        }
    });

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
        LEFT,RIGHT,FORWARD,BACK,UP
    }

    private static final int MovePriority = 2;

    public class CfCommand implements Comparable{

        // time in ms
        private float mTime;
        private float mRemainingTime;
        // used for priority queue to allow overrides
        public int mPriority;
        public HoverPacket mHoverPacket;

        public CfCommand(){}

        public CfCommand(Direction dir,float time, float velocity, float yawRate){
            mPriority = MovePriority;
            mTime = time;
            mRemainingTime = time;
            switch(dir) {
                //  NOTE: The crazyflie sees "forward" as +x and "right" as +y
                case FORWARD:
                    mHoverPacket = new HoverPacket(-velocity,0,0,mZdistance);
                    break;
                case BACK:
                    mHoverPacket = new HoverPacket(velocity,0,0,mZdistance);
                    break;
                case RIGHT:
                    mHoverPacket = new HoverPacket(0,velocity,0,mZdistance);
                    break;
                case LEFT:
                    mHoverPacket = new HoverPacket(0,-velocity,0,mZdistance);
                    break;
                case UP:
                default:
                    break;

            }
        }

        // @param
        public CfCommand(Direction dir,float time, float velocity, float yawRate, int priority){
            this(dir,time,velocity,yawRate);
            mPriority = priority;
        }

        public float decrementTime(float time){
            mRemainingTime -= time;
            return mRemainingTime;
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
    }

    public boolean goRight(float time){
        return addCommand(new CfCommand(Direction.RIGHT,time,mVx,0));
    }

    public boolean goLeft(float time){
        return addCommand(new CfCommand(Direction.LEFT,time,mVx,0));
    }

    public boolean goForward(float time){
        return addCommand(new CfCommand(Direction.FORWARD,time,mVx,0));
    }

    public boolean goBack(float time){
        return addCommand(new CfCommand(Direction.BACK,time,mVx,0));
    }

    public boolean addCommand(CfCommand cmnd){
        return mQueue.offer(cmnd);
    }

    public CfCommand getNextCommand(){
        return mQueue.poll();
    }

    public void liftOff(){
        if (mState == STATE.GROUNDED){
            mLogger.debug("[PacketControl]: liftOff Called!");
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

            mPacketSender.start();
            mThrust = mLiftOffThrust;
            try {
                Thread.sleep(25);
                mThrust = mHoverThrust;
                mState = STATE.CALIBRATING;
            } catch (InterruptedException e) {
                mThrust = 0;
            }
            //mCrazyFlie.setParamValue("flightmode.althold",1);

        } else {
            mLogger.debug("[PacketControl]: Not Grounded!");
        }
    }

    public void land(){
        if (mState != STATE.GROUNDED){
            mLogger.debug("[PacketControl]: land Called!");
            mState = STATE.GROUNDED;
            mPacketSender.interrupt();
            mPacketSender = null;

            mYaw = 0;
            mPitch = 0;
            mRoll = 0;
            mThrust = 0.0f;
            mVx = 0;
            mVy = 0;
            mZdistance = 0;
            mYawRate = 0;
            mCrazyFlie.sendPacket(new CommanderPacket(0, 0, 0, (char) (0.0f), mControls.isXmode()));
        } else {
            mLogger.debug("[PacketControl]: Not Airborn!");
        }
    }

    public void up(){
        if (!mIsFlying){
            mIsFlying = true;
        }
        return;
    }

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
