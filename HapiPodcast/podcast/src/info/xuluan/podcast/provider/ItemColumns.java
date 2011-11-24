package info.xuluan.podcast.provider;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;

public class ItemColumns implements BaseColumns {

	public static final int ITEM_STATUS_UNREAD = 0;
	public static final int ITEM_STATUS_READ = 1;
	public static final int ITEM_STATUS_MAX_READING_VIEW = 10;
	public static final int ITEM_STATUS_DOWNLOAD_PAUSE = 15;
	public static final int ITEM_STATUS_DOWNLOAD_QUEUE = 20;
	public static final int ITEM_STATUS_DOWNLOADING_NOW = 21;

	public static final int ITEM_STATUS_MAX_DOWNLOADING_VIEW = 30;
	public static final int ITEM_STATUS_NO_PLAY = 50;
	public static final int ITEM_STATUS_START_PLAY = 51;

	public static final int ITEM_STATUS_KEEP = 63;
	public static final int ITEM_STATUS_PLAYED = 66;
	public static final int ITEM_STATUS_MAX_PLAYLIST_VIEW = 100;
	
	public static final int ITEM_STATUS_MIN_DELETE = 190;
	public static final int ITEM_STATUS_DELETE = 195;	
	public static final int ITEM_STATUS_DELETED = 200;

	public static final Uri URI = Uri.parse("content://"
			+ PodcastProvider.AUTHORITY + "/items");

	public static final String TABLE_NAME = "item";

	// feed
	public static final String SUBS_ID = "subs_id";

	public static final String TITLE = "title";

	public static final String AUTHOR = "author";

	public static final String DATE = "date";

	public static final String LAST_UPDATE = "last_update";

	public static final String CONTENT = "content";

	// download
	public static final String STATUS = "status";

	public static final String URL = "url";

	public static final String RESOURCE = "res";

	public static final String DURATION = "duration";

	public static final String LENGTH = "length";

	public static final String OFFSET = "offset";

	public static final String PATHNAME = "path";

	public static final String FAIL_COUNT = "fail";

	// play
	public static final String MEDIA_URI = "uri";

	public static final String SUB_TITLE = "sub_title";
	public static final String CREATED = "created";
	public static final String TYPE = "audio_type";

	public static final String[] ALL_COLUMNS = { _ID, SUBS_ID, TITLE, AUTHOR,
			DATE, LAST_UPDATE, CONTENT, STATUS, URL, RESOURCE, DURATION,
			LENGTH, OFFSET, PATHNAME, FAIL_COUNT, MEDIA_URI, SUB_TITLE,
			CREATED, TYPE };

	public static final String DEFAULT_SORT_ORDER = CREATED + " DESC";

	public static final String sql_create_table = "CREATE TABLE " 
		+ TABLE_NAME + " (" 
		+ _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
		+ SUBS_ID + " INTEGER, " 
		+ TITLE + " VARCHAR(128), " 
		+ AUTHOR + " VARCHAR(128), " 
		+ DATE + " VARCHAR(64), " 
		+ LAST_UPDATE + " INTEGER, " 
		+ CONTENT + " TEXT, " 
		+ STATUS + " INTEGER, " 
		+ URL + " VARCHAR(1024), " 
		+ RESOURCE + " VARCHAR(1024), " 
		+ DURATION + " VARCHAR(16), " 
		+ LENGTH + " INTEGER, " 
		+ OFFSET + " INTEGER, "
		+ PATHNAME + " VARCHAR(128), " 
		+ FAIL_COUNT + " INTEGER, "
		+ MEDIA_URI + " VARCHAR(128), " 
		+ SUB_TITLE + " VARCHAR(128), "
		+ TYPE + " VARCHAR(64), " 
		+ CREATED + " INTEGER " 
		+ ");";

	public static final String sql_index_item_res = "CREATE INDEX IDX_"
			+ TABLE_NAME + "_" + RESOURCE + " ON " + TABLE_NAME + " ("
			+ RESOURCE + ");";

	public static final String sql_index_item_created = "CREATE INDEX IDX_"
			+ TABLE_NAME + "_" + LAST_UPDATE + " ON " + TABLE_NAME + " ("
			+ LAST_UPDATE + ");";

	public static ContentValues checkValues(ContentValues values, Uri uri) {
		if (values.containsKey(SUBS_ID) == false) {
			throw new SQLException(
					"Fail to insert row because SUBS_ID is needed " + uri);
		}

		if (values.containsKey(URL) == false) {
			values.put(URL, "");
		}

		if (values.containsKey(TITLE) == false) {
			values.put(TITLE, "unknow");
		}

		if (values.containsKey(AUTHOR) == false) {
			values.put(AUTHOR, "");
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		if (values.containsKey(LAST_UPDATE) == false) {
			values.put(LAST_UPDATE, now);
		}

		if (values.containsKey(DATE) == false) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss Z");
			Date currentTime = new Date();
			values.put(DATE, formatter.format(currentTime));
		}

		if (values.containsKey(CONTENT) == false) {
			values.put(CONTENT, "");
		}

		if (values.containsKey(STATUS) == false) {
			values.put(STATUS, ITEM_STATUS_UNREAD);
		}

		if (values.containsKey(RESOURCE) == false) {
			throw new SQLException(
					"Fail to insert row because RESOURCE is needed " + uri);
		}

		if (values.containsKey(DURATION) == false) {
			values.put(DURATION, "");
		}

		if (values.containsKey(LENGTH) == false) {
			values.put(LENGTH, 0);
		}

		if (values.containsKey(OFFSET) == false) {
			values.put(OFFSET, 0);
		}

		if (values.containsKey(PATHNAME) == false) {
			values.put(PATHNAME, "");
		}

		if (values.containsKey(FAIL_COUNT) == false) {
			values.put(FAIL_COUNT, 0);
		}

		if (values.containsKey(MEDIA_URI) == false) {
			values.put(MEDIA_URI, "");
		}

		if (values.containsKey(SUB_TITLE) == false) {
			values.put(SUB_TITLE, "");
		}

		if (values.containsKey(CREATED) == false) {
			values.put(CREATED, now);
		}
		if (values.containsKey(TYPE) == false) {
			values.put(TYPE, "");
		}
		return values;
	}

}
