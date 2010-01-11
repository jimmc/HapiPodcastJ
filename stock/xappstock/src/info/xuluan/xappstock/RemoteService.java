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

import info.xuluan.xappstock.fetch.FeedFetcher;
import info.xuluan.xappstock.fetch.FeedFetcherListenerAdapter;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.util.Log;
import android.widget.Toast;
import android.app.Service;
import android.net.Uri;
import android.database.Cursor;

public class RemoteService extends Service {

	private static final String TAG = "RemoteService";
	final RemoteCallbackList<IRemoteServiceCallback> mCallbacks = new RemoteCallbackList<IRemoteServiceCallback>();

	String mMirrorStockID = "";
	String mStockID = "";
	
	int mUpdateInteval = 3 * 60 * 1000;
	int mFailCount = 0;
	
	boolean mUpdataRuning = false;
	boolean mAddStockRuning = false;
	
	private static final String[] PROJECTION = new String[] {
			StockMetaData.Stocks._ID, // 0
			StockMetaData.Stocks.SYMBOL, // 1
	};

	@Override
	public void onCreate() {

		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.xappstock_preferences",
				Service.MODE_WORLD_READABLE);

		String s = pref.getString("list_preference", "3");
		int aInt = Integer.parseInt(s);
		mUpdateInteval = aInt * 60 * 1000;
		Log.d(TAG, "onCreate !!! " + s);
	}

	@Override
	public void onDestroy() {
		mCallbacks.kill();
		mHandler.removeMessages(ADD_STOCK_MSG);
		mHandler.removeMessages(ONLY_UPDATE_MSG);
		mHandler.removeMessages(UPDATE_MSG);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// Select the interface to return. If your service only implements
		// a single interface, you can just return it here without checking
		// the Intent.
		if (IRemoteService.class.getName().equals(intent.getAction())) {
			return mBinder;
		}

		return null;
	}

	/**
	 * The IRemoteInterface is defined through IDL
	 */
	private final IRemoteService.Stub mBinder = new IRemoteService.Stub() {
		public void registerCallback(IRemoteServiceCallback cb) {
			if (cb != null)
				mCallbacks.register(cb);
		}

		public void unregisterCallback(IRemoteServiceCallback cb) {
			if (cb != null)
				mCallbacks.unregister(cb);
		}

		public int getPid() {
			return Process.myPid();
		}

		public void updateStockID(String stockid) {
			if (stockid == null) {
				mAddStockRuning = false;
				mHandler.removeMessages(ADD_STOCK_MSG);

			} else {
				mStockID = stockid;
				mHandler.sendEmptyMessage(ADD_STOCK_MSG);
				mAddStockRuning = true;
				Log.d("SERVICE", "setStockid: " + mStockID + ";" + stockid);
			}
		}

		public void updateStockInfo() {
			mHandler.sendEmptyMessage(ONLY_UPDATE_MSG);
			Log.d(TAG, "update_stock");
		}

		public void setUpdateFlag(boolean b) {
			if (mUpdataRuning == b)
				return;

			if (b) {
				mHandler.sendEmptyMessage(UPDATE_MSG);
			} else {
				mHandler.removeMessages(UPDATE_MSG);
			}

			mUpdataRuning = b;

		}

	};

	private static final int ADD_STOCK_MSG = 1;
	private static final int UPDATE_MSG = 2;
	private static final int ONLY_UPDATE_MSG = 3;
	/**
	 * Our Handler used to execute operations on the main thread. This is used
	 * to schedule increments of our value.
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ADD_STOCK_MSG: {
				if (!mMirrorStockID.equals(mStockID)) {

					if (mStockID.length() > 0) {
						String str = get_http_request("http://www.google.com/finance/match?matchtype=matchall?&q="
								+ mStockID);
						if (str != null) {
							mMirrorStockID = mStockID;
							mFailCount = 0;
							final int N = mCallbacks.beginBroadcast();
							Log.d(TAG, "mCallbacks " + N);
							for (int i = 0; i < N; i++) {
								try {
									mCallbacks.getBroadcastItem(i)
											.http_request_update(str);
								} catch (RemoteException e) {
								}
							}
							mCallbacks.finishBroadcast();
						} else {
							mFailCount++;
							if (mFailCount > 5) {
								mFailCount = 0;
								mMirrorStockID = mStockID;

								Toast.makeText(RemoteService.this,
										getText(R.string.no_network),
										Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
				removeMessages(ADD_STOCK_MSG);
				if (mAddStockRuning)
					sendMessageDelayed(obtainMessage(ADD_STOCK_MSG), 1 * 200);
			}
				break;
			case UPDATE_MSG:
				updatestock();
				removeMessages(UPDATE_MSG);
				if (mUpdataRuning) {
					sendMessageDelayed(obtainMessage(UPDATE_MSG), mUpdateInteval);
				}

				break;
			case ONLY_UPDATE_MSG:
				updatestock();
				break;
			default:
				super.handleMessage(msg);
			}
		}

		private String get_http_request(String url) {
			FeedFetcher fetcher = new FeedFetcher();
			FeedFetcherListenerAdapter listener = new FeedFetcherListenerAdapter();
			return fetcher.fetch(url, listener);
		}

		private void updatestock() {

			String symbols = "";
			Cursor cur = getContentResolver().query(
					StockMetaData.Stocks.CONTENT_URI, PROJECTION, null, null,
					null);
			if (cur.moveToFirst() == false) {
				Log.w(TAG, "cur is null");
				return;
			}
			for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
				int symbolColumn = cur
						.getColumnIndex(StockMetaData.Stocks.SYMBOL);

				String symbol = cur.getString(symbolColumn);
				symbols += symbol + ",";
			}
			Log.d(TAG, "symbols: " + symbols);
			String url = "http://www.google.com/finance/info?q=" + symbols
					+ "&infotype=infoquoteall";
			String content = get_http_request(url);

			if (content == null) {
				Log.w(TAG, "http get null ");
				return;
			}

			try {
				JSONArray arr = new JSONArray(content.substring(3));
				for (int i = 0; i < arr.length(); i++) {
					JSONObject obj = new JSONObject(arr.get(i).toString());
					String symbol = obj.get("e").toString() + ":"
							+ obj.get("t").toString();
					ContentValues values = new ContentValues();
					values.put(StockMetaData.Stocks.NAME, obj.get("name")
							.toString());
					values.put(StockMetaData.Stocks.LAST_PRICE, obj.get("l")
							.toString());

					values.put(StockMetaData.Stocks.OPEN, obj.get("op")
							.toString());
					// Log.e(TAG, "OPEN"+obj.get("op").toString());

					values.put(StockMetaData.Stocks.HIGH, obj.get("hi")
							.toString());
					// Log.e(TAG, "HIGH"+obj.get("hi").toString());

					values.put(StockMetaData.Stocks.LOW, obj.get("lo")
							.toString());
					// Log.e(TAG, "LOW"+obj.get("lo").toString());

					values.put(StockMetaData.Stocks.CHANGE, obj.get("cp")
							.toString());
					// Log.e(TAG, "CHANGE"+obj.get("cp").toString());

					values.put(StockMetaData.Stocks.PE, obj.get("pe")
							.toString());
					// Log.e(TAG, "PE"+obj.get("pe").toString());

					values.put(StockMetaData.Stocks.EPS, obj.get("eps")
							.toString());
					// Log.e(TAG, "EPS"+obj.get("eps").toString());

					values.put(StockMetaData.Stocks.TIME, obj.get("lt")
							.toString());
					// Log.e(TAG, "TIME"+obj.get("lt").toString());

					values.put(StockMetaData.Stocks.MKT_VALUE, obj.get("mc")
							.toString());
					// Log.e(TAG, "MKT_VALUE"+obj.get("mc").toString());

					Uri uri = StockMetaData.Stocks.CONTENT_URI;
					getContentResolver().update(uri, values, "symbol=?",
							new String[] { symbol });

				}
			} catch (Exception e) {
				Log.w(TAG, "http request error: " + e);

			}

		}

	};

}
