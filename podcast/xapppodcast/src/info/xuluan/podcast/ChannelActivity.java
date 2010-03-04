package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class ChannelActivity extends PodcastBaseActivity {

	private static final int MENU_UNSUBSCRIBE = Menu.FIRST + 1;
	private static final int MENU_AUTO_DOWNLOAD = Menu.FIRST + 2;

	

	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS, // 1

	};

	private static HashMap<Integer, Integer> mIconMap;
	
	Subscription mChannel = null;
	long id;

	static {

		mIconMap = new HashMap<Integer, Integer>();
		mIconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.new_item);
		mIconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.open_item);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE, R.drawable.download);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE, R.drawable.download);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW, R.drawable.download);
		
		mIconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.music);		

	}

	private String getTimeString(long time){
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm");
		Date date = new Date(time);
		return  formatter.format(date);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel);

		Intent intent = getIntent();

		Uri uri = intent.getData();
		
		Cursor cursor = getContentResolver().query(uri,
				SubscriptionColumns.ALL_COLUMNS, null, null, null);
		if (!cursor.moveToFirst()) {
			return;
		}
		
		mChannel = Subscription.getByCursor(cursor);

		cursor.close();
		
		if(mChannel==null){
			finish();
			return;
		}
		setTitle(mChannel.title);

		getListView().setOnCreateContextMenuListener(this);

		
		mPrevIntent = null;
		mNextIntent = null;
		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_UNSUBSCRIBE, 0,
				getResources().getString(R.string.unsubscribe)).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		

		
		menu.add(0, MENU_AUTO_DOWNLOAD, 0,"Auto Download").setIcon(
				android.R.drawable.ic_menu_set_as);
		
	
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_AUTO_DOWNLOAD);
		String auto;
		if(mChannel.auto_download==0){
			auto = "Auto Download";
		}else{
			auto = "Manual Download";
		}        
        item.setTitle(auto);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_UNSUBSCRIBE:
		
		new AlertDialog.Builder(ChannelActivity.this)
                .setTitle(R.string.unsubscribe_channel)
                .setPositiveButton(R.string.menu_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
							mChannel.delete(getContentResolver());	
							finish();
							dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.menu_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
			return true;
		case MENU_AUTO_DOWNLOAD:
			mChannel.auto_download= 1-mChannel.auto_download;
			mChannel.update(getContentResolver());	
			return true;			

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, id);
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
		
		
		FeedItem item = FeedItem.getById(getContentResolver(), cursor.getInt(0));
		if(item==null)
			return;
		// Setup the menu header
		menu.setHeaderTitle(item.title);
		if(item.status<ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
			// Add a menu item to delete the note
			menu.add(0, MENU_ITEM_START_DOWNLOAD, 0, R.string.menu_download);		
		}else if(item.status>ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
			menu.add(0, MENU_ITEM_START_PLAY, 0, R.string.menu_play);			
		}


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
			case MENU_ITEM_START_DOWNLOAD: {
	
				FeedItem feeditem = FeedItem.getById(getContentResolver(), info.id);
				if (feeditem == null)
					return true;
	
				feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
				feeditem.update(getContentResolver());
				mServiceBinder.start_download();
				return true;
			}
			case MENU_ITEM_START_PLAY: {
	
				FeedItem feeditem = FeedItem.getById(getContentResolver(), info.id);
				if (feeditem == null)
					return true;
		
				feeditem.play(this);
				return true;
			}		
		}
		return false;
	}

	@Override
	public void startInit() {

		String where = ItemColumns.SUBS_ID + "=" + mChannel.id;

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, null);

		mAdapter = new IconCursorAdapter(this, R.layout.channel_list_item, mCursor,
				new String[] { ItemColumns.TITLE,ItemColumns.STATUS }, new int[] {
						R.id.text1}, mIconMap);
		setListAdapter(mAdapter);

		super.startInit();

	}
}
