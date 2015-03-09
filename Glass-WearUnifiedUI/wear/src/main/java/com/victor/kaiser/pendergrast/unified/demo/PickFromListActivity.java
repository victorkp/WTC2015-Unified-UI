package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.victor.kaiser.pendergrast.unified.demo.view.RecyclerArrayAdapter;
import com.victor.kaiser.pendergrast.unified.shared.WearableComm;

public class PickFromListActivity extends Activity {

	private static final String TAG = "OrderDoneActivity";

	/**
	 * Key used to set the PickFromListActivity's title
	 */
	public static final String KEY_TITLE = "title";

	/**
	 * Key used to set what path to call on a selection cancel
	 */
	public static final String KEY_CANCEL_PATH = "cancel_path";

	/**
	 * Key used to set what path to call on a selection
	 */
	public static final String KEY_SELECTION_PATH = "selection_path";

	/**
	 * Key used to set a String[] of items to display
	 */
	public static final String KEY_LIST_ITEMS = "list_items";

	private String mCancelPath;
	private String mSelectionPath;
	private String[] mListItems;
	private String mTitleText;

	private TextView mTitle;
	private ListView mList;

	private GoogleApiClient mGoogleClient;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_pick_from_list);

		Intent intent = getIntent();
		if(intent == null) {
			// Need arguments...
			Log.e(TAG, "PickFromListActivity needs values corresponding to " +
					"KEY_CANCEL_PATH, KEY_SELECTION_PATH  and " +
					"KEY_LIST_ITEMS");
			finish();
			return;
		}

		mCancelPath = intent.getStringExtra(KEY_CANCEL_PATH);
		mSelectionPath = intent.getStringExtra(KEY_SELECTION_PATH);
		mTitleText = intent.getStringExtra(KEY_TITLE);
		mListItems = intent.getStringArrayExtra(KEY_LIST_ITEMS);

		if(mSelectionPath == null || mCancelPath == null || mListItems == null) {
			// Need arguments...
			Log.e(TAG, "PickFromListActivity needs values corresponding to KEY_SELECTION_PATH");
			finish();
			return;
		}

		// Setup GoogleApiClient for Android Wear messaging
		mGoogleClient = new GoogleApiClient.Builder(this)
				.addApi(Wearable.API)
				.build();
		mGoogleClient.connect();


		mTitle = (TextView) findViewById(R.id.list_title);
		mList = (ListView) findViewById(R.id.list);

		if(mTitleText != null) {
			mTitle.setText(mTitleText);
		} else {
			mTitle.setVisibility(View.GONE);
		}

		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListItems));
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				if(mGoogleClient.isConnected()) {
					Wearable.NodeApi.getConnectedNodes(mGoogleClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
						@Override
						public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
							String path = mSelectionPath + mListItems[position];
							Log.i(TAG, "Sending \"" + path + "\"");
							for(Node node : getConnectedNodesResult.getNodes()) {
								Log.i(TAG, "Sending message to " + node.getDisplayName());
								PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(mGoogleClient, node.getId(), path, null);
								result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
									@Override
									public void onResult(MessageApi.SendMessageResult sendMessageResult) {
										Log.i(TAG, "Delivering message status: " + sendMessageResult.getStatus().getStatusMessage());
									}
								});
							}

							PickFromListActivity.this.finish();
						}
					});
				} else {
					Log.e(TAG, "GoogleApiClient not connected");
				}
			}
		});
	}

}
