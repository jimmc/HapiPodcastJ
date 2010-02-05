package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.content.ContentUris;
import android.content.Intent;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.HashMap;

public class MainActivity extends PodcastBaseActivity {

	private static final int MENU_REFRESH = Menu.FIRST + 1;
	private static final int MENU_SUBS = Menu.FIRST + 2;
	private static final int MENU_PREF = Menu.FIRST + 3;
	private static final int MENU_DOWNLOADING = Menu.FIRST + 4;
	private static final int MENU_DOWNLOADED = Menu.FIRST + 5;
	
	private static final int MENU_SEARCH = Menu.FIRST + 6;
	

	private static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST + 10;

	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS, // 1

	};

	private static HashMap<Integer, Integer> mIconMap;

	static {

		mIconMap = new HashMap<Integer, Integer>();
		mIconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.new_item);
		mIconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.open_item);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		setTitle(getResources().getString(R.string.app_name));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REFRESH, 0,
				getResources().getString(R.string.menu_refresh)).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_SEARCH, 1, "Search")
		.setIcon(android.R.drawable.ic_menu_search);		
		menu.add(0, MENU_SUBS, 2, getResources().getString(R.string.menu_subs))
				.setIcon(android.R.drawable.ic_menu_agenda);
		menu.add(0, MENU_PREF, 3, getResources().getString(R.string.menu_pref))
				.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_DOWNLOADING, 4,
				getResources().getString(R.string.menu_downloading)).setIcon(
				android.R.drawable.ic_menu_set_as);
		menu.add(0, MENU_DOWNLOADED, 5,
				getResources().getString(R.string.menu_play_list)).setIcon(
				android.R.drawable.ic_menu_slideshow);
		
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case MENU_REFRESH:
			mServiceBinder.start_update();
			return true;
		case MENU_SUBS:
			startActivity(new Intent(this, SubsActivity.class));
			return true;

		case MENU_PREF:
			startActivity(new Intent(this, Pref.class));
			return true;
		case MENU_DOWNLOADING:
			intent = new Intent(this, DownloadingActivity.class);
			startActivity(intent);
			return true;
		case MENU_DOWNLOADED:
			intent = new Intent(this, PlayListActivity.class);
			startActivity(intent);
			return true;

		case MENU_SEARCH:
			intent = new Intent(this, AddChannel.class);
			startActivity(intent);
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

		// Setup the menu header
		menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

		// Add a menu item to delete the note
		menu.add(0, MENU_ITEM_START_DOWNLOAD, 0, R.string.menu_download);

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
		}
		return false;
	}

	@Override
	public void startInit() {

		String where = ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW;

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, null);

		mAdapter = new IconCursorAdapter(this, R.layout.list_item, mCursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS }, new int[] {
						R.id.text1, R.id.text2, R.id.text3 }, mIconMap);
		setListAdapter(mAdapter);

		super.startInit();

	}
}
