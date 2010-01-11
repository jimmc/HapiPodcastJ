package info.xuluan.podcast;

import info.xuluan.podcast.parser.FeedParserListenerAdapter;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.service.ReadingService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

/**
 * Manage all subscriptions.
 * 
 * @author Michael Liao (askxuefeng@gmail.com)
 */
public class SubsActivity extends ListActivity {

	private final int MENU_ADD = Menu.FIRST + 1;
	private final int MENU_DEL = Menu.FIRST + 2;

	private final Log log = Utils.getLog(getClass());

	private int selected = (-1);
	private ProgressDialog progress = null;

	private ReadingService serviceBinder = null;
	SimpleCursorAdapter mAdapter;
	private static final String[] PROJECTION = new String[] {
			SubscriptionColumns._ID, // 0
			SubscriptionColumns.TITLE, // 1
	};

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((ReadingService.ReadingBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subs);
		setTitle(getResources().getString(R.string.title_subs));

		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getIntent();
		intent.setData(SubscriptionColumns.URI);
		Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null,
				null, null);

		// Used to map notes entries from the database to views
		mAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, cursor,
				new String[] { SubscriptionColumns.TITLE },
				new int[] { android.R.id.text1 });
		setListAdapter(mAdapter);

		// bind service:
		Intent bindIntent = new Intent(this, ReadingService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// unbind service:
		unbindService(serviceConnection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADD, 0, getResources().getString(R.string.menu_add))
				.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, MENU_DEL, 0, getResources().getString(R.string.menu_del))
				.setIcon(android.R.drawable.ic_menu_delete);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		MenuItem item = menu.findItem(MENU_DEL);
		item.setEnabled(selected != (-1));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			addSubscription();
			break;
		case MENU_DEL:
			// deleteSubscription();
			break;
		}
		return true;
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
			// if(!url.equals(value)){
			log.info("OLD URL =" + value);
			log.info("NEW URL =" + url);
	        if (serviceBinder.querySubscriptionByUrl(url)!=null)
	        	fail_res =  R.string.dialog_message_url_exist;			
			// }
		} catch (MalformedURLException e) {
			fail_res = R.string.dialog_message_malformed_url;
		}
			
		if(fail_res!=0){
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
				// log.info("doInBackground URL ="+url);
				return serviceBinder.fetchFeed(url);
			}

			@Override
			protected void onPostExecute(FeedParserListenerAdapter result) {
				// this method is running on UI thread,
				// so it is safe to update UI:
				// log.info("onPostExecute URL ="+url);

				if (SubsActivity.this.progress != null) {
					SubsActivity.this.progress.dismiss();
					SubsActivity.this.progress = null;
				}
				if (result != null){
					addFeed(url, result);
					Toast.makeText(SubsActivity.this, "success",
							Toast.LENGTH_SHORT).show();					
				}else{
					Toast.makeText(SubsActivity.this, "failed",
					Toast.LENGTH_SHORT).show();						
				}
			}
		};
		asyncTask.execute(url);
	}

	private void addFeed(String url, FeedParserListenerAdapter feed) {
		if (serviceBinder == null)
			return;
		// log.info("addFeeds URL ="+url);
		serviceBinder.addSubscription(url);
		serviceBinder.updateFeed(url, feed);

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

}
