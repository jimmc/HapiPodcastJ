package info.xuluan.podcast;

import java.util.HashMap;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;

public class PlayListActivity extends PodcastBaseActivity {

	private static HashMap<Integer, Integer> mIconMap;
	static {

		mIconMap = new HashMap<Integer, Integer>();
		mIconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.no_play);
		mIconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		mIconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);
		
	}

	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS, // 1
	};

	// static final int MENU_REFRESH = Menu.FIRST + 1;
	static final int MENU_BACK = Menu.FIRST + 2;
	static final int MENU_PREF = Menu.FIRST + 3;

	public static final int MENU_ITEM_PLAY = Menu.FIRST + 10;
	public static final int MENU_ITEM_KEEP = Menu.FIRST + 11;
	public static final int MENU_ITEM_VIEW = Menu.FIRST + 12;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 13;
	public static final int MENU_ITEM_SHARE = Menu.FIRST + 14;
	
	


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DialogMenu dialog_menu = createDialogMenus(id);
		if( dialog_menu==null)
			return;
		
		
		 new AlertDialog.Builder(this)
         .setTitle(dialog_menu.getHeader())
         .setItems(dialog_menu.getItems(), new EpisodeClickListener(dialog_menu,id)).show();		


	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setTitle(getResources().getString(R.string.title_play_list));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		mPrevIntent = new Intent(this, DownloadingActivity.class);
		mNextIntent = new Intent(this, SearchChannel.class);		
		startInit();
	}

	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		dialog_menu.addMenu(MENU_ITEM_PLAY,
				getResources().getString(R.string.menu_play));
		
		if(feed_item.status!=ItemColumns.ITEM_STATUS_KEEP){
			dialog_menu.addMenu(MENU_ITEM_KEEP, 
					getResources().getString(R.string.menu_keep));			
		}		

		dialog_menu.addMenu(MENU_ITEM_SHARE,
				getResources().getString(R.string.menu_share));	
		
		dialog_menu.addMenu(MENU_ITEM_VIEW,
				getResources().getString(R.string.menu_view));
		
		dialog_menu.addMenu(MENU_ITEM_DELETE,
				getResources().getString(R.string.menu_delete));	

		return dialog_menu;
	}


	class EpisodeClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public EpisodeClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		FeedItem select_item = FeedItem.getById(getContentResolver(), item_id);
    		if(select_item==null)
    			return;
    		
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_DELETE: {

    			// TODO are you sure?
    			
    			select_item.delFile(PlayListActivity.this.getContentResolver());
    			return;
    		}
    		case MENU_ITEM_PLAY: {
    			select_item.play(PlayListActivity.this);
    			return;
    		}
    		case MENU_ITEM_KEEP: {
    			if (select_item.status != ItemColumns.ITEM_STATUS_KEEP) {
    				select_item.status = ItemColumns.ITEM_STATUS_KEEP;
    				select_item.update(PlayListActivity.this.getContentResolver());
    			}
    			return;
    		}
    		case MENU_ITEM_VIEW: {
    			Uri uri = ContentUris
    					.withAppendedId(getIntent().getData(), select_item.id);
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));
    			return;
    		}
    		case MENU_ITEM_SHARE: {
    			select_item.sendMail(PlayListActivity.this);
    			return;
    		}		
    		}
		}        	
       }


	public void startInit() {
		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND " 
		+ ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
		
		String order = ItemColumns.STATUS + " ASC, " + ItemColumns.LAST_UPDATE
				+ " DESC";

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, order);

		// Used to map notes entries from the database to views
		mAdapter = new IconCursorAdapter(this, R.layout.list_item, mCursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS }, new int[] {
						R.id.text1, R.id.text2, R.id.text3 }, mIconMap);

		setListAdapter(mAdapter);

		super.startInit();
	}

}
