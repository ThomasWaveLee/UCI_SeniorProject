package se.bitcraze.crazyflie.lib.crtp;

import java.nio.ByteBuffer;

/**
 * Created by Thomas Lee on 1/21/2018.
 */

public class HoverPacket extends CrtpPacket {
    private final static short commanderGenericHoverType = 5;
    private float mVx;
    private float mVy;
    private float mYawRate;
    private float mZDistance;

    public HoverPacket(float vx, float vy, float yawRate, float zDistance) {
        super(0, CrtpPort.COMMANDER_GENERIC);
        mVx = vx;
        mVy = vy;
        mYawRate = yawRate;
        mZDistance = zDistance;
    }

    @Override
    protected void serializeData(ByteBuffer buffer){
        buffer.putChar((char)commanderGenericHoverType);
        buffer.putFloat(mVx);
        buffer.putFloat(mVy);
        buffer.putFloat(mYawRate);
        buffer.putFloat(mZDistance);
    }

    @Override
    protected int getDataByteCount(){
        return 4 * 4 + 1 * 2; // 4 float: 4 bytes ea, 1 char: 2 bytes: 18
    }

    @Override
    public String toString(){
        return "HoverPacket: vx:" + mVx + " vy:" + mVy + " yawRate:" + mYawRate + " zDistance:" + mZDistance;
    }
}
