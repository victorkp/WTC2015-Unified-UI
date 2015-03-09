package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.victor.kaiser.pendergrast.unified.shared.R;


public class MenuActivity extends Activity {

	private static final String TAG = "MenuActivity";

	private boolean mIsAttached = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(mIsAttached) {
			openOptionsMenu();
		}
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mIsAttached = true;
		openOptionsMenu();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_livecard, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId()  == R.id.action_stop) {
			stopService(new Intent(this, LiveCardService.class));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);

		// This MenuActivity has no purpose if the
		// menu is closed...
		finish();
	}
}
