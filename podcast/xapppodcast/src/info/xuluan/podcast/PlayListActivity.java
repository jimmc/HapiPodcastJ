package info.xuluan.podcast;

import java.io.File;
import java.util.HashMap;
import android.view.ContextMenu.ContextMenuInfo;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		FeedItem item = FeedItem.getById(getContentResolver(), id);
		if (item == null)
			return;
		item.play(this);

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
		menu.add(0, MENU_ITEM_PLAY, 0, R.string.menu_play);
		menu.add(0, MENU_ITEM_KEEP, 0, R.string.menu_keep);
		menu.add(0, MENU_ITEM_VIEW, 0, R.string.menu_view);
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
			// Delete the note that the context menu is for
			FeedItem feed_item = FeedItem
					.getById(getContentResolver(), info.id);
			if (feed_item == null)
				return true;
			/*
			 * if(feed_item.status == ItemColumns.ITEM_STATUS_KEEP){
			 * Toast.makeText(this, "The item status is KEEP!",
			 * Toast.LENGTH_SHORT).show(); return true; }
			 */
			// TODO are you sure?
			
			feed_item.delFile(getContentResolver());
			return true;
		}
		case MENU_ITEM_PLAY: {
			FeedItem play_item = FeedItem.getById(getContentResolver(), info.id);
			if(play_item==null)
				return true;
			play_item.play(this);
			return true;
		}
		case MENU_ITEM_KEEP: {
			FeedItem feed_item = FeedItem
					.getById(getContentResolver(), info.id);
			if ((feed_item != null)
					&& (feed_item.status != ItemColumns.ITEM_STATUS_KEEP)) {
				feed_item.status = ItemColumns.ITEM_STATUS_KEEP;
				feed_item.update(getContentResolver());
			}
			return true;
		}
		case MENU_ITEM_VIEW: {
			Uri uri = ContentUris
					.withAppendedId(getIntent().getData(), info.id);
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
			return true;
		}
		}
		return false;
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
