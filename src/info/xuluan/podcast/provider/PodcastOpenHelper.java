package info.xuluan.podcast.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import info.xuluan.podcast.utils.Log;

public class PodcastOpenHelper extends SQLiteOpenHelper {

	private final Log log = Log.getLog(getClass());

	//Version 12: inherited from original HapiPodcast
	//Version 13: add KEEP column to items table
	//Version 14: add SUSPENDED column to subscriptions table
	private final static int DBVERSION = 14;
	
	public PodcastOpenHelper(Context context) {
		super(context, "podcast.db", null, DBVERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL(SubscriptionColumns.sql_create_table);
		db.execSQL(SubscriptionColumns.sql_index_subs_url);
		db.execSQL(SubscriptionColumns.sql_index_last_update);

		db.execSQL(ItemColumns.sql_create_table);
		db.execSQL(ItemColumns.sql_index_item_res);
		db.execSQL(ItemColumns.sql_index_item_created);

		db.execSQL(SubscriptionColumns.sql_insert_default);
		db.execSQL(SubscriptionColumns.sql_insert_default1);
		db.execSQL(SubscriptionColumns.sql_insert_default2);
		//db.execSQL(SubscriptionColumns.sql_insert_default3);
		//db.execSQL(SubscriptionColumns.sql_insert_default4);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion>=newVersion)
			return;
		
		if (oldVersion < 12) {
			log.debug("Database is version "+oldVersion+", drop and recreate");
			db.execSQL("DROP TABLE " + ItemColumns.TABLE_NAME);
			db.execSQL("DROP TABLE " + SubscriptionColumns.TABLE_NAME);
			onCreate(db);
			return;
		}
		
		log.debug("Upgrading database from version "+oldVersion+" to "+newVersion);			
		if (oldVersion<=12) {
			//Add the KEEP column to the items table,
			//use that rather than the old KEEP status
			log.debug("executing sql: "+ItemColumns.sql_upgrade_table_add_keep_column);
			db.execSQL(ItemColumns.sql_upgrade_table_add_keep_column);
			log.debug("executing sql: "+ItemColumns.sql_populate_keep_from_status);
			db.execSQL(ItemColumns.sql_populate_keep_from_status);
			log.debug("executing sql: "+ItemColumns.sql_change_keep_status_to_played);
			db.execSQL(ItemColumns.sql_change_keep_status_to_played);
			log.debug("Done upgrading database");
		}
		if (oldVersion<=13) {
			//Add the SUSPENDED column to the subscriptions table
			log.debug("executing sql: "+SubscriptionColumns.sql_upgrade_subscriptions_add_suspended_column);
			db.execSQL(SubscriptionColumns.sql_upgrade_subscriptions_add_suspended_column);
		}
	}

}
