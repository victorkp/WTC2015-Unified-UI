package com.victor.kaiser.pendergrast.unified.demo;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.victor.kaiser.pendergrast.unified.shared.SandwichOptions;
import com.victor.kaiser.pendergrast.unified.shared.WearableComm;


public class OngoingNotificationListenerService extends WearableListenerService {

	private static final String TAG = "NotifListenerService";

	/**
	 * We only care about messages in this demo;
	 * not using the Wearable DataAPI
	 */
	@Override
	public void onMessageReceived(MessageEvent messageEvent) {
		Log.i(TAG, "Received message: " + messageEvent.getPath());

		if(messageEvent.getPath().equals(WearableComm.PATH_SHOW_BREAD_LIST)) {
			// Start the PickFromListActivity, pass Bread path and Bread list
			Intent showBreads = new Intent(getBaseContext(), PickFromListActivity.class);
			showBreads.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			showBreads.putExtra(PickFromListActivity.KEY_TITLE, 
					getString(R.string.title_bread));
			showBreads.putExtra(PickFromListActivity.KEY_SELECTION_PATH, 
					WearableComm.PATH_BREAD_PICKED);
			showBreads.putExtra(PickFromListActivity.KEY_LIST_ITEMS, 
					SandwichOptions.BREAD_OPTIONS);
			showBreads.putExtra(PickFromListActivity.KEY_CANCEL_PATH, 
					WearableComm.PATH_ORDER_CANCELED);

			getApplication().startActivity(showBreads);
		} else if(messageEvent.getPath().equals(WearableComm.PATH_SHOW_CHEESE_LIST)) {
			// Start the PickFromListActivity, pass Cheese path and Cheese list
			Intent showCheeses= new Intent(getBaseContext(), PickFromListActivity.class);
			showCheeses.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			showCheeses.putExtra(PickFromListActivity.KEY_TITLE, getString(R.string.title_cheese));
			showCheeses.putExtra(PickFromListActivity.KEY_SELECTION_PATH, WearableComm.PATH_CHEESE_PICKED);
			showCheeses.putExtra(PickFromListActivity.KEY_LIST_ITEMS, SandwichOptions.CHEESE_OPTIONS);
			showCheeses.putExtra(PickFromListActivity.KEY_CANCEL_PATH, WearableComm.PATH_ORDER_CANCELED);
			getApplication().startActivity(showCheeses);
		} else if(messageEvent.getPath().equals(WearableComm.PATH_SHOW_SANDWICH_ALERT)) {
			// Start the OrderDoneActivity
			Intent showOrderDone = new Intent(getBaseContext(), OrderDoneActivity.class);
			showOrderDone.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			getApplication().startActivity(showOrderDone);
		} else {
			// Uh-oh
			Log.e(TAG, "Unknown message");
		}
	}

}
