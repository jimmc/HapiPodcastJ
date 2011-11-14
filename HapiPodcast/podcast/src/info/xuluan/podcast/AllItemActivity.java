package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.net.Uri;
import android.os.Bundle;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import java.util.HashMap;

public class AllItemActivity extends PodcastBaseActivity {

	private static final int MENU_REFRESH = Menu.FIRST + 1;
	private static final int MENU_SORT = Menu.FIRST + 2;
	private static final int MENU_DISPLAY = Menu.FIRST + 3;

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
			ItemColumns.SUBS_ID,

	};

	private static HashMap<Integer, Integer> mIconMap;
	
	private long pref_order;
	private long pref_where;

	static {

		mIconMap = new HashMap<Integer, Integer>();
		initFullIconMap(mIconMap);
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
	
	public static void initFullIconMap(HashMap<Integer,Integer> iconMap) {
		iconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.new_item);
		iconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.open_item);
		
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE, R.drawable.pause);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE, R.drawable.waiting);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW, R.drawable.download);
		
		iconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.no_play);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		iconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		setTitle(getResources().getString(R.string.title_read_list));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		mPrevIntent = new Intent(this, ChannelsActivity.class);
		mNextIntent = new Intent(this, DownloadingActivity.class);
		
		getPref();

		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH, 0,
				getResources().getString(R.string.menu_update)).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SORT, 1,
				getResources().getString(R.string.menu_sort)).setIcon(
				android.R.drawable.ic_menu_agenda);	
		menu.add(0, MENU_DISPLAY, 2,
				getResources().getString(R.string.menu_display)).setIcon(
				android.R.drawable.ic_menu_today);			
		return true;
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_DISPLAY);
		String auto;
		if(pref_where==0){
			auto = "Only Undownload";
		}else{
			auto = "Display All";
		}        
        item.setTitle(auto);
        return true;
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			mServiceBinder.start_update();
			return true;
		case MENU_SORT:
			 new AlertDialog.Builder(this)
             .setTitle("Chose Sort Mode")
             .setSingleChoiceItems(R.array.sort_select, (int) pref_order, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int select) {
      			
         			if(mCursor!=null)
         				mCursor.close();
         			
         			pref_order = select;
         			SharedPreferences prefsPrivate = getSharedPreferences("info.xuluan.podcast_preferences", Context.MODE_PRIVATE);
    				Editor prefsPrivateEditor = prefsPrivate.edit();
    				prefsPrivateEditor.putLong("pref_order", pref_order);
    				prefsPrivateEditor.commit();

         			mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());
         			mAdapter.changeCursor(mCursor);
         			//setListAdapter(mAdapter);         			
         			dialog.dismiss();

                 }
             })
            .show();
			return true;
		case MENU_DISPLAY:
 			if(mCursor!=null)
 				mCursor.close();
 			pref_where = 1- pref_where;

 			SharedPreferences prefsPrivate = getSharedPreferences("info.xuluan.podcast_preferences", Context.MODE_PRIVATE);
			Editor prefsPrivateEditor = prefsPrivate.edit();
			prefsPrivateEditor.putLong("pref_where", pref_where);
			prefsPrivateEditor.commit();
			
 			mCursor = managedQuery(ItemColumns.URI, PROJECTION,getWhere(), null, getOrder());
 			mAdapter.changeCursor(mCursor);
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
		
				feeditem.play(AllItemActivity.this);
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

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());

		mAdapter = new IconCursorAdapter(this, R.layout.list_item, mCursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS }, new int[] {
						R.id.text1, R.id.text2, R.id.text3 }, mIconMap);
		setListAdapter(mAdapter);

		super.startInit();

	}
	public String getOrder() {
			String order = ItemColumns.CREATED + " DESC";
 			if(pref_order==0){
 				 order = ItemColumns.SUBS_ID +"," +order;
 			}else if(pref_order==1){
				 order = ItemColumns.STATUS +"," +order;
 			}
 			return order;
	}	

	public String getWhere() {
		String where = ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
			if(pref_where!=0){
				where =  ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_READING_VIEW;
			}
			return where;
}	
	
	public void getPref() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order",2);

		pref_where = pref.getLong("pref_where", 0);
	}
}
