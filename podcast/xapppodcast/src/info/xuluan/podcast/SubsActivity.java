package info.xuluan.podcast;


import info.xuluan.podcast.parser.FeedParserListenerAdapter;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class SubsActivity extends PodcastBaseActivity {
	private final int MENU_ADD = Menu.FIRST + 2;

	
	private final int MENU_ITEM_DELETE = Menu.FIRST + 10;

	private ProgressDialog progress = null;

	private static final String[] PROJECTION = new String[] {
			SubscriptionColumns._ID, // 0
			SubscriptionColumns.TITLE, // 1
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subs);
		setTitle(getResources().getString(R.string.title_subs));

		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getIntent();
		intent.setData(SubscriptionColumns.URI);

		mPrevIntent = new Intent(this, SearchChannel.class);
		mNextIntent = new Intent(this, MainActivity.class);		
		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADD, 0, getResources().getString(R.string.menu_add))
				.setIcon(android.R.drawable.ic_menu_add);
	
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			addSubscription();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (ClassCastException e) {
			log.error("bad menuInfo", e);
			return;
		}

		Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
		if (cursor == null) {
			// For some reason the requested item isn't available, do nothing
			return;
		}

		// Setup the menu header
		menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

		// Add a menu item to delete the note
		menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete);

	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info;
		try {
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		} catch (ClassCastException e) {
			log.error("bad menuInfo", e);
			return false;
		}

		switch (item.getItemId()) {
		case MENU_ITEM_DELETE: {

			// TODO are you sure?

			Subscription subs = Subscription.getSubbyId(getContentResolver(),
					info.id);
			if (subs == null)
				return true;

			subs.delete(getContentResolver());

		}
		}

		return true;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {
			FeedItem item = FeedItem.getById(getContentResolver(), id);
			if ((item != null)
					&& (item.status == ItemColumns.ITEM_STATUS_UNREAD)) {
				item.status = ItemColumns.ITEM_STATUS_READ;
				item.update(getContentResolver());
			}

			startActivity(new Intent(Intent.ACTION_EDIT, uri));
		}
	}	

	private void addSubscription() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getResources().getText(R.string.dialog_title_add_sub));
		alert.setMessage(getResources()
				.getText(R.string.dialog_message_add_sub));

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setText("http://");
		alert.setView(input);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						checkValid(input.getText().toString());
					}
				});
		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
		alert.show();
	}

	void checkValid(String value) {
		String url = null;
		int fail_res = 0;
		try {
			url = formatURL(value);

			if (Subscription.getByUrl(getContentResolver(), url) != null)
				fail_res = R.string.dialog_message_url_exist;
		} catch (MalformedURLException e) {
			fail_res = R.string.dialog_message_malformed_url;
		}

		if (fail_res != 0) {
			new AlertDialog.Builder(this).setTitle(
					getResources().getText(R.string.dialog_title_add_sub))
					.setMessage(getResources().getText(fail_res))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).show();
			return;
		}
		this.progress = ProgressDialog.show(this, getResources().getText(
				R.string.dialog_title_loading), getResources().getText(
				R.string.dialog_message_loading), true);
		AsyncTask<String, ProgressDialog, FeedParserListenerAdapter> asyncTask = new AsyncTask<String, ProgressDialog, FeedParserListenerAdapter>() {
			String url;

			@Override
			protected FeedParserListenerAdapter doInBackground(String... params) {

				url = params[0];
				// log.debug("doInBackground URL ="+url);
				return mServiceBinder.fetchFeed(url);
			}

			@Override
			protected void onPostExecute(FeedParserListenerAdapter result) {

				if (SubsActivity.this.progress != null) {
					SubsActivity.this.progress.dismiss();
					SubsActivity.this.progress = null;
				}
				if (result != null) {
					addFeed(url, result);
					Toast.makeText(SubsActivity.this, getResources().getString(R.string.success),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(SubsActivity.this, getResources().getString(R.string.fail),
							Toast.LENGTH_SHORT).show();
				}
			}
		};
		asyncTask.execute(url);
	}

	private void addFeed(String url, FeedParserListenerAdapter feed) {
		if (mServiceBinder == null)
			return;
		mServiceBinder.addSubscription(url);
		mServiceBinder.updateFeed(url, feed);

	}

	private String formatURL(String value) throws MalformedURLException {
		Pattern p = Pattern.compile("^(http|https)://.*",
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(value);
		if (m.find()) {
			URL url = new URL(value);
			return url.toString();
		}

		return null;
	}

	@Override
	public void startInit() {

		mCursor = managedQuery(SubscriptionColumns.URI, PROJECTION, null, null,
				null);

		// Used to map notes entries from the database to views
		mAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, mCursor,
				new String[] { SubscriptionColumns.TITLE },
				new int[] { android.R.id.text1 });
		setListAdapter(mAdapter);

		super.startInit();
	}
}
