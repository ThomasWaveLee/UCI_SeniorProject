package lightingtheway;

import java.util.ArrayList;

import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;


public class PacketControl {
    private int thrust;
    private int roll;
    private int pitch;
    private int yaw;
    private float maxThrust;
    private float minThrust;
    private boolean isXMode;

    // Potentially want to look at previous commmandPackets sent.
    // Hold up to historyLength of past packets
    private static final int historyLength = 10;
    private ArrayList<CommanderPacket> history;

    public final static int absMaxThrust = 65535;
    private final static float lightOffThrust = .60f;

    public PacketControl(float maxThrust,float minThrust, boolean mode){
        thrust = 0;
        roll = 0;
        pitch = 0;
        yaw = 0;
        this.maxThrust = maxThrust;
        this.minThrust = minThrust;
        isXMode = mode;
        history = new ArrayList<>();
    }

    public CommanderPacket getCurrentPacket(){
        CommanderPacket cp = new CommanderPacket(roll, pitch, yaw, (char)((int)(thrust)), isXMode);
        if (history.size() < 10) {
            history.add(cp);
        }
        else {
            history.remove(0);
            history.add(cp);
        }
        return cp;
    }

    public void incrementAll(int roll,int pitch, int yaw, float thrust){
        this.roll += roll;
        this.pitch += pitch;
        this.yaw += yaw;
        this.thrust += thrust;
    }

    public void incrementRoll(int roll){
        this.roll += roll;
    }

    public void incrementPitch(int pitch){
        this.pitch += pitch;
    }

    public void incrementYaw(int yaw){
        this.yaw += yaw;
    }

    public void incrementThrust(int thrust){
        this.thrust += thrust;
    }


    /**  Setter functions **/
    public void setMaxThrust(float thrust){
        maxThrust = thrust;
    }

    public void setMinThrust(float thrust){
        minThrust = thrust;
    }

    public void setXMode(boolean mode){
        isXMode = mode;
    }
}
