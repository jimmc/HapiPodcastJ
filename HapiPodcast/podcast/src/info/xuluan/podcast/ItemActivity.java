package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ItemActivity extends HapiActivity {

	private FeedItem mItem;
	private Button play_btn;
	private Button download_btn;

	private final Log log = Log.getLog(getClass());
	//private String url = null;

	private static long ONE_HOUR = 1000L * 60L * 60L;
	private static long ONE_DAY = ONE_HOUR * 24L;

	private long pref_item_expire = 0;
	private long pref_download_file_expire = 0;
	private long pref_played_file_expire = 0;
	
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

	private void loadPrefs() {
		SharedPreferences pref = getSharedPreferences(
				Pref.HAPI_PREFS_FILE_NAME, Service.MODE_PRIVATE);

		pref_item_expire = Integer.parseInt(pref.getString("pref_item_expire",
				"7"));
		pref_item_expire *= ONE_DAY;
		pref_download_file_expire = Integer.parseInt(pref.getString(
				"pref_download_file_expire", "7"));
		pref_download_file_expire *= ONE_DAY;
		pref_played_file_expire = Integer.parseInt(pref.getString(
				"pref_played_file_expire", "24"));
		pref_played_file_expire *= ONE_HOUR;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read);

		loadPrefs();
		mItem = getFeedItem();
		if (mItem==null){
			finish();
			return;
		}
		
		// set title:
		setTitle(mItem.title);

		TextView contentView = (TextView) findViewById(R.id.content);		
		contentView.setText(Html.fromHtml(mItem.content));

		TextView channelView = (TextView) findViewById(R.id.channel_view);
		channelView.setText(mItem.sub_title);
		
		TextView fullTitleView = (TextView) findViewById(R.id.full_title_view);
		fullTitleView.setText(mItem.title);
		
		TextView timeView = (TextView) findViewById(R.id.time_view);
		timeView.setText("at "+getTimeString(mItem.created));
		
		TextView durationView = (TextView) findViewById(R.id.duration_view);
		durationView.setText(mItem.duration);

		setStatusIcons();
		
		service = startService(new Intent(this, PodcastService.class));

		// bind service:
		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);

		int daysUntilExpire = daysUntilExpire(mItem.created, mItem.update, mItem.status);
		TextView expView = (TextView) findViewById(R.id.exp_view);
		String expText = "exp days:"+Integer.toString(daysUntilExpire);
		if (mItem.keep!=0)
			expText = "("+expText+")";
		expView.setText(expText);
		
		play_btn = (Button) findViewById(R.id.ButtonPlay);
		download_btn = (Button) findViewById(R.id.ButtonDownload);	
		
		play_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mItem.play(ItemActivity.this);
			}
		});
		
		download_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Toast.makeText(ItemActivity.this, getResources().getString(R.string.download_hint),
						Toast.LENGTH_SHORT).show();
				
				ContentValues cv = new ContentValues();

				cv.put(ItemColumns.STATUS, ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE);
				getContentResolver().update(ItemColumns.URI, cv, "_ID=?",
						new String[] { Long.toString(mItem.id) });
				
				Button btn = (Button) findViewById(R.id.ButtonDownload);	
				btn.setEnabled(false);
				
				serviceBinder.start_download();
				mItem = getFeedItem();
				setStatusIcons();
			}
		});		

		enableButtons();
	}

	@Override
	public void onResume() {
		super.onResume();
		mItem = getFeedItem();
		setStatusIcons();
	}

	private FeedItem getFeedItem() {
		Intent intent = getIntent();

		Uri uri = intent.getData();
		Cursor cursor = getContentResolver().query(uri,
				ItemColumns.ALL_COLUMNS, null, null, null);
		if (!cursor.moveToFirst()) {
			// show404();
			return null;
		}		
		return FeedItem.getByCursor(cursor);
	}
	
	private void setStatusIcons() {
		int icon = AllItemActivity.mapToIcon(mItem.status);
		ImageView iconView = (ImageView) findViewById(R.id.status_icon);
		iconView.setImageResource(icon);
		
		ImageView keepIconView = (ImageView) findViewById(R.id.keep_icon);
		if (mItem.keep!=0) {
			keepIconView.setImageResource(R.drawable.keep);
		} else {
			keepIconView.setImageResource(R.drawable.blank);
		}
	}
	
	private void enableButtons() {
		if (mItem.status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW) {
			download_btn.setEnabled(true);
		} else {
			download_btn.setEnabled(false);
		}	
		
		if (mItem.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW) {
			play_btn.setEnabled(true);
		} else {
			play_btn.setEnabled(false);
		}		

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.item_activity, menu);
        return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		mItem = getFeedItem();
        setMenuItemsVisibility(menu);
        setStatusIcons();
        enableButtons();
		return true;
	}

	private void setMenuItemsVisibility(Menu menu) {
		boolean isDownloaded =
				(mItem.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW);
    	menu.findItem(R.id.add_to_playlist).setVisible(isDownloaded);
    	menu.findItem(R.id.played_by).setVisible(isDownloaded);
    	menu.findItem(R.id.export).setVisible(isDownloaded);
    	menu.findItem(R.id.mark_new).setVisible(isDownloaded);
    	menu.findItem(R.id.keep).setVisible(mItem.keep==0);
    	menu.findItem(R.id.unkeep).setVisible(mItem.keep!=0);   
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.view_channel:
			mItem.viewChannel(this);
			return true;
		case R.id.add_to_playlist:
			mItem.addtoPlaylist(getContentResolver());
			Toast.makeText(ItemActivity.this,
					getResources().getString(R.string.toast_added_to_playlist),
					Toast.LENGTH_SHORT).show();
			setStatusIcons();
			return true;
		case R.id.played_by:
			mItem.playedBy(this);
	        return true;
		case R.id.share:
			mItem.sendMail(this);
			return true;
		case R.id.export:
			mItem.export(this);
			return true;
		case R.id.keep:
			mItem.markKeep(getContentResolver());
			setStatusIcons();
			return true;
		case R.id.unkeep:
			mItem.markUnkeep(getContentResolver());
			setStatusIcons();
			return true;
		case R.id.mark_new:
			mItem.markNew(this.getContentResolver());
			setStatusIcons();
			return true;
		case R.id.delete:
  			mItem.delFile(getContentResolver()); 		  
			setStatusIcons();
			finish();
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

	private int daysUntilExpire(long created, long updated, int status) {
		long expire_duration = 0;
		long item_time = updated;
		if (status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW) {
			item_time = created;
			expire_duration = pref_item_expire;
		} else if (status <= ItemColumns.ITEM_STATUS_PLAY_PAUSE) {
			expire_duration = pref_download_file_expire;
		} else if (status < ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW) {
			expire_duration = pref_played_file_expire;
		}
		long expire_time = item_time + expire_duration;
		long time_until_expire = expire_time - System.currentTimeMillis();
		if (time_until_expire < 0)
			return 0;	//already expired
		return (int)(time_until_expire / ONE_DAY);
	}

}
