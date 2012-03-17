package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.DialogMenu;

import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class ChannelActivity extends PodcastBaseActivity {

	private static final int MENU_UNSUBSCRIBE = Menu.FIRST + 1;
	private static final int MENU_AUTO_DOWNLOAD = Menu.FIRST + 2;

	
	private static final int MENU_ITEM_VIEW = Menu.FIRST + 9;
	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;
	private static final int MENU_ITEM_START_PLAY = Menu.FIRST + 11;
	private static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 12;
	
	private static final String[] PROJECTION = new String[] {
		ItemColumns._ID, // 0
		ItemColumns.TITLE, // 1
		ItemColumns.DURATION,
		ItemColumns.SUB_TITLE,
		ItemColumns.STATUS,
		ItemColumns.KEEP
	};

	private static HashMap<Integer, Integer> mIconMap;
	
	Subscription mChannel = null;
	long id;

	static {

		mIconMap = new HashMap<Integer, Integer>();
		AllItemActivity.initFullIconMap(mIconMap);
/*
		mIconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.new_item);
		mIconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.open_item);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE, R.drawable.download);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE, R.drawable.download);
		mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW, R.drawable.download);
		
		mIconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.music);		
*/
	}

	public static boolean channelExists(Activity act, Uri uri) {
		Cursor cursor = act.getContentResolver().query(uri,
				SubscriptionColumns.ALL_COLUMNS, null, null, null);
		if (!cursor.moveToFirst()) {
			return false;
		}
		
		Subscription ch = Subscription.getByCursor(cursor);

		cursor.close();
		
		return (ch!=null);
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
			auto = getResources().getString(R.string.menu_auto_download);
		}else{
			auto = getResources().getString(R.string.menu_manual_download);
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
			mChannel.auto_download = 1-mChannel.auto_download;
			mChannel.update(getContentResolver());	
			return true;			

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {


			DialogMenu dialog_menu = createDialogMenus(id);
			if( dialog_menu==null)
				return;
			
			
			 new AlertDialog.Builder(this)
             .setTitle(dialog_menu.getHeader())
             .setItems(dialog_menu.getItems(), new MainClickListener(dialog_menu,id)).show();		

		}
	}
	
	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		dialog_menu.addMenu(MENU_ITEM_VIEW, 
				getResources().getString(R.string.menu_view));
		
		if(feed_item.status<ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_DOWNLOAD, 
					getResources().getString(R.string.menu_download));			
		}else if(feed_item.status>ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_PLAY, 
					getResources().getString(R.string.menu_play));
			dialog_menu.addMenu(MENU_ITEM_ADD_TO_PLAYLIST, 
					getResources().getString(R.string.menu_add_to_playlist));
		}

		return dialog_menu;
	}	

	


	class MainClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public MainClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_VIEW: {
    			Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
    			FeedItem item = FeedItem.getById(getContentResolver(), item_id);
    			if ((item != null)
    					&& (item.status == ItemColumns.ITEM_STATUS_UNREAD)) {
    				item.status = ItemColumns.ITEM_STATUS_READ;
    				item.update(getContentResolver());
    			}    			
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));   
    			return;
    		}    		
			case MENU_ITEM_START_DOWNLOAD: {
	
				FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
				if (feeditem == null)
					return;
	
				feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
				feeditem.update(getContentResolver());
				mServiceBinder.start_download();
				return;
			}
			case MENU_ITEM_START_PLAY: {
	
				FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
				if (feeditem == null)
					return;
		
				feeditem.play(ChannelActivity.this);
				return;
			}
			case MENU_ITEM_ADD_TO_PLAYLIST: {
				
				FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
				if (feeditem == null)
					return;
		
				feeditem.addtoPlaylist(getContentResolver());
				return;
			}
    		}
		}        	
       }

	@Override
	public void startInit() {

		String where = ItemColumns.SUBS_ID + "=" + mChannel.id + " AND " 
		+ ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, null);

		mAdapter = AllItemActivity.channelListItemCursorAdapter(this, mCursor);
/*		mAdapter = new IconCursorAdapter(this, R.layout.channel_list_item, mCursor,
				new String[] { ItemColumns.TITLE,ItemColumns.STATUS }, new int[] {
						R.id.text1}, mIconMap);
*/
		setListAdapter(mAdapter);

		super.startInit();

	}
}
