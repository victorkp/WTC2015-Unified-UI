package com.victor.kaiser.pendergrast.unified.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

		mTitle = (TextView) findViewById(R.id.list_title);
		mList = (ListView) findViewById(R.id.list);

		if(mTitleText != null) {
			mTitle.setText(mTitleText);
		} else {
			mTitle.setVisibility(View.GONE);
		}

		mList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListItems));
	}

}
