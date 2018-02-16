package se.bitcraze.crazyfliecontrol2;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import lightingtheway.PacketControl;

public class ManualActivity extends Activity {
    private PacketControl mPacketControl;

    private ImageButton mDeltaXUpButton;
    private ImageButton mDeltaXDownButton;
    private ImageButton mDeltaYUpButton;
    private ImageButton mDeltaYDownButton;
    private ImageButton mLiftOffThreshUpButton;
    private ImageButton mLiftOffThreshDownButton;
    private ImageButton mHoverThreshUpButton;
    private ImageButton mHoverThreshDownButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        // testing tweak buttons
        mDeltaXUpButton = (ImageButton) findViewById(R.id.button_deltaXUp);
        mDeltaXDownButton = (ImageButton) findViewById(R.id.button_deltaXDown);
        mDeltaYUpButton = (ImageButton) findViewById(R.id.button_deltaYUp);
        mDeltaYDownButton = (ImageButton) findViewById(R.id.button_deltaYDown);
        mLiftOffThreshUpButton = (ImageButton) findViewById(R.id.button_LiftOffUp);
        mLiftOffThreshDownButton = (ImageButton) findViewById(R.id.button_LiftOffDown);
        mHoverThreshUpButton = (ImageButton) findViewById(R.id.button_HoverUp);
        mHoverThreshDownButton = (ImageButton) findViewById(R.id.button_HoverDown);
        initializeTestingButtons();

        mPacketControl = (PacketControl) getIntent().getSerializableExtra("PacketControl");
    }

    public void initializeTestingButtons(){

        // testing tweak buttons
        mDeltaXUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // mPacketControl.incrementVx(0.1f);
                mPacketControl.goRight(.5f,.1f);
            }
        });
        mDeltaXDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mPacketControl.incrementVx(-0.1f);
                mPacketControl.goLeft(.5f,.1f);
            }
        });
        mDeltaYUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mPacketControl.incrementVy(0.1f);
                mPacketControl.goForward(.5f,.1f);
            }
        });
        mDeltaYDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mPacketControl.incrementVy(-0.1f);
                mPacketControl.goBack(.5f,.1f);
            }
        });
        mLiftOffThreshUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketControl.incrementZDistance(0.1f);
            }
        });
        mLiftOffThreshDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketControl.incrementZDistance(-0.1f);
            }
        });
        mHoverThreshUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketControl.incrementYawRate(1.0f);
            }
        });
        mHoverThreshDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPacketControl.incrementYawRate(-1.0f);
            }
        });

        mDeltaXUpButton.setEnabled(true);
        mDeltaXDownButton.setEnabled(true);
        mDeltaYUpButton.setEnabled(true);
        mDeltaYDownButton.setEnabled(true);
        mLiftOffThreshUpButton.setEnabled(true);
        mLiftOffThreshDownButton.setEnabled(true);
        mHoverThreshUpButton.setEnabled(true);
        mHoverThreshDownButton.setEnabled(true);
    }
}
