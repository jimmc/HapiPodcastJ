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

package info.xuluan.podcast;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import info.xuluan.podcast.fetcher.FeedFetcher;
import info.xuluan.podcast.fetcher.Response;
import info.xuluan.podcast.parser.SearchItem;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.utils.Log;

public class SearchActivity extends PodcastBaseActivity implements TextWatcher {

	private final Log log = Log.getLog(getClass());
	private ProgressDialog progress = null;

	EditText mEditText;
	String mHttpContent = "";
	private ArrayAdapter<String> mAdapter;
	private int mStart = 0;
	public List<SearchItem> mItems = new ArrayList<SearchItem>();
	private ArrayList<String> mStrings = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.addchannel);
		setTitle(getResources().getString(R.string.title_search_channel));

		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings);

		setListAdapter(mAdapter);
		getListView().setOnCreateContextMenuListener(this);
		mEditText = (EditText) findViewById(R.id.keywords);
		mEditText.addTextChangedListener(this);
		
		mPrevIntent = new Intent(this, PlayListActivity.class);
		mNextIntent = new Intent(this, ChannelsActivity.class);
		
		startInit();
		Button next = (Button) findViewById(R.id.ButtonNext);

		next.setEnabled(false);
		next.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "&start=" + mStart;

				String url = getUrl();
				if (url != null) {
					start_search(url+str);
				}
			}
		});

		ImageButton start = (ImageButton) findViewById(R.id.ButtonStart);
		start.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				m.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
				m.hideSoftInputFromInputMethod(mEditText.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
				
				String str = "&start=" + mStart;

				String url = getUrl();
				if (url != null) {
					start_search(url+str);
				}
			}
		});

	}

	public String getUrl() {
		String prefix = "http://lfe-alpo-gm.appspot.com/search?q=";
		String suffix = "";
		String str = SearchActivity.this.mEditText.getText().toString();

		Pattern pattern = Pattern.compile("^\\s+");
		Matcher matcher = pattern.matcher(str);

		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+$");
		matcher = pattern.matcher(str);

		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+");
		matcher = pattern.matcher(str);

		str = matcher.replaceAll("+");
		log.debug("query string = " + str);
		if (str.length() > 0)
			return prefix + str + suffix;

		return null;
	}
	
	public List<SearchItem> parseResult(String content) {
		 List<SearchItem> items = new ArrayList<SearchItem>();

		try {
			JSONObject obj_match = new JSONObject(content);
			JSONArray arr = obj_match.getJSONArray("items");

			if (arr.length() == 0) {

			}
			
			for (int i = 0; i < arr.length(); i++) {
				SearchItem item = new SearchItem();
				JSONObject obj = new JSONObject(arr.get(i).toString());
				item.url = obj.get("feed_url").toString();
				item.link = obj.get("feed_url").toString();
				item.title = obj.get("feed_title").toString();
				item.content = obj.get("summary").toString();
				items.add(item);
				
			}

		} catch (Exception e) {

		}
		
		return items;
	}
	public String fetchFeed(String url) {
		log.debug("fetchFeed start");

		FeedFetcher fetcher = new FeedFetcher("Google-Listen/1.1.2 (droid)");

		try {

			Response response = fetcher.fetch(url);

			log.debug("fetcher.fetch end");
			// log.debug("print string " + response.getContentAsString());
			if (response != null) {
				log.debug("response != null");
				return response.getContentAsString();
			} else {
				log.debug("response == null");
			}

		} catch (Exception e) {
			log.debug("Parse XML error:", e);
			// e.printStackTrace();

		}

		return null;

	}

	private void start_search(String zie_url) {
		SearchActivity.this.progress = ProgressDialog.show(SearchActivity.this,
				getResources().getText(R.string.dialog_title_loading),
				getResources().getText(R.string.dialog_message_loading), true);
		AsyncTask<String, ProgressDialog, String> asyncTask = new AsyncTask<String, ProgressDialog, String>() {
			String url;

			@Override
			protected String doInBackground(String... params) {

				url = params[0];
				// log.debug("doInBackground URL ="+url);
				return fetchFeed(url);

			}

			@Override
			protected void onPostExecute(String result) {

				if (SearchActivity.this.progress != null) {
					SearchActivity.this.progress.dismiss();
					SearchActivity.this.progress = null;
				}

				if (result == null) {
					Toast.makeText(SearchActivity.this, getResources().getString(R.string.network_fail),
							Toast.LENGTH_SHORT).show();
				} else {
					List<SearchItem> items = parseResult(result);
					if(items.size()==0) {
						Toast.makeText(SearchActivity.this, getResources().getString(R.string.no_data_found),
								Toast.LENGTH_SHORT).show();					
					}else{
						mStart += items.size();
						for (int i = 0; i < items.size(); i++) {
							mItems.add(items.get(i));
							mAdapter.add(items.get(i).title);

						}					
					}
				}
				
				updateBtn();
			}
		};
		asyncTask.execute(zie_url);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final SearchItem item = mItems.get(position);

		String content = item.content;
		AlertDialog d = new AlertDialog.Builder(this).setIcon(
				R.drawable.alert_dialog_icon).setTitle(item.title).setMessage(
				Html.fromHtml(content)).setPositiveButton(R.string.subscribe,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Subscription sub = new Subscription(item.url);
						sub.link = item.link;
						
						String tags = SearchActivity.this.mEditText.getText().toString();
						String content = "<tag>" + tags + "</tag>";
						content += "<content>" + item.content + "</content>";
						
						sub.comment = content;
						
						int rc = sub.subscribe(getContentResolver());
						
						if(rc == Subscription.ADD_FAIL_DUP){
							Toast.makeText(SearchActivity.this,
									getResources().getString(R.string.already_subscribed),
									Toast.LENGTH_SHORT).show();														
						}else if(rc == Subscription.ADD_SUCCESS){
							Toast.makeText(SearchActivity.this,
									getResources().getString(R.string.success),
									Toast.LENGTH_SHORT).show();								
						}else {
							Toast.makeText(SearchActivity.this,
									getResources().getString(R.string.fail),
									Toast.LENGTH_SHORT).show();	
						}

						mServiceBinder.start_update();

					}
				}).setNegativeButton(R.string.menu_cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						/* User clicked Cancel so do some stuff */
					}
				}).create();

		d.show();

	}

	public void updateBtn() {
		Button next = (Button) findViewById(R.id.ButtonNext);
		if(mStart==0){
			next.setEnabled(false);
			
		} else {
			next.setEnabled(true);
			
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mStart = 0;
		mItems.clear();
		mAdapter.clear();
		updateBtn();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

}
