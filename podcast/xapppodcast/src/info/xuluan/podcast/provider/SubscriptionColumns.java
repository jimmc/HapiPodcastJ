package info.xuluan.podcast.provider;

import android.content.ContentValues;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;

public class SubscriptionColumns implements BaseColumns {

	public static final Uri URI = Uri.parse("content://" + ReadingProvider.AUTHORITY + "/subscriptions");

	public static final String TABLE_NAME = "subs";

	public static final String URL = "url";
    
	public static final String LINK = "link";    

	public static final String TITLE = "title";

	public static final String DESCRIPTION = "description";

	public static final String LAST_UPDATED = "last_updated";

	public static final String LAST_ITEM_UPDATED = "last_item_updated";	
	
	public static final String FAIL_COUNT = "fail";		

	public static final String STATUS = "status";		
	
	public static final String COMMENT = "comment";
	public static final String RATING = "rating";

	public static final String[] ALL_COLUMNS = { _ID, URL, LINK, TITLE, DESCRIPTION, LAST_UPDATED , 
		LAST_ITEM_UPDATED, FAIL_COUNT, STATUS, COMMENT, RATING};

	public static final String DEFAULT_SORT_ORDER = _ID + " ASC";
	public static final String sql_create_table = "CREATE TABLE "
            + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + 
            URL + " VARCHAR(1024), " +
            LINK + " VARCHAR(256), " +
            TITLE + " VARCHAR(128), " +
            DESCRIPTION + " VARCHAR(1024), " +
            LAST_UPDATED + " INTEGER, " +
            LAST_ITEM_UPDATED + " INTEGER, " +
            FAIL_COUNT + " INTEGER, " +   
            STATUS + " INTEGER, " + 
            COMMENT + " INTEGER, " +               
            RATING + " VARCHAR(1024) " +                        
            ");";

	public static final  String sql_index_subs_url = "CREATE UNIQUE INDEX IDX_"
        + TABLE_NAME
        + "_"
        + URL
        + " ON "
        + TABLE_NAME
        + " ("
        + URL + ");";

	public static final  String sql_index_last_update = "CREATE INDEX IDX_"
        + TABLE_NAME + "_"
        + LAST_UPDATED + " ON "
        + TABLE_NAME + " ("
        + LAST_UPDATED + ");";
	
	public static final  String sql_insert_default = "INSERT INTO "
            + TABLE_NAME + " ("
            + URL + ","
            + TITLE + ","
            + DESCRIPTION + ","
            + LINK + ","
            + LAST_UPDATED + ","
            + FAIL_COUNT + ","
            + LAST_ITEM_UPDATED 
            + ") VALUES ('http://www.cbcradio3.com/podcast/', 'unknown', 'unknown', '', 0, 0, 0);";

	public static final  String sql_insert_default1 = "INSERT INTO "
        + TABLE_NAME + " ("
        + URL + ","
        + TITLE + ","
        + DESCRIPTION + ","
        + LINK + ","
        + LAST_UPDATED + ","
        + FAIL_COUNT + ","
        + LAST_ITEM_UPDATED 
        + ") VALUES ('http://www.justing.com.cn/justpod/justpod.xml', 'unknown', 'unknown', '', 0, 0, 0);";
	
	public static final  String sql_insert_default2 = "INSERT INTO "
        + TABLE_NAME + " ("
        + URL + ","
        + TITLE + ","
        + DESCRIPTION + ","
        + LINK + ","
        + LAST_UPDATED + ","
        + FAIL_COUNT + ","
        + LAST_ITEM_UPDATED 
        + ") VALUES ('http://feeds2.feedburner.com/qiangqiang', 'unknown', 'unknown', '', 0, 0, 0);";	
	public static final  String sql_insert_default3 = "INSERT INTO "
        + TABLE_NAME + " ("
        + URL + ","
        + TITLE + ","
        + DESCRIPTION + ","
        + LINK + ","
        + LAST_UPDATED + ","
        + FAIL_COUNT + ","
        + LAST_ITEM_UPDATED 
        + ") VALUES ('http://feeds.feedburner.com/EnglishAsASecondLanguagePodcast', 'unknown', 'unknown', '', 0, 0, 0);";		

    public static ContentValues checkValues(ContentValues values, Uri uri) {
        if (values.containsKey(URL) == false) {
            throw new SQLException("Fail to insert row because URL is needed " + uri);
        }
        
        if (values.containsKey(LINK) == false) {
            values.put(LINK, "");
        }
        
        if (values.containsKey(TITLE) == false) {
            values.put(TITLE, "unknow");
        }

        if (values.containsKey(DESCRIPTION) == false) {
            values.put(DESCRIPTION, "");
        }

        if (values.containsKey(LAST_UPDATED) == false) {
            values.put(LAST_UPDATED, 0);
        }
        
        if (values.containsKey(LAST_ITEM_UPDATED) == false) {
            values.put(LAST_ITEM_UPDATED, 0);
        }   
        
        if (values.containsKey(FAIL_COUNT) == false) {
            values.put(FAIL_COUNT, 0);
        }         
        return values;
    }    
}
