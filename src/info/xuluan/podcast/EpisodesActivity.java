package info.xuluan.podcast;

import info.xuluan.podcastj.R;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;

import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class EpisodesActivity extends PodcastBaseActivity implements PodcastTab {

	private static final int MENU_ITEM_VIEW_CHANNEL = Menu.FIRST + 8;
	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;
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
			ItemColumns.KEEP

	};

	private long pref_order;
	private long pref_where;
	private long pref_select;
	/*
	private long pref_select_bits;	//bitmask of which status values to display
		private static long pref_select_bits_new = 1<<0;	//new or viewed
		private static long pref_select_bits_download = 1<<1; //being downloaded
		private static long pref_select_bits_unplayed = 1<<2; //downloaded, not in playlist
		private static long pref_select_bits_inplay = 1<<3;	//in playlist, play, pause
		private static long pref_select_bits_done = 1<<4; //done being played
		private static long pref_select_bits_all = -1;	//all bits set
	 */
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.episodes_activity);
		setTitle(getResources().getString(R.string.title_episodes));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		getPref();

		TabsHelper.setEpisodeTabClickListeners(this, R.id.episode_bar_library_button);

		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.episodes_activity, menu);
        return true;
	}

	/*
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
    */
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.refresh:
			mServiceBinder.start_update();
			return true;
		case R.id.sort:
			 new AlertDialog.Builder(this)
             .setTitle("Chose Sort Mode")
             .setSingleChoiceItems(R.array.sort_select, (int) pref_order, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int select) {
      			
         			if(mCursor!=null)
         				mCursor.close();
         			
         			pref_order = select;
         			SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
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
		case R.id.select:
			 new AlertDialog.Builder(this)
            .setTitle("Chose Select Mode")
            .setSingleChoiceItems(R.array.select_select, (int) pref_select, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int select) {
     			
        			if(mCursor!=null)
        				mCursor.close();
        			
        			pref_select = select;
        			SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
	   				Editor prefsPrivateEditor = prefsPrivate.edit();
	   				prefsPrivateEditor.putLong("pref_select", pref_select);
	   				prefsPrivateEditor.commit();

        			mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());
        			mAdapter.changeCursor(mCursor);
        			//setListAdapter(mAdapter);         			
        			dialog.dismiss();
                }
            })
           .show();
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
		
		dialog_menu.addMenu(MENU_ITEM_DETAILS, 
				getResources().getString(R.string.menu_details));
		dialog_menu.addMenu(MENU_ITEM_VIEW_CHANNEL, 
				getResources().getString(R.string.menu_view_channel));
		
		if(feed_item.status<ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
			dialog_menu.addMenu(MENU_ITEM_START_DOWNLOAD, 
					getResources().getString(R.string.menu_download));
		} else if (feed_item.status>=ItemColumns.ITEM_STATUS_DELETED) {
			//TODO: add command to reset to new status
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
    		case MENU_ITEM_DETAILS: {
    			FeedItem.view(EpisodesActivity.this, item_id);
    			return;
    		} 
    		case MENU_ITEM_VIEW_CHANNEL: {
    			FeedItem.viewChannel(EpisodesActivity.this, item_id);
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
				FeedItem.play(EpisodesActivity.this, item_id);
				return;
			}
			case MENU_ITEM_ADD_TO_PLAYLIST: {
				FeedItem.addToPlaylist(EpisodesActivity.this, item_id);
				return;
			}
    		}
		}        	
        }
        


	@Override
	public void startInit() {

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, getWhere(), null, getOrder());

		mAdapter = EpisodeIcons.listItemCursorAdapter(this, mCursor);
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
		switch ((int)pref_select) {
		case 1:		// New only
			where =  ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_READING_VIEW;
			break;
		case 2:		// Unplayed only
			where =  ItemColumns.STATUS + "=" + ItemColumns.ITEM_STATUS_NO_PLAY;
			break;
		case 3:		// Playable only
			where = "(" + where + ") AND (" + 
					ItemColumns.STATUS + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + ")";
			break;
		case 4:		// All Plus Deleted
			where = "1 = 1";
			break;
		default:	// case 0 = All, no change to initial where clause
			;	// treat any unknown values as "All"
		}
		return where;
	}	
	
	public void getPref() {
		SharedPreferences pref = getSharedPreferences(
				Pref.HAPI_PREFS_FILE_NAME, Service.MODE_PRIVATE);
		pref_order = pref.getLong("pref_order",2);
		pref_where = pref.getLong("pref_where", 0);
		pref_select = pref.getLong("pref_select", 0);
	}
	
	//PodcastTab interface
	public int iconResource() { return R.drawable.playlist_big_pic; }
	public int tabLabelResource(boolean isLandscape) { return R.string.episode_bar_button_library; }
}
