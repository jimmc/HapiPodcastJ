/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.xuluan.xappstock;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.Toast;


/**
 * A gallery of basic controls: Button, EditText, RadioButton, Checkbox,
 * Spinner. This example uses the default theme.
 */
public class AddStock extends ListActivity implements TextWatcher {

	private static final String TAG = "AddStock";
	IRemoteService mService = null;
	private boolean mIsBound = false;
	EditText mEditText;
	String mHttpContent = "";
	private ArrayAdapter<String> mAdapter;

	private ArrayList<String> mStrings = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.addstock);
		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings);

		setListAdapter(mAdapter);
		getListView().setOnCreateContextMenuListener(this);
		mEditText = (EditText) findViewById(R.id.stockid);
		mEditText.addTextChangedListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(IRemoteService.class.getName()), mConnection,
				Context.BIND_AUTO_CREATE);
		mIsBound = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mIsBound) {
			if (mService != null) {
				try {
					mService.updateStockID(null);
					mService.unregisterCallback(mCallback);
				} catch (RemoteException e) {
				}
			}
		}
		unbindService(mConnection);
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = IRemoteService.Stub.asInterface(service);
			try {
				mService.registerCallback(mCallback);
				mIsBound = true;
			} catch (RemoteException e) {

			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	private IRemoteServiceCallback mCallback = new IRemoteServiceCallback.Stub() {

		public void http_request_update(String value) {
			mHttpContent = value;

			mHandler.sendMessage(mHandler.obtainMessage(UPDATE_STOCK_LIST_MSG));
		}
	};

	private static final int UPDATE_STOCK_LIST_MSG = 1;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case UPDATE_STOCK_LIST_MSG:

				try {
					mAdapter.clear();
					JSONObject obj_match = new JSONObject(mHttpContent);
					JSONArray arr = obj_match.getJSONArray("matches");
					Log.d(TAG, "got  JSONArray");

					if (arr.length() == 0) {
						Toast.makeText(AddStock.this, getText(R.string.no_stock_match),
								Toast.LENGTH_SHORT).show();
					}
					for (int i = 0; i < arr.length(); i++) {
						JSONObject obj = new JSONObject(arr.get(i).toString());
						String symbol = obj.get("e").toString() + ":"
								+ obj.get("t").toString();
						String line = obj.get("n").toString();

						Pattern p = Pattern.compile("\\w+\\:[\\w\\.\\-\\_]+");
						Matcher m = p.matcher(symbol);
						if (m.find())
							mAdapter.add(symbol + "\n" + line);
					}
					Log.d(TAG, "mAdapter update");

				} catch (Exception e) {

				}
				break;
			default:
				super.handleMessage(msg);
			}
		}

	};

	public void afterTextChanged(Editable s) {
		if (mService != null) {
			try {
				mService.updateStockID(mEditText.getText().toString());

			} catch (RemoteException e) {

			}
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		String str = mStrings.get(position);

		Pattern pattern = Pattern.compile("\n");
		String[] strs = pattern.split(str);
		final String symbol = strs[0];

		if (str != null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(AddStock.this);
			builder.setTitle(getText(R.string.ask_add_stock));
			builder.setMessage(str);
			builder.setPositiveButton(getText(R.string.Yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							AddStock.this.AddSymbol(symbol);
							dialog.dismiss();
						}
					});
			builder.setNegativeButton(getText(R.string.No),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							dialog.dismiss();
						}
					});
			AlertDialog ad = builder.create();

			ad.show();
		}

	}

	private void AddSymbol(String symbol) {
		String text = symbol;

		ContentValues values = new ContentValues();
		values.put("symbol", text);

		Uri uri = this.getContentResolver().insert(
				StockMetaData.Stocks.CONTENT_URI, values);
		if (uri == null) {
			Log.w(TAG, "Failed to insert new note into " + text);
			AlertDialog.Builder builder = new AlertDialog.Builder(AddStock.this);
			builder.setTitle(getText(R.string.add_stock_fail_title));
			builder.setMessage(getText(R.string.add_stock_fail_content));
			builder.setPositiveButton(getText(R.string.OK),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {

							dialog.dismiss();
						}
					});
			AlertDialog ad = builder.create();

			// show
			ad.show();
		} else {

			this.finish();
		}
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

}
