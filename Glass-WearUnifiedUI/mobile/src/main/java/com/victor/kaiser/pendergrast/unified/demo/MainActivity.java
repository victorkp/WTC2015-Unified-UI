package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.victor.kaiser.pendergrast.unified.demo.R;

import app.akexorcist.bluetoothspp.BluetoothState;
import app.akexorcist.bluetoothspp.DeviceList;


public class MainActivity extends ActionBarActivity implements BridgeService.DeviceConnectionListener {
	private static final String TAG = "MainActivity";

	private Toolbar mToolbar;

	private Switch mRunningSwitch;

	private TextView mStatus;
	private TextView mGlassStatus;
	private TextView mWearStatus;

	private boolean mGlassConnected;
	private boolean mWearConnected;

	private BridgeService.BridgeBinder mBinder;
	private boolean mBoundToService = false;

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder ibinder) {
			Log.i(TAG, "Bound to BridgeService");
			mBinder = (BridgeService.BridgeBinder) ibinder;
			mBinder.setListener(MainActivity.this);
			mBoundToService = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundToService = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);

		mStatus = (TextView) findViewById(R.id.text_status);
		mGlassStatus = (TextView) findViewById(R.id.text_glass_status);
		mWearStatus = (TextView) findViewById(R.id.text_wear_status);

		// Add a toggle switch to the Toolbar
		mRunningSwitch = new Switch(this);
		mRunningSwitch.setText(R.string.switch_run);
		mRunningSwitch.setPadding(getPixels(32), 0, 0, 0);
		Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT,
										Toolbar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT);
		mToolbar.addView(mRunningSwitch, layoutParams);

		mRunningSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				showView(isChecked);

				if(isChecked) {
					startService(new Intent(MainActivity.this, BridgeService.class));
					bindService(new Intent(MainActivity.this, BridgeService.class), mServiceConn, 0);
				} else {
					stopService(new Intent(MainActivity.this, BridgeService.class));
					unbindService(mServiceConn);
				}
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		showView(BridgeService.isRunning());

		if(BridgeService.isRunning()) {
			bindService(new Intent(this, BridgeService.class), mServiceConn, 0);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();

		if(mBoundToService) {
			unbindService(mServiceConn);
		}
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
			if(resultCode == Activity.RESULT_OK)
				if(mBoundToService) {
					mBinder.setGlass(data);
				}
		} else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
			if(resultCode == Activity.RESULT_OK) {
				mBinder.enableBluetooth();
			}
		}
	}


	private void showView(boolean isRunning) {
		mRunningSwitch.setChecked(isRunning);

		if(isRunning) {
			if(mGlassConnected && mWearConnected) {
				mStatus.setText(R.string.status_running);
				mStatus.setTextColor(getResources().getColor(R.color.text));
			} else {
				mStatus.setText(R.string.status_running_error);
				mStatus.setTextColor(getResources().getColor(R.color.error_text));
			}

			if(mGlassStatus.getVisibility() < 1) {
				mGlassStatus.animate().alpha(1).start();
				mWearStatus.animate().alpha(1).translationY(0).start();
			}
		} else {
			mStatus.setText(R.string.status_not_running);
			mStatus.setTextColor(getResources().getColor(R.color.error_text));

			if(mGlassStatus.getAlpha() > 0) {
				// Animate out
				mGlassStatus.animate().alpha(0).start();
				mWearStatus.animate().alpha(0).translationY(-mWearStatus.getHeight()).start();
			}
		}
	}

	private int getPixels(int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}

	// BridgeService.DeviceConnectionListener Callbacks
	@Override
	public void onGlassConnected() {
		Log.d(TAG, "Glass connected");
		mGlassConnected = true;
		mGlassStatus.setText(R.string.glass_connected);
		mGlassStatus.setTextColor(getResources().getColor(R.color.text));
		showView(BridgeService.isRunning());
	}

	@Override
	public void onGlassDisconnected() {
		Log.d(TAG, "Glass disconnected");
		mGlassConnected = false;
		mWearStatus.setText(R.string.glass_disconnected);
		mWearStatus.setTextColor(getResources().getColor(R.color.error_text));
		showView(BridgeService.isRunning());
	}

	@Override
	public void onWearConnected() {
		Log.d(TAG, "Wear connected");
		mWearConnected = true;
		mWearStatus.setText(R.string.wear_connected);
		mWearStatus.setTextColor(getResources().getColor(R.color.text));
		showView(BridgeService.isRunning());
	}

	@Override
	public void onWearDisconnected() {
		Log.d(TAG, "Wear disconnected");
		mWearConnected = false;
		mWearStatus.setText(R.string.wear_disconnected);
		mWearStatus.setTextColor(getResources().getColor(R.color.error_text));
		showView(BridgeService.isRunning());
	}
}
