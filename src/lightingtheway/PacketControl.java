package lightingtheway;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.HoverPacket;
import se.bitcraze.crazyfliecontrol.controller.Controls;


public class PacketControl {

    final Logger mLogger = LoggerFactory.getLogger("PacketControl");

    private Thread mPacketSender;

    private enum STATE {
        GROUNDED, LIFTOFF, CALIBRATING, HOVER, MOVING, LANDING
    }

    public STATE mState = STATE.GROUNDED;

    private float mDeltaXThresh = 1.0f;
    private float mDeltaYThresh = 18.0f;
    private float mDeltaZThresh = 1.0f;

    private float mRoll;
    private float mPitch;
    private float mYaw;
    private float mThrust;
    private float mMaxThrust;
    private float mMinThrust;
    private boolean mIsFlying = false;

    private Crazyflie mCrazyFlie;
    private Controls mControls;

    // Potentially want to look at previous commmandPackets sent.
    // Hold up to historyLength of past packets
    private static final int historyLength = 10;
    private ArrayList<ArrayList<Object>> mHistory;

    public final static int absMaxThrust = 65535;

    private float mLiftOffThrustControlFactor = .60f;
    private float mLiftOffThrust = mLiftOffThrustControlFactor * absMaxThrust;
    private float mHoverThrustControlFactor = .56f;
    private float mHoverThrust = mHoverThrustControlFactor * absMaxThrust;

    public PacketControl(){
        mThrust = 0;
        mRoll = 0;
        mPitch = 0;
        mYaw = 0;
        mHistory = new ArrayList<>();
        mThrust = mLiftOffThrust;
    }

    public PacketControl(Crazyflie cf, Controls control){
        mThrust = 0;
        mRoll = 0;
        mPitch = 0;
        mYaw = 0;
        mHistory = new ArrayList<>();
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

    public CommanderPacket getCurrentPacket(){
        CommanderPacket cp = new CommanderPacket(mRoll, mPitch, mYaw, (char)((int)(mThrust)), mControls.isXmode());
        ArrayList<Object> newEntry = new ArrayList<>();
        newEntry.add(new Float(mRoll));
        newEntry.add(new Float(mPitch));
        newEntry.add(new Float(mYaw));
        newEntry.add(new Character((char)((int)(mThrust))));
        if (mHistory.size() < 10) {
            mHistory.add(newEntry);
        }
        else {
            mHistory.remove(0);
            mHistory.add(newEntry);
        }
        return cp;
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

            mPacketSender = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mCrazyFlie != null) {
                        mCrazyFlie.sendPacket(new HoverPacket(0.0f,0.0f,0.0f,0.4f));
                        //mCrazyFlie.sendPacket(new CommanderPacket(mRoll, mPitch, mYaw, (char) (mThrust), mControls.isXmode()));
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            break;
                        }

                    }
                }
            });
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

    private boolean checkLastCommand(){
        ArrayList<Object> lastEntry = mHistory.get(mHistory.size()-1);
        // iterate through the 4 values: roll,yaw,pitch.thrust
        boolean zeroed = true;
        // if all 4 values are zero then we know we are in a landed state
        for(int i = 0; i < 4; i++) {
            zeroed = zeroed && (lastEntry.get(i) == new Integer(0));
        }
        return zeroed;
    }

    public void calibrateDriftXY(float deltaX, float deltaY){
        if (mState != STATE.CALIBRATING)
            return;
        boolean calibrated = true;
        if (deltaX > Math.abs(mDeltaXThresh)) {
            calibrated = false;
            if (deltaX > 0) {
                setRoll(-1);
            } else {
                setRoll(1);
            }
        } else {
            setRoll(0);
        }
        if (deltaY > Math.abs(mDeltaYThresh)) {
            calibrated = false;
            if (deltaY > 0) {
                setPitch(-1);
            } else {
                setPitch(1);
            }
        } else {
            setPitch(0);
        }
        if (calibrated){
                mState = STATE.HOVER;
        }
    }

    public void incrementDeltaXThresh(int dX) {
        mDeltaXThresh += dX;
        mLogger.debug("DeltaXThresh Set to: " + mDeltaXThresh);
    }

    public void incrementDeltaYThresh(int dY){
        mDeltaYThresh += dY;
        mLogger.debug("DeltaYThresh Set to: " + mDeltaYThresh);
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
