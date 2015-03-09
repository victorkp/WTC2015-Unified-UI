package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.WindowManager;

public class OrderDoneActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON, WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		setContentView(R.layout.activity_order_done);

		// Vibrate to alert user
		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		vibrator.vibrate(1000);
	}

}
