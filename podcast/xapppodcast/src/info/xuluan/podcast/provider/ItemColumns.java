package info.xuluan.podcast.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;

public class ItemColumns implements BaseColumns {
	
	public static final int ITEM_STATUS_UNREAD = 0;	
	public static final int ITEM_STATUS_READ = 1;	
	public static final int ITEM_STATUS_MAX_READING_VIEW = 10;	
	public static final int ITEM_STATUS_DOWNLOADING = 20;
	public static final int ITEM_STATUS_MAX_DOWNLOADING_VIEW = 30;		
	public static final int ITEM_STATUS_DOWNLOADED = 50;
	
	public static final Uri URI = Uri.parse("content://"
			+ ReadingProvider.AUTHORITY + "/items");

	public static final String TABLE_NAME = "item";

	public static final String SUBS_ID = "subs_id";

	public static final String TITLE = "title";

	public static final String AUTHOR = "author";

	public static final String DATE = "date";

	public static final String CREATED_DATE = "created";

	public static final String CONTENT = "content";

	public static final String STATUS = "status";

	public static final String URL = "url";

	public static final String RESOURCE = "res";

	public static final String DURATION = "duration";

	public static final String LENGTH = "length";

	public static final String OFFSET = "offset";

	public static final String PATHNAME = "path";

	public static final String FAIL_COUNT = "fail";

	public static final String[] ALL_COLUMNS = { _ID, SUBS_ID, TITLE,
			AUTHOR, DATE, CREATED_DATE, CONTENT, STATUS, URL, RESOURCE, DURATION,
			LENGTH, OFFSET, PATHNAME, FAIL_COUNT };
	
	public static final String DEFAULT_SORT_ORDER = CREATED_DATE + " DESC";

	public static final String sql_create_table = "CREATE TABLE " + TABLE_NAME + " (" 
            + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + SUBS_ID + " INTEGER, "
            + TITLE + " VARCHAR(256), "
            + AUTHOR + " VARCHAR(256), "
            + DATE + " VARCHAR(256), "            
            + CREATED_DATE + " INTEGER, "            
            + CONTENT + " TEXT, "
            + STATUS + " INTEGER, "
            + URL + " VARCHAR(1024), "            
            + RESOURCE + " VARCHAR(1024), "
            + DURATION + " VARCHAR(16), "
            + LENGTH + " INTEGER, "
            + OFFSET + " INTEGER, "   
            + PATHNAME + " VARCHAR(1024), "              
            + FAIL_COUNT + " INTEGER "            
            + ");";


	public static final String sql_index_item_res = "CREATE INDEX IDX_"
            + TABLE_NAME + "_"
            + RESOURCE + " ON "
            + TABLE_NAME + " ("
            + RESOURCE + ");";

	public static final String sql_index_item_created = "CREATE INDEX IDX_"
            + TABLE_NAME + "_"
            + CREATED_DATE + " ON "
            + TABLE_NAME + " ("
            + CREATED_DATE + ");";


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

		if (values.containsKey(CREATED_DATE) == false) {
			values.put(CREATED_DATE, now);
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
			values.put(DURATION, "00:00");
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

		return values;
	}

}
