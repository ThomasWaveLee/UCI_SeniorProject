/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyflie.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.Toast;

import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyfliecontrol2.MainActivity;

@SuppressLint("NewApi")
public class BleLink extends CrtpDriver {

	final Logger mLogger = LoggerFactory.getLogger("BLELink");

	// Set to -40 to connect only to close-by Crazyflie
	private static final int rssiThreshold = -100;

	private final BlockingQueue<byte[]> mInQueue;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice;
	private BluetoothGattCharacteristic mLedChar;
	private List<BluetoothGattCharacteristic> mLedsChars;
	private BluetoothGatt mGatt;
	private BluetoothGatt mGattDown;
	private BluetoothGattCharacteristic mCrtpChar;
	private BluetoothGattCharacteristic mCrtpUpChar;
	private BluetoothGattCharacteristic mCrtpDownChar;
	private Timer mScannTimer;

	private static final String CF_DEVICE_NAME = "Crazyflie";
	private static final String CF_LOADER_DEVICE_NAME = "Crazyflie Loader";

	private static UUID CF_SERVICE = UUID.fromString("00000201-1C7F-4F9E-947B-43B7C00A9A08");
	private static UUID CRTP = UUID.fromString("00000202-1C7F-4F9E-947B-43B7C00A9A08");
	private static UUID CRTPUP = UUID.fromString("00000203-1C7F-4F9E-947B-43B7C00A9A08");
	private static UUID CRTPDOWN = UUID.fromString("00000204-1C7F-4F9E-947B-43B7C00A9A08");

	private final static int REQUEST_ENABLE_BT = 1;
	protected boolean mWritten = true;
	private Activity mContext;
	private boolean mWriteWithAnswer;
	protected boolean mConnected;
	private boolean RSSI_status;
	private boolean connected_once = false;
	private double RSSI_Value_Center = 0;
	private int RSSI_Value_Num = 0;
	private double RSSI_range = 0;
	private ArrayList<Integer> RSSI_field;
	private double Global_RSSI_Center = 0;
	private int Global_RSSI_Value_Num = 0;
	private ArrayList<Double> Global_RSSI_range_field;
	private double Global_RSSI_range = 0;
	private ArrayList<Integer> Global_RSSI_field;
	private int iteration_number = 0;
	private int checks = 0;
	private Timer readRssiTask;
	private double safe_RSSI_threshold = 0;

	protected enum State {IDLE, CONNECTING, CONNECTED};
	protected State state = State.IDLE;

