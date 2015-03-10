package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.MessageApi;
import com.victor.kaiser.pendergrast.unified.shared.SerialComm;
import com.victor.kaiser.pendergrast.unified.shared.WearableComm;

import java.util.ArrayList;

import app.akexorcist.bluetoothspp.BluetoothSPP;
import app.akexorcist.bluetoothspp.BluetoothState;

public class BridgeService extends Service implements GoogleApiClient.ConnectionCallbacks,
							GoogleApiClient.OnConnectionFailedListener, BluetoothSPP.AutoConnectionListener, BluetoothSPP.BluetoothConnectionListener,
							BluetoothSPP.OnDataReceivedListener, BluetoothSPP.BluetoothStateListener, NodeApi.NodeListener, MessageApi.MessageListener {

	private static final String TAG = "BridgeService";

	private static boolean mIsRunning = false;

	public static boolean isRunning() {
		return mIsRunning;
	}

	public interface DeviceConnectionListener {
		public void onGlassConnected();
		public void onGlassDisconnected();
		public void onWearConnected();
		public void onWearDisconnected();
	}

	public class BridgeBinder extends Binder {
		public void setListener(DeviceConnectionListener listener) {
			mListener = listener;
			onListenerSet();
		}

		public boolean isGlassConnected() {
			return mGlassConnected;
		}

		public boolean isWearableConnected() {
			return mWearableConnected;
		}

		public void setGlass(Intent data) {
			mBluetoothSerial.connect(data);
		}

		public void enableBluetooth() {
			mBluetoothSerial.setupService();
			mBluetoothSerial.startService(BluetoothState.DEVICE_ANDROID);
		}
	}

	private DeviceConnectionListener mListener;
	private BridgeBinder mBinder = new BridgeBinder();

	private GoogleApiClient mGoogleClient;
	private BluetoothSPP mBluetoothSerial;

	ArrayList<Node> mNodes;

	private boolean mWearableConnected = false;
	private boolean mGlassConnected = false;

	// Our PutRequests must be unique, or they won't be delivered
	private int mAlertCounter = 0;


	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNodes = new ArrayList<Node>();

		// Setup GoogleApiClient for Android Wear messaging
		mGoogleClient = new GoogleApiClient.Builder(this)
						.addApi(Wearable.API)
						.addConnectionCallbacks(this)
						.addOnConnectionFailedListener(this)
						.build();

		mBluetoothSerial = new BluetoothSPP(this);
		mBluetoothSerial.setBluetoothConnectionListener(this);
		mBluetoothSerial.setOnDataReceivedListener(this);
		mBluetoothSerial.setAutoConnectionListener(this);
		mBluetoothSerial.setBluetoothStateListener(this);
		mBluetoothSerial.setupService();
		mBluetoothSerial.startService(BluetoothState.DEVICE_ANDROID);
		mBluetoothSerial.autoConnect("Glass");

		Wearable.MessageApi.addListener(mGoogleClient, this);
	}

	@Override
	public int onStartCommand(Intent bundle, int flags, int startId) {
		mIsRunning = true;

		if(!mBluetoothSerial.isBluetoothEnabled()) {
			Log.d(TAG, "Bluetooth starting auto connect");
			mBluetoothSerial.enable();
			mBluetoothSerial.setupService();
			mBluetoothSerial.startService(BluetoothState.DEVICE_ANDROID);
			mBluetoothSerial.autoConnect("Glass");
		} else {
			// Try to turn on bluetooth
			Log.d(TAG, "Trying to turn on bluetooth");
			if(!mBluetoothSerial.isServiceAvailable()) {
				mBluetoothSerial.enable();
				mBluetoothSerial.setupService();
				mBluetoothSerial.startService(BluetoothState.DEVICE_ANDROID);
			}
		}

		mGoogleClient.connect();

		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mIsRunning = false;

		mBluetoothSerial.stopAutoConnect();
		mBluetoothSerial.stopService();

		if(mGoogleClient.isConnected()) {
			mGoogleClient.disconnect();
		}
	}

	public void isWearableConnected() {
		if(mGoogleClient.isConnected()) {
			PendingResult<NodeApi.GetConnectedNodesResult> result = Wearable.NodeApi.getConnectedNodes(mGoogleClient);
			result.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
				@Override
				public void onResult(NodeApi.GetConnectedNodesResult result) {
					mWearableConnected = !result.getNodes().isEmpty();
					for(Node n : result.getNodes()) {
						mNodes.add(n);
					}

					Log.i(TAG, "Wearable connected: " + mWearableConnected);

					if(mListener != null) {
						if(mWearableConnected) {
							mListener.onWearConnected();
						} else {
							mListener.onWearDisconnected();
						}
					}
				}
			});

		}
	}

	private void sendWearableMessage(final String path) {
		if(mGoogleClient.isConnected() && mWearableConnected) {
			for(Node node : mNodes) {
				Log.i(TAG, "Sending message to " + node.getDisplayName());
				PendingResult<MessageApi.SendMessageResult> result = 
						Wearable.MessageApi.sendMessage(mGoogleClient, node.getId(), path, null);
				result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
					@Override
					public void onResult(MessageApi.SendMessageResult sendMessageResult) {
						Log.i(TAG, "Delivering message status: " + 
								sendMessageResult.getStatus().getStatusMessage());
					}
				});
			}
		}
	}

	/**
	 * Called when a new DeviceConnectionListener is set in the Binder
	 */
	private void onListenerSet() {
		if(mGlassConnected) {
			mListener.onGlassConnected();
		}
		if(mWearableConnected) {
			mListener.onWearConnected();
		}
	}

	// GoogleApiClient Callbacks
	@Override
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "GoogleApiClient connected");
		Wearable.NodeApi.addListener(mGoogleClient, this);
		Wearable.MessageApi.addListener(mGoogleClient, this);

		isWearableConnected();

		if(mListener != null) {
			if(mWearableConnected) {
				mListener.onWearConnected();
			} else {
				mListener.onWearDisconnected();
			}
		}
	}

	@Override
	public void onConnectionSuspended(int cause) {
		Log.d(TAG, "GoogleApiClient connection suspended: " + cause);
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(TAG, "GoogleApiClient connection failed: ");
	}

	// BluetoothSPP Callbacks
	@Override
	public void onDataReceived(byte[] data, String string) {
		Log.d(TAG, "onDataReceived: " + string);
		if(string.contains(SerialComm.ORDER_READY)) {
			// Show the sleep alert on wearable
			Log.d(TAG, "Received order ready, sending");
			sendWearableMessage(WearableComm.PATH_SHOW_SANDWICH_ALERT);
		} else if (string.contains(SerialComm.SHOW_BREAD_LIST)) {
			// Show the bread list on Android Wear
			Log.d(TAG, "Received show bread list, sending");
			sendWearableMessage(WearableComm.PATH_SHOW_BREAD_LIST);
		} else if (string.contains(SerialComm.SHOW_CHEESE_LIST)){
			// Show the cheese list on Android Wear
			Log.d(TAG, "Received show cheese list, sending");
			sendWearableMessage(WearableComm.PATH_SHOW_CHEESE_LIST);
		} else {
			Log.e(TAG, "Unknown command");
		}
	}

	@Override
	public void onNewConnection(String device, String address) {
		Log.i(TAG, "New Connection: " + device);
		mGlassConnected = false;
		if(mListener != null) {
			mListener.onGlassConnected();
		}
	}

	@Override
	public void onAutoConnectionStarted() {
		Log.d(TAG, "Auto-connection started");
	}

	@Override
	public void onDeviceConnected(String name, String address) {
		Log.i(TAG, "Device Connected :" + name);
		mGlassConnected = true;
		if(mListener != null) {
			mListener.onGlassConnected();
		}
	}

	@Override
	public void onServiceStateChanged(int state) {
		Log.d(TAG, "Bluetooth State: " + state);
		if(state == BluetoothState.STATE_CONNECTED) {
			Log.d(TAG, "Bluetooth state: connected");
			mGlassConnected = true;
			if(mListener != null ) {
				mListener.onGlassConnected();
			}
		} else {
			Log.d(TAG, "Bluetooth state: disconnected");
			mGlassConnected = false;
			mBluetoothSerial.autoConnect("Glass");
			if(mListener != null ) {
				mListener.onGlassDisconnected();
			}
		}
	}

	@Override
	public void onDeviceDisconnected() {
		Log.i(TAG, "Device disconnected");
		mGlassConnected = false;
	}

	@Override
	public void onDeviceConnectionFailed() {
		Log.i(TAG, "Device connection failed");
	}

	// NodeApi.NodeListener Callbacks
	@Override
	public void onPeerConnected(Node peer) {
		Log.i(TAG, "Peer connected: " + peer.getDisplayName());
		mWearableConnected = true;
		mNodes.add(peer);
	}

	@Override
	public void onPeerDisconnected(Node peer) {
		Log.i(TAG, "Peer disconnected: " + peer.getDisplayName());
		mNodes.remove(peer);
		mWearableConnected = !mNodes.isEmpty();
	}

	// MessageApi.MessageListener Callbacks
	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		String message = messageEvent.getPath();
		Log.i(TAG, "Message Received: " + message);

		if(message.contains(WearableComm.PATH_BREAD_PICKED)) {
			Log.d(TAG, "Sending bread pick to Glass");
			mBluetoothSerial.send(SerialComm.BREAD_PICKED + message.replace(WearableComm.PATH_BREAD_PICKED, ""));
		} else if (message.contains(WearableComm.PATH_CHEESE_PICKED)) {
			Log.d(TAG, "Sending cheese pick to Glass");
			mBluetoothSerial.send(SerialComm.CHEESE_PICKED + message.replace(WearableComm.PATH_CHEESE_PICKED, ""));
		} else {
			Log.i(TAG, "Unknown message");
		}
	}

}
