package info.xuluan.podcast;

import java.text.SimpleDateFormat;
import java.util.Date;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

public class ReadActivity extends Activity {

	private static final int MENU_SHARE = Menu.FIRST + 1;

	static final int MENU_DOWNLOAD = Menu.FIRST + 2;

	String item_id;
	FeedItem mItem;

	private final Log log = Log.getLog(getClass());
	//private String url = null;

	ComponentName service = null;

	private PodcastService serviceBinder = null;

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	private String getTimeString(long time){
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm");
		Date date = new Date(time);
		return  formatter.format(date);
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
		
		mItem = FeedItem.getByCursor(cursor);
		if(mItem==null){
			finish();
			return;
		}
		item_id = cursor.getString(cursor.getColumnIndex(ItemColumns._ID));

		String title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.TITLE));

		String content = cursor.getString(cursor
				.getColumnIndex(ItemColumns.CONTENT));
		Long time = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CREATED));		
		cursor.close();

		// set title:
		setTitle(title);

		TextView contentView = (TextView) findViewById(R.id.content);
		
		contentView.setText(Html.fromHtml(content));

		TextView timeView = (TextView) findViewById(R.id.time_view);
		timeView.setText("at "+getTimeString(time));
		
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
				FeedItem item = FeedItem.getById(getContentResolver(), Integer
						.parseInt(item_id));

				if(item==null)
					return;
				item.play(ReadActivity.this);


			}
		});
		
		download_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(ReadActivity.this, getResources().getString(R.string.download_hint),
						Toast.LENGTH_SHORT).show();
				
				ContentValues cv = new ContentValues();

				cv.put(ItemColumns.STATUS, ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE);
				getContentResolver().update(ItemColumns.URI, cv, "_ID=?",
						new String[] { item_id });
				
				Button btn = (Button) findViewById(R.id.ButtonDownload);	
				btn.setEnabled(false);
				
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
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_SHARE, 0,
				getResources().getString(R.string.menu_share)).setIcon(
				android.R.drawable.ic_menu_share);
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_SHARE:
			mItem.sendMail(this);
			return true;			

		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
		// stopService(new Intent(this, service.getClass()));
	}
}
