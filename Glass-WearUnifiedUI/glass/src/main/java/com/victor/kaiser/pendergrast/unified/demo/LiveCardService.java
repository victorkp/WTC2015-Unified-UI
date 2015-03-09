package com.victor.kaiser.pendergrast.unified.demo;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.glass.timeline.LiveCard;
import com.victor.kaiser.pendergrast.unified.shared.SerialComm;

import app.akexorcist.bluetoothspp.BluetoothSPP;
import app.akexorcist.bluetoothspp.BluetoothState;

/**
 * A simple LiveCard service;
 * Functions primarily to setup BluetoothSPP
 * and handle those messages
 */
public class LiveCardService extends Service implements BluetoothSPP.BluetoothConnectionListener,
								BluetoothSPP.AutoConnectionListener, BluetoothSPP.OnDataReceivedListener,
								BluetoothSPP.BluetoothStateListener {

	public static final String TAG = "LiveCardService";
	public static final String CARD_TAG = "unified_demo_card";

	public LiveCardService() {

	}

	/**
	 * A listener that receives updates on
	 * what is happening in the orderin process
	 */
	public interface SandwichListener {

		/**
		 * The user selected a bread from
		 * the list of breads in SandwichOptions.BREAD_LIST
		 */
		public void onBreadPicked(String bread);

		/**
		 * The user selected a cheese from
		 * the list of cheeses in SandwichOptions.CHEESE_LIST
		 */
		public void onCheesePicked(String cheese);

		/**
		 * Order was cancelled
		 */
		public void onOrderCanceled();
	}

	public class LiveCardBinder extends Binder {
		public void setOnSandwichListener(SandwichListener listener) {
			mSandwichListener = listener;
		}

		/**
		 * Request connected Android Wear device
		 * to display a bread selection list;
		 * Response is sent to SandwichListener
		 */
		public void requestBreadSelection() {
			Log.d(TAG, "Requesting bread selection");
			if(mHandsetConnected) {
				mBluetoothSPP.send(SerialComm.SHOW_BREAD_LIST);
				Log.d(TAG, "Sent bread selection");
			} else {
				Log.d(TAG, "Handset not connected");
			}
		}

		/**
		 * Request connected Android Wear device
		 * to display a cheese selection list;
		 * Response is sent to SandwichListener
		 */
		public void requestCheeseSelection() {
			if(mHandsetConnected) {
				mBluetoothSPP.send(SerialComm.SHOW_CHEESE_LIST);
			}
		}

		/**
		 * Request connected Android Wear device
		 * to display an "Order is ready" alert
		 */
		public void requestOrderReadyAlert() {
			if(mHandsetConnected) {
				mBluetoothSPP.send(SerialComm.ORDER_READY);
			}
		}
	}

	private LiveCardBinder mBinder = new LiveCardBinder();

	private LiveCard mLiveCard;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSPP mBluetoothSPP;

	private boolean mHandsetConnected = false;
	
	private SandwichListener mSandwichListener;

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}


	@Override
	public void onCreate() {
		// Setup BluetoothSPP to allow connection acceptance
		mBluetoothSPP = new BluetoothSPP(this);
		mBluetoothSPP.setBluetoothConnectionListener(this);
		mBluetoothSPP.setOnDataReceivedListener(this);
		mBluetoothSPP.setAutoConnectionListener(this);
		mBluetoothSPP.setBluetoothStateListener(this);
		mBluetoothSPP.setupService();
		mBluetoothSPP.startService(BluetoothState.DEVICE_ANDROID);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Show LiveCard
		if(mLiveCard == null) {
			// LiveCard was not yet published, so create and publish one
			mLiveCard = new LiveCard(this, CARD_TAG);
			RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.live_card);
			mLiveCard.setViews(views);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0,
					new Intent(this, MenuActivity.class), 0));
			mLiveCard.publish(LiveCard.PublishMode.REVEAL);
		} else {
			// Card is already published, so unpublish
			// and republish to bring it to forefront again
			mLiveCard.unpublish();
			mLiveCard.publish(LiveCard.PublishMode.REVEAL);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Unpublish LiveCard
		if(mLiveCard != null && mLiveCard.isPublished()) {
			mLiveCard.unpublish();
			mLiveCard = null;
		}

		mBluetoothSPP.stopService();

		super.onDestroy();
	}


	// BluetoothSPP's callbacks
	@Override
	public void onDeviceDisconnected() {
		Log.d(TAG, "Handset disconnected");
		mHandsetConnected = false;
	}

	@Override
	public void onDeviceConnectionFailed() {
		Log.e(TAG, "Handset connection failed");
		mHandsetConnected = false;
	}

	@Override
	public void onDeviceConnected(String name, String address) {
		Log.d(TAG, "Handset \"" + name + "\" connected");
		mHandsetConnected = true;
	}

	@Override
	public void onNewConnection(String device, String address) {
		Log.i(TAG, "New Connection: " + device);
		mHandsetConnected = true;
	}

	@Override
	public void onServiceStateChanged(int state) {
		Log.d(TAG, "Bluetooth State: " + state);
		if(state == BluetoothState.STATE_CONNECTED) {
			mHandsetConnected = true;
		} else {
			mHandsetConnected = false;
		}
	}

	@Override
	public void onAutoConnectionStarted() {
		Log.d(TAG, "Auto-connection started");
	}

	@Override
	public void onDataReceived(byte[] bytes, String data) {
		// Do nothing with the data for now
		Log.d(TAG, "Received: \"" + data + "\"");

		if(data.equals(SerialComm.ORDER_CANCELED)) {
			// Tell sandwich listener to stop
		} else if (data.equals(SerialComm.BREAD_PICKED)) {
			// Inform sandwich listener 
			if(mSandwichListener != null) { 
				mSandwichListener.onBreadPicked(data.substring(SerialComm.BREAD_PICKED.length()));
			}
		} else if (data.equals(SerialComm.CHEESE_PICKED)) {
			// Inform sandwich listener 
			if(mSandwichListener != null) { 
				mSandwichListener.onCheesePicked(data.substring(SerialComm.CHEESE_PICKED.length()));
			}
		} else {
			// This shouldn't happen...
			Log.d(TAG, "Unrecognized message: " + data);
		}

	}
}
