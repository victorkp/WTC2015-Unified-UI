package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.TextView;

import com.victor.kaiser.pendergrast.unified.shared.R;


public class OrderActivity extends Activity implements LiveCardService.SandwichListener {

	private TextView mBreadSelection;
	private TextView mCheeseSelection;
	private TextView mStatus;

	private boolean mBoundToService;
	private LiveCardService.LiveCardBinder mBinder;

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			if (service instanceof LiveCardService.LiveCardBinder) {
				mBinder = (LiveCardService.LiveCardBinder) service;
				mBinder.setOnSandwichListener(OrderActivity.this);
			}

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

		// Keep the screen on
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_order);

		mBreadSelection = (TextView) findViewById(R.id.bread_selection);
		mCheeseSelection = (TextView) findViewById(R.id.cheese_selection);
		mStatus = (TextView) findViewById(R.id.status);

		// Start off by asking user to select bread
		mStatus.setText(R.string.pick_bread);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(mBoundToService) {
			unbindService(mServiceConn);
		}
	}


	// SandwichListener methods
	
	/**
	 * The user selected a bread from
	 * the list of breads in SandwichOptions.BREAD_LIST
	 */
	public void onBreadPicked(String bread) {
		mBreadSelection.setText(bread);
		mStatus.setText(R.string.pick_cheese);
	}

	/**
	 * The user selected a cheese from
	 * the list of cheeses in SandwichOptions.CHEESE_LIST
	 */
	public void onCheesePicked(String cheese) {
		mCheeseSelection.setText(cheese);
		mStatus.setText(R.string.making_sandwich);

		// Simulate sandwich being made by waiting
		// five seconds
		new Thread(new Runnable() {
			@Override
			public void run() {
				try  {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mStatus.setText(R.string.done);
					}
				});
			}
		}).start();
	}

	/**
	 * Order was cancelled,
	 * we'll just finish this OrderActivity
	 */
	public void onOrderCanceled() {
		finish();
	}


}
