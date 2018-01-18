package lightingtheway;

import java.util.ArrayList;

import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyfliecontrol.controller.Controls;


public class PacketControl {
    private int mThrust;
    private int mRoll;
    private int mPitch;
    private int mYaw;
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
    private final static float liftOffThrust = .60f * absMaxThrust;

    public PacketControl(Crazyflie cf, Controls control){
        mThrust = 0;
        mRoll = 0;
        mPitch = 0;
        mYaw = 0;
        mCrazyFlie = cf;
        mMaxThrust = control.getMaxThrust();
        mMinThrust = control.getMinThrust();
        mHistory = new ArrayList<>();
        mControls = control;
    }

    public CommanderPacket getCurrentPacket(){
        CommanderPacket cp = new CommanderPacket(mRoll, mPitch, mYaw, (char)((int)(mThrust)), mControls.isXmode());
        ArrayList<Object> newEntry = new ArrayList<>();
        newEntry.add(new Integer(mRoll));
        newEntry.add(new Integer(mPitch));
        newEntry.add(new Integer(mYaw));
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

    public ArrayList<CommanderPacket> getLiftOffSequence() {
        ArrayList<CommanderPacket> sequence = new ArrayList<>();
        if(mHistory.isEmpty() || checkLastCommand()) {
            int step = 10;
            for(int i = 0;i < step; i++) {
                sequence.add(new CommanderPacket(mRoll, mPitch, mYaw, (char) (0), mControls.isXmode()));
            }
        }
        return sequence;
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

    public void incrementRoll(int roll){
        mRoll += roll;
    }

    public void incrementPitch(int pitch){
        mPitch += pitch;
    }

    public void incrementYaw(int yaw){
        mYaw += yaw;
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
}
