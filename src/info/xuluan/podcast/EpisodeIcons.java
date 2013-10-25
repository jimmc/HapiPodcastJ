package info.xuluan.podcast;

import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.IconCursorAdapter;
import info.xuluan.podcastj.R;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;

public class EpisodeIcons {

	private static HashMap<Integer, Integer> mIconMap;
	public static HashMap<Integer, Integer> mKeepIconMap;
	
	static {
		mIconMap = new HashMap<Integer, Integer>();
		initFullIconMap(mIconMap);
		
		mKeepIconMap = new HashMap<Integer, Integer>();
		mKeepIconMap.put(1, R.drawable.keep);		
		mKeepIconMap.put(IconCursorAdapter.ICON_DEFAULT_ID, R.drawable.blank);	 //anything other than KEEP	
	}
	
	public static void initFullIconMap(HashMap<Integer,Integer> iconMap) {
		//green
		iconMap.put(ItemColumns.ITEM_STATUS_UNREAD, R.drawable.feed_new);
		iconMap.put(ItemColumns.ITEM_STATUS_READ, R.drawable.feed_viewed);

		//blue
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE, R.drawable.download_pause);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE, R.drawable.download_wait);
		iconMap.put(ItemColumns.ITEM_STATUS_DOWNLOADING_NOW, R.drawable.downloading);

		//orange
		iconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.playable);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_READY, R.drawable.play_ready);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYING_NOW, R.drawable.playing);
		iconMap.put(ItemColumns.ITEM_STATUS_PLAY_PAUSE, R.drawable.play_pause);
		
		//red
		iconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		//iconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);
			//we now show KEEP status with a separate icon, based on separate DB flag

		//red
		iconMap.put(ItemColumns.ITEM_STATUS_DELETE, R.drawable.delete);
		iconMap.put(ItemColumns.ITEM_STATUS_DELETED, R.drawable.deleted);

		iconMap.put(IconCursorAdapter.ICON_DEFAULT_ID, R.drawable.status_unknown);		//default for unknowns
	}
	
	public static IconCursorAdapter listItemCursorAdapter(Context context, Cursor cursor) {
		IconCursorAdapter.FieldHandler[] fields = {
				IconCursorAdapter.defaultTextFieldHandler,
				IconCursorAdapter.defaultTextFieldHandler,
				IconCursorAdapter.defaultTextFieldHandler,
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new IconCursorAdapter(context, R.layout.list_item, cursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS, ItemColumns.KEEP },
				new int[] { R.id.text1, R.id.text2, R.id.text3, R.id.icon, R.id.keep_icon },
				fields);
	}

	public static IconCursorAdapter channelListItemCursorAdapter(Context context, Cursor cursor) {
		IconCursorAdapter.FieldHandler[] fields = {
				IconCursorAdapter.defaultTextFieldHandler,
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(mKeepIconMap)
		};
		return new IconCursorAdapter(context, R.layout.channel_list_item, cursor,
				new String[] { ItemColumns.TITLE, ItemColumns.STATUS, ItemColumns.KEEP },
				new int[] { R.id.text1, R.id.icon, R.id.keep_icon },
				fields);
	}

	public static int mapToIcon(int status) {
		return mapToIcon(status,mIconMap);
	}
	
	public static int mapToIcon(int key, HashMap<Integer,Integer> iconMap) {
		Integer iconI = iconMap.get(key);
		if (iconI==null)
			iconI = iconMap.get(IconCursorAdapter.ICON_DEFAULT_ID);	//look for default value in map
		int icon = (iconI!=null)?
			iconI.intValue():
			R.drawable.status_unknown;	//Use this icon when not in map and no map default.
				//This allows going back to a previous version after data has been
				//added in a new version with additional status codes.
		return icon;
	}
	
}
