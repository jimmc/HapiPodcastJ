package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ReadActivity extends Activity {

	static final int MENU_DOWNLOAD = Menu.FIRST + 2;
	static final int MENU_PLAY = Menu.FIRST + 6;
	static final int MENU_BACK = Menu.FIRST + 5;
	static final int MENU_PREF = Menu.FIRST + 3;
	String item_id;

	private final Log log = Log.getLog(getClass());
	//private String url = null;

	ComponentName service = null;

	private PodcastService serviceBinder = null;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((PodcastService.ReadingBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {


		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		MenuItem item = menu.findItem(MENU_DOWNLOAD);
		FeedItem feed_item = FeedItem.getById(getContentResolver(), Integer
				.parseInt(item_id));
		if (feed_item.status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW) {
			item.setEnabled(true);

		} else {
			item.setEnabled(false);

		}
		/*
		 * item = menu.findItem(MENU_PLAY);
		 * 
		 * if(feed_item.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
		 * item.setEnabled(true);
		 * 
		 * }else{ item.setEnabled(false);
		 * 
		 * }
		 */

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == MENU_DOWNLOAD) {

			ContentValues cv = new ContentValues();

			cv.put(ItemColumns.STATUS, ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE);
			getContentResolver().update(ItemColumns.URI, cv, "_ID=?",
					new String[] { item_id });
			serviceBinder.start_download();
			return true;
		} else if (item.getItemId() == MENU_BACK) {
			finish();
			return true;
		} else if (item.getItemId() == MENU_PREF) {
			startActivity(new Intent(this, Pref.class));
			return true;
		} else if (item.getItemId() == MENU_PLAY) {
			play(Integer.parseInt(item_id));
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read);

		Intent intent = getIntent();

		Uri uri = intent.getData();
		Cursor cursor = getContentResolver().query(uri,
				ItemColumns.ALL_COLUMNS, null, null, null);
		if (!cursor.moveToFirst()) {
			// show404();
			return;
		}
		item_id = cursor.getString(cursor.getColumnIndex(ItemColumns._ID));

		String title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.TITLE));
		String content = cursor.getString(cursor
				.getColumnIndex(ItemColumns.CONTENT));
		cursor.close();

		// set title:
		setTitle(title);

		TextView contentView = (TextView) findViewById(R.id.content);
		contentView.setText(Html.fromHtml(content));

		service = startService(new Intent(this, PodcastService.class));

		// bind service:
		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		
		FeedItem feed_item = FeedItem.getById(getContentResolver(), Integer
				.parseInt(item_id));
		Button play_btn = (Button) findViewById(R.id.ButtonPlay);
		Button download_btn = (Button) findViewById(R.id.ButtonDownload);	
		
		play_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				play(Integer.parseInt(item_id));

			}
		});
		
		download_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				ContentValues cv = new ContentValues();

				cv.put(ItemColumns.STATUS, ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE);
				getContentResolver().update(ItemColumns.URI, cv, "_ID=?",
						new String[] { item_id });
				serviceBinder.start_download();

			}
		});		
		
		if (feed_item.status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW) {
			download_btn.setEnabled(true);

		} else {
			download_btn.setEnabled(false);
		}	
		
		if (feed_item.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW) {
			play_btn.setEnabled(true);

		} else {
			play_btn.setEnabled(false);
		}		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
		// stopService(new Intent(this, service.getClass()));
	}

	private void play(long id) {

		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, id);
		Cursor cursor = managedQuery(uri, ItemColumns.ALL_COLUMNS, null, null,
				null);
		if (cursor.moveToFirst()) {
			FeedItem item = FeedItem.getByCursor(cursor);
			cursor.close();

			if ((item != null)
					&& (item.status == ItemColumns.ITEM_STATUS_NO_PLAY)) {
				item.status = ItemColumns.ITEM_STATUS_PLAYED;
				item.update(getContentResolver());
			}
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

			Uri data = Uri.parse(item.uri);

			intent.setDataAndType(data, item.getType());
			log.debug("palying " + item.pathname);
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
				Intent intent2 = new Intent(android.content.Intent.ACTION_VIEW);

				data = Uri.parse("file://" + item.pathname);
				intent2.setDataAndType(data, "audio/mp3");
				try {
					startActivity(intent2);

				} catch (Exception e2) {
					e2.printStackTrace();

				}

			}
		}

		if (cursor != null)
			cursor.close();

	}
}
