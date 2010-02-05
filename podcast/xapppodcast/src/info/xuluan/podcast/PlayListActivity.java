package info.xuluan.podcast;

import java.io.File;
import java.util.HashMap;
import android.view.ContextMenu.ContextMenuInfo;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.IconCursorAdapter;

import android.content.ActivityNotFoundException;
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// menu.add(0, MENU_REFRESH, 0,
		// getResources().getString(R.string.menu_refresh)).setIcon(android.R.drawable.ic_menu_more);
		menu.add(0, MENU_PREF, 1, getResources().getString(R.string.menu_pref))
				.setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, MENU_BACK, 2, getResources().getString(R.string.menu_back))
				.setIcon(android.R.drawable.ic_menu_revert);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_BACK:
			finish();
			return true;

		case MENU_PREF:
			startActivity(new Intent(this, Pref.class));
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		play(id);

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setTitle("Play List");

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		startInit();
	}

	private void play(long id) {

		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
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
			// Intent intent = new Intent("com.android.music.PLAYBACK_VIEWER");
			// intent.setComponent(new
			// ComponentName("com.android.music","com.android.music.MediaPlaybackActivity"));
			// intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			// getContentResolver().query("content://media/external/audio/media"
			// ItemColumns.ALL_COLUMNS, where, null, null);
			// Uri data = Media.getContentUriForPath("file://"+item.pathname);
			// log.warn("Uri =  " + data);
			// Uri data = Uri.parse("file://"+item.pathname);
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
			try {
				File file = new File(feed_item.pathname);
				boolean deleted = file.delete();

			} catch (Exception e) {
				log.warn("del file failed : " + feed_item.pathname + "  " + e);

			}
			Uri delUri = ContentUris.withAppendedId(getIntent().getData(),
					info.id);
			getContentResolver().delete(delUri, null, null);
			return true;
		}
		case MENU_ITEM_PLAY: {
			play(info.id);
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
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW;
		String order = ItemColumns.STATUS + " ASC, " + ItemColumns.CREATED
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