	public BleLink(Activity ctx, boolean writeWithAnswer) {
		mContext = ctx;
		mWriteWithAnswer = writeWithAnswer;
		mInQueue = new LinkedBlockingQueue<byte[]>();
		RSSI_field = new ArrayList<>();
		Global_RSSI_field = new ArrayList<>();
		Global_RSSI_range_field = new ArrayList<>();
	}

	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			final int status_check = newState;
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				connected_once = true;
				mLogger.debug("onConnectionStateChange: STATE_CONNECTED");
				gatt.discoverServices();
				mGatt = gatt;
				if(readRssiTask != null) {
					readRssiTask.cancel();
					readRssiTask = null;
				}
				readRssiTask = new Timer();
				TimerTask thread_timer = new TimerTask(){
					public void run(){
						RSSI_status = mGatt.readRemoteRssi();
						/*if ((status_check == BluetoothProfile.STATE_DISCONNECTED)){
							readRssiTask.cancel();
						}*/
						/*if((RSSI_status != true)&&(connected_once)){
							//If the Array is more than 40, we can trust that a new add of the RSSI temporary storage will provide data.
							if(RSSI_Value_Num > 15) {
								Global_RSSI_field.add((int)RSSI_Value_Center);
								Global_RSSI_range_field.add(RSSI_range);
								Global_RSSI_Value_Num++;
								double Global_RSSI_newCenter = 0;
								for (int k = 0; k < Global_RSSI_Value_Num; k++) {
									Global_RSSI_newCenter += Global_RSSI_field.get(k);
								}
								Global_RSSI_Center = Global_RSSI_newCenter / Global_RSSI_Value_Num;
								double Global_new_RSSI_values = 0;
								for (int j = 0; j < Global_RSSI_Value_Num; j++){
									Global_new_RSSI_values += Global_RSSI_range_field.get(j);
								}
								Global_RSSI_range = Global_new_RSSI_values / RSSI_Value_Num;
							}
							//Clears the value of the RSSI Temporary Storage to make sure that new reads will be clean
							RSSI_Value_Center = 0;
							RSSI_Value_Num = 0;
							RSSI_range = 0;
							RSSI_field = null;
							RSSI_field = new ArrayList<>();
						}*/
					}
				};
				readRssiTask.schedule(thread_timer, 2000, 1000);
				//Creating a method to invoke a RSSI call to the onReadRemoteRSSI value;

			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				mLogger.debug("onConnectionStateChange: STATE_DISCONNECTED");
				// This is necessary to handle a disconnect on the copter side
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mConnected = false;
				state = State.IDLE;
				// it should actually be notifyConnectionLost, but there is
				// no difference between a deliberate disconnect and a lost connection
				notifyDisconnected();
			} else {
				mLogger.debug("onConnectionStateChange: else: " + newState);
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				mConnected = false;
				state = State.IDLE;
				notifyConnectionLost("BLE connection lost");
				//Converts Global RSSI to local RSSI if the connection is broken as a new RSSI threshold and state will be chosen
				//Helps Normalized the value by using the RSSI value of subsequent data
				if (connected_once) {
					RSSI_Value_Center = Global_RSSI_Center;
					RSSI_Value_Num = 1;
					RSSI_range = Global_RSSI_range;
					RSSI_field = null;
					RSSI_field = new ArrayList<>();
					RSSI_field.add((int)Global_RSSI_Center);
					Global_RSSI_Center = 0;
					Global_RSSI_Value_Num = 0;
					Global_RSSI_range = 0;
					Global_RSSI_field = null;
					Global_RSSI_field = new ArrayList<>();
					Global_RSSI_range_field = null;
					Global_RSSI_range_field = new ArrayList<>();
					connected_once = false;
					Toast.makeText(mContext , "Bluetooth Connection Lost, Please move faster", Toast.LENGTH_SHORT).show();
				}
				else if((Global_RSSI_range == 0) && (RSSI_range ==0)){
					Toast.makeText(mContext , "Bluetooth Connection Unreliable.", Toast.LENGTH_SHORT).show();
				}
			}
		}
		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
			iteration_number++;
			if (iteration_number < 40){
				safe_RSSI_threshold += rssi;
			}
			else if(iteration_number == 40){
				safe_RSSI_threshold = (safe_RSSI_threshold + 10)/40;
			}
			else {
				super.onReadRemoteRssi(gatt, rssi, status);
				RSSI_field.add(rssi);
				RSSI_Value_Num++;
				double RSSI_new_center = 0;
				if (RSSI_Value_Num > 15) {
					double threshold_upper = 0;
					double threshold_lower = 0;
					if (RSSI_range > 0) {
						threshold_upper = RSSI_Value_Center - 0.5 * RSSI_range;
						threshold_lower = RSSI_Value_Center + 0.5 * RSSI_range;
					} else {
						threshold_lower = RSSI_Value_Center - 0.5 * RSSI_range;
						threshold_upper = RSSI_range + 0.5 * RSSI_Value_Center;
					}
					if ((rssi < threshold_lower)) {
						//Checks for the lower bound in which the user will move away from the drone
						checks++;
						/*mContext.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(mContext, "Bluetooth Connection Weak1, Please move faster", Toast.LENGTH_SHORT).show();
							}
						});*/
					} else if ((rssi > threshold_upper) && (RSSI_Value_Num > 10)) {
						Global_RSSI_field.add((int) RSSI_Value_Center);
						Global_RSSI_range_field.add(RSSI_range);
						Global_RSSI_Value_Num++;
						double Global_RSSI_newCenter = 0;
						for (int k = 0; k < Global_RSSI_Value_Num; k++) {
							Global_RSSI_newCenter += Global_RSSI_field.get(k);
						}
						Global_RSSI_Center = Global_RSSI_newCenter / Global_RSSI_Value_Num;
						double Global_new_RSSI_values = 0;
						for (int j = 0; j < Global_RSSI_Value_Num; j++) {
							Global_new_RSSI_values += Global_RSSI_range_field.get(j);
						}
						Global_RSSI_range = Global_new_RSSI_values / RSSI_Value_Num;
						RSSI_Value_Center = 0;
						RSSI_Value_Num = 0;
						RSSI_range = 0;
						RSSI_field = null;
						RSSI_field = new ArrayList<>();
						checks--;
					} else {
						checks--;
					}
				}
				if (RSSI_Value_Num > 0) {
					for (int i = 0; i < RSSI_Value_Num; i++) {
						RSSI_new_center += RSSI_field.get(i);
					}
					RSSI_Value_Center = RSSI_new_center / RSSI_Value_Num;
					for (int j = 0; j < RSSI_Value_Num; j++) {
						RSSI_range += Math.abs((RSSI_Value_Center - RSSI_field.get(j)) * (RSSI_Value_Center - RSSI_field.get(j)));
					}
					RSSI_range = Math.sqrt(RSSI_range) / RSSI_Value_Num;
				}
				if (RSSI_Value_Num > 20) {
					Global_RSSI_field.add((int) RSSI_Value_Center);
					Global_RSSI_range_field.add(RSSI_range);
					Global_RSSI_Value_Num++;
					double Global_RSSI_newCenter = 0;
					for (int k = 0; k < Global_RSSI_Value_Num; k++) {
						Global_RSSI_newCenter += Global_RSSI_field.get(k);
					}
					Global_RSSI_Center = Global_RSSI_newCenter / Global_RSSI_Value_Num;
					double Global_new_RSSI_values = 0;
					for (int j = 0; j < Global_RSSI_Value_Num; j++) {
						Global_new_RSSI_values += Global_RSSI_range_field.get(j);
					}
					Global_RSSI_range = Global_new_RSSI_values / RSSI_Value_Num;
					RSSI_Value_Center = 0;
					RSSI_Value_Num = 0;
					RSSI_range = 0;
					RSSI_field = null;
					RSSI_field = new ArrayList<>();
				}

			/*else{
				mContext.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext , "Bluetooth Connection Lost, Please move faster", Toast.LENGTH_SHORT).show();
					}
				});

			}*/
				if ((iteration_number % 2 == 0)&&(iteration_number > 40)) {
					if ((checks > 2) && (Global_RSSI_Value_Num == 0)) {
						mContext.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(mContext, "Bluetooth Connection Weak2, Please move faster", Toast.LENGTH_SHORT).show();
							}
						});
						checks = 0;
					} else if (Global_RSSI_Value_Num > 2) {
						double temp_range = Math.abs(Global_RSSI_range);
						if (temp_range > 30) {
							temp_range = 20;
						}
						double global_thres_lower = Global_RSSI_Center - temp_range;
						if (rssi < global_thres_lower) {
							mContext.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(mContext, "Bluetooth Connection Weak, Please move faster", Toast.LENGTH_SHORT).show();
								}
							});
						}
					}
					if(((safe_RSSI_threshold - 30) > RSSI_Value_Center)&&(RSSI_Value_Num > 5)){
						mContext.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(mContext, "Bluetooth Connection Weak, Please move faster", Toast.LENGTH_SHORT).show();
								tooFar = true;
							}
						});
					}
					else{
						tooFar = false;
					}

				}
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			if (status != BluetoothGatt.GATT_SUCCESS) {
				gatt.disconnect();
			} else {
				BluetoothGattService cfService = gatt.getService(CF_SERVICE);

				mCrtpChar = cfService.getCharacteristic(CRTP);
				//mCrtpUpChar = cfService.getCharacteristic(CRTPUP);
				mCrtpDownChar = cfService.getCharacteristic(CRTPDOWN);

				gatt.setCharacteristicNotification(mCrtpChar, true);
				//gatt.setCharacteristicNotification(mCrtpUpChar, true);
				gatt.setCharacteristicNotification(mCrtpDownChar, true);


				// add descriptor for CTRP
				BluetoothGattDescriptor descriptor = mCrtpDownChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"));
				descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				mLogger.debug("1st Descriptor: " + gatt.writeDescriptor(descriptor));

				// add descriptor for CTRPDOWN
				//descriptor = mCrtpDownChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"));
				//descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
				//mLogger.debug("2nd Descriptor: " + gatt.writeDescriptor(descriptor));

				mLogger.debug( "Connected!");

				mConnected = true;
				mWritten = false;

				state = State.CONNECTED;
				notifyConnected();
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicWrite(gatt, characteristic, status);
			//mLogger.debug("On write called for char: " + characteristic.getUuid().toString());
			mWritten  = true;
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			//mLogger.debug("On write called for descriptor: " + descriptor.getUuid().toString());
			mWritten = true;
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			super.onCharacteristicRead(gatt, characteristic, status);
			/*
			mLogger.debug("On read call for characteristic: " + characteristic.getUuid().toString());
			byte[] byteCode = characteristic.getValue();
			mLogger.debug("Characteristic Values:\n");
			for (byte i: byteCode) {
				mLogger.debug(Integer.toHexString(i));
			}
			*/
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			//mLogger.debug("On changed call for characteristic: " + characteristic.getUuid().toString());
			try {
				byte[] data = characteristic.getValue();
				if (data != null && data.length > 0) {
					mInQueue.put(data);
				}
			} catch(InterruptedException e) {
				mLogger.error("InterruptedException: " + e.getMessage());
				return;
			}
			/* // Debug Print for whats being received
			byte[] byteCode = characteristic.getValue();
			mLogger.debug("Bytes Recieved: " + byteCode.length);
			String binaryValue = "";
			for (byte i: byteCode) {
                binaryValue += Integer.toBinaryString(i);
			}
            mLogger.debug("Characteristic Value: " + binaryValue);
			*/
		}
	};

	private LeScanCallback mLeScanCallback = new LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] anounce) {
			if (device != null && device.getName() != null) {
				mLogger.debug("Scanned device \"" + device.getName() + "\" RSSI: " + rssi);

				if (device.getName().equals(CF_DEVICE_NAME) && rssi>rssiThreshold) {
					mBluetoothAdapter.stopLeScan(this);
					if (mScannTimer != null) {
						mScannTimer.cancel();
						mScannTimer = null;
					}
					state = State.CONNECTING;
					mDevice = device;
					mContext.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mDevice.connectGatt(mContext, false, mGattCallback);
						}
					});
				}
			}
		}
	};

	@Override
	public void connect(ConnectionData connectionData) {
		this.mConnectionData = connectionData;
		// TODO: connectionData is unused until BLE can address specific quadcopter
		if (state != State.IDLE) {
			throw new IllegalArgumentException("Connection already started");
		}

		final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();

		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mContext.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			throw new IllegalArgumentException("Bluetooth needs to be started");
		}

		mBluetoothAdapter.stopLeScan(mLeScanCallback);
		mBluetoothAdapter.startLeScan(mLeScanCallback);
		if (mScannTimer != null) {
			mScannTimer.cancel();
		}
		mScannTimer = new Timer();
		mScannTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				mBluetoothAdapter.stopLeScan(mLeScanCallback);
				state = State.IDLE;
				notifyConnectionFailed("BLE connection timeout");
			}
		}, 5000);

		state = State.CONNECTING;
		notifyConnectionRequested();
	}

	@Override
	public void disconnect() {
		mContext.runOnUiThread(new Runnable() {
			public void run() {
				if(mConnected) {
					mGatt.disconnect();
					//delay close command to fix potential NPE
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							mGatt.close();
							mGatt = null;
						}
					}, 100);
					mConnected = false;
					mBluetoothAdapter.stopLeScan(mLeScanCallback);
					if (mScannTimer != null) {
						mScannTimer.cancel();
						mScannTimer = null;
					}
					state = State.IDLE;
					notifyDisconnected();
				}
			}
		});
	}

	@Override
	public boolean isConnected() {
		return state == State.CONNECTED;
	}

	int ctr = 0;
	@Override
	public void sendPacket(CrtpPacket packet) {

		// FIXME: Skipping half of the commander packets to avoid queuing up packets on slow BLE
		if ((mWriteWithAnswer == false) && ((ctr++)%2 == 0)) {
			return;
		}

		class SendBlePacket implements Runnable {
			CrtpPacket pk;

			public SendBlePacket(CrtpPacket pk) {
				this.pk = pk;
			}

			public void run() {
				if(mConnected && mWritten) {
					if (mWriteWithAnswer) {
						mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
						mWritten = false;
					} else {
						mCrtpChar.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
						mWritten = true;
					}
					mCrtpChar.setValue(pk.toByteArray());
					// Debug prints to see whats being sent
					/*
					String pkt = "";
					for(byte i: pk.toByteArray()){
						pkt += Integer.toBinaryString(i);
					}
					mLogger.debug("Sent packet: " + pkt);
					*/
					mGatt.writeCharacteristic(mCrtpChar);
				}
			}
		}
		mContext.runOnUiThread(new SendBlePacket(packet));
	}

	@Override
	public CrtpPacket receivePacket(int wait) {
		try {
			if(isConnected()){
				// grab 1st packet
				byte[] pkt = mInQueue.poll((long) wait, TimeUnit.SECONDS);
				if (pkt == null) {
					return null;
				}
				byte BLEheader = pkt[0];
				int headerValue = Byte.valueOf(BLEheader).intValue();
				// DECODING
				// BLE packet header has form:
				// 7 = start
				// 5-6 = pid
				// 0-4 = length
				// shift by 5 to get bits 5-7
				int pid = headerValue >> 5;
				// mask with 3 to get bits 5-6
				pid = pid & 0b011;
				// m
				int length = headerValue & 0b00011111;
				// BLE has length = actualLength-1
				length++;

				byte[] crtpPkt = new byte[32];
				// copy out crtp packets values from pkt1
				for(int i = 1; i < pkt.length; i++){
					crtpPkt[i-1] = pkt[i];
				}
				if (length > 19){
					// 2nd packet
					byte[] pkt2;
					pkt2 = mInQueue.poll((long) wait, TimeUnit.SECONDS);
					if(pkt2 == null){
						return null;
					}
					byte BLEheader2 = pkt2[0];
					int headerValue2 = Byte.valueOf(BLEheader2).intValue();
					int pid2 = headerValue2 >> 5;
					pid2 = pid2 & 0b11;
					if (pid2 != pid){
						return null;
					}
					// copy out crtp packets values from pkt
					for(int i = 1; i < pkt2.length; i++){
						crtpPkt[pkt.length+i-1] = pkt2[i];
					}
				}
				return new CrtpPacket(crtpPkt);
			}
		} catch (InterruptedException e) {
			mLogger.error("InterruptedException: " + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean scanSelected(ConnectionData connectionData, byte[] packet) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startSendReceiveThread() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopSendReceiveThread() {
		// TODO Auto-generated method stub
	}
}
