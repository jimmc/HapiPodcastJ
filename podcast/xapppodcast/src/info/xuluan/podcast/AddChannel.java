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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import info.xuluan.podcast.fetcher.FeedFetcher;
import info.xuluan.podcast.fetcher.Response;
import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.SearchItem;
import info.xuluan.podcast.parser.SearchParserHandler;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.Log;

public class AddChannel extends PodcastBaseActivity implements TextWatcher {

	private final Log log = Log.getLog(getClass());
	private ProgressDialog progress = null;

	EditText mEditText;
	String mHttpContent = "";
	private ArrayAdapter<String> mAdapter;

	private SearchParserHandler mHandler = null;

	private ArrayList<String> mStrings = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.addchannel);
		setTitle(getResources().getString(R.string.search_channel));

		mAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mStrings);

		setListAdapter(mAdapter);
		getListView().setOnCreateContextMenuListener(this);
		mEditText = (EditText) findViewById(R.id.stockid);
		mEditText.addTextChangedListener(this);
		startInit();

		ImageButton prev = (ImageButton) findViewById(R.id.ButtonPrev);
		prev.setEnabled(false);
		prev.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "";
				if (mHandler != null) {
					int i = mHandler.startindex - 10;
					if (i > 1) {
						str = "&start=" + i;
					}
				}
				String url = getUrl(str);
				if (url != null) {
					start_search(url);
				}

			}
		});
		ImageButton next = (ImageButton) findViewById(R.id.ButtonNext);
		next.setEnabled(false);
		next.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "";
				if (mHandler != null) {
					int i = mHandler.startindex + 10;
					if (i < mHandler.foundcount) {
						str = "&start=" + i;
					}
				}
				String url = getUrl(str);
				if (url != null) {
					start_search(url);
				}
			}
		});

		ImageButton start = (ImageButton) findViewById(R.id.ButtonStart);
		start.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				m.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
				// EditText.setInputType(InputType.TYPE_NULL);
				m.hideSoftInputFromInputMethod(mEditText.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);

				String url = getUrl("");
				if (url != null) {
					start_search(url);
				}
			}
		});

	}

	public String getUrl(String s) {
		String prefix = "http://www.feedzie.com/scripts/getSearchListing_v2.php?stype=podcast&query=";
		String suffix = "&searchbtn=Search&version=1.0";
		String str = AddChannel.this.mEditText.getText().toString();

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
			return prefix + str + s + suffix;

		return null;
	}

	public SearchParserHandler fetchFeed(String url) {
		log.debug("fetchFeed start");

		FeedFetcher fetcher = new FeedFetcher();
		SearchParserHandler handler = new SearchParserHandler();

		try {

			Response response = fetcher.fetch(url);

			log.debug("fetcher.fetch end");
			// log.debug("print string " + response.getContentAsString());
			byte[] content;
			if (response != null) {
				log.debug("response != null");

				content = response.content;
				log.debug("content len: " + content.length);
				for (int i = 0; i < content.length; i++) {
					if (content[i] < 0x20) {
						// log.debug("pos = "+ i+ " value= " +content[i]);
						content[i] = 0x2e;
					}
				}

				FeedParser.getDefault().parse(
						new ByteArrayInputStream(content), handler);
				// new StringBufferInputStream(response.getContentAsString()),

			} else {
				log.debug("response == null");
			}

		} catch (Exception e) {
			log.debug("Parse XML error:", e);
			// e.printStackTrace();

		}

		return handler;

	}

	private void start_search(String zie_url) {
		AddChannel.this.progress = ProgressDialog.show(AddChannel.this,
				getResources().getText(R.string.dialog_title_loading),
				getResources().getText(R.string.dialog_message_loading), true);
		AsyncTask<String, ProgressDialog, SearchParserHandler> asyncTask = new AsyncTask<String, ProgressDialog, SearchParserHandler>() {
			String url;

			@Override
			protected SearchParserHandler doInBackground(String... params) {

				url = params[0];
				// log.debug("doInBackground URL ="+url);
				return fetchFeed(url);

			}

			@Override
			protected void onPostExecute(SearchParserHandler result) {

				if (AddChannel.this.progress != null) {
					AddChannel.this.progress.dismiss();
					AddChannel.this.progress = null;
				}
				mAdapter.clear();

				if (result.startindex == 0) {
					Toast.makeText(AddChannel.this, "network fail",
							Toast.LENGTH_SHORT).show();
				} else if (result.foundcount == 0) {
					Toast.makeText(AddChannel.this, "no data found",
							Toast.LENGTH_SHORT).show();
				}
				List<SearchItem> items = result.items;
				AddChannel.this.mHandler = result;
				updateBtn();

				for (int i = 0; i < items.size(); i++) {
					mAdapter.add(items.get(i).title);

				}

			}
		};
		asyncTask.execute(zie_url);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		List<SearchItem> items = mHandler.items;
		final SearchItem item = items.get(position);

		String content = "Language: " + item.language + "<br /><br />"
				+ item.content;
		AlertDialog d = new AlertDialog.Builder(this).setIcon(
				R.drawable.alert_dialog_icon).setTitle(item.title).setMessage(
				Html.fromHtml(content)).setPositiveButton(R.string.subscribe,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						Subscription sub = Subscription.getByUrl(
								getContentResolver(), item.url);
						if (sub != null) {
							Toast.makeText(AddChannel.this,
									"The channel has been subscribed.",
									Toast.LENGTH_SHORT).show();
							return;
						}

						String content = "<language>" + item.language
								+ "</language>";
						content += "<tag>" + item.tags + "</tag>";
						content += "<content>" + item.content + "</content>";

						ContentValues cv = new ContentValues();
						cv.put(SubscriptionColumns.TITLE, item.url);
						cv.put(SubscriptionColumns.URL, item.url);
						cv.put(SubscriptionColumns.LAST_UPDATED, 0L);
						cv.put(SubscriptionColumns.COMMENT, content);
						getContentResolver()
								.insert(SubscriptionColumns.URI, cv);

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
		ImageButton prev = (ImageButton) findViewById(R.id.ButtonPrev);
		ImageButton next = (ImageButton) findViewById(R.id.ButtonNext);

		if (mHandler == null) {
			prev.setEnabled(false);
			next.setEnabled(false);
			next.setImageResource(R.drawable.nextdis);
			prev.setImageResource(R.drawable.prevdis);

			return;
		}

		if ((mHandler.startindex - 10) < 2) {
			prev.setEnabled(false);
			prev.setImageResource(R.drawable.prevdis);

		} else {
			prev.setEnabled(true);
			prev.setImageResource(R.drawable.prev);

		}
		if ((mHandler.startindex + 10) < mHandler.foundcount) {
			next.setEnabled(true);
			next.setImageResource(R.drawable.next);
		} else {
			next.setEnabled(false);
			next.setImageResource(R.drawable.nextdis);
		}

	}

	@Override
	public void afterTextChanged(Editable arg0) {
		mHandler = null;
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
