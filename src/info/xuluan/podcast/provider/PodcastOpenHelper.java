package info.xuluan.podcast.provider;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import info.xuluan.podcast.utils.Log;

public class PodcastOpenHelper extends SQLiteOpenHelper {

	private final Log log = Log.getLog(getClass());

	//Version 12: inherited from original HapiPodcast
	//Version 13: add KEEP column to items table
	//Version 14: add SUSPENDED column to subscriptions table
	private final static int DBVERSION = 14;
	private boolean downgradeEnabled = false; //for debugging
	
	public PodcastOpenHelper(Context context) {
		super(context, "podcast.db", null, DBVERSION);
	}

	//For debugging, allow opening the database using an older version number
	public PodcastOpenHelper(Context context, int version) {
		super(context, "podcast.db", null, version);
		downgradeEnabled = true;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL(SubscriptionColumns.sql_create_table(DBVERSION));
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

	@SuppressLint("NewApi")
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion>=newVersion) {
			if ((newVersion < oldVersion) && downgradeEnabled) {
				onDowngrade(db, oldVersion, newVersion);
			}
			return;
		}
		
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

	//For debugging, allow downgrading the database.
	//Before API 11, this method did not exist in the API, and onUpgrade would be called whenever
	//oldVersion!=newVersion, even when oldVersion>newVersion.
	//Starting with API 11, the default behavior is to throw an exception if
	//oldVersion>newVersion and there is no onDowngrade. By defining our own onDowngrade
	//and putting code in onUpgrade to call us when oldVersion>newVersion, we end up
	//executing this method on a downgrade for any API version.
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion<=newVersion)
			return;
		
		if (newVersion<14 && oldVersion>=14) {
			//Remove the stuff added in version 14
			dropTableColumn(db, SubscriptionColumns.TABLE_NAME, SubscriptionColumns.SUSPENDED,
					SubscriptionColumns.sql_create_table(13));
		}
		
		if (newVersion<13 && oldVersion>=13) {
			//Remove stuff added in version 13
			//TODO - for each item marked in the KEEP columns of the items table,
			// if the status is played, change it to keep.
			//TODO - remove the KEEP column from the items table
		}
	}
	
	private void dropTableColumn(SQLiteDatabase db, String table, String dropColumn,
			String createTableSql) {
		List<String> columnList = tableColumns(db, table, dropColumn);
		String columnNames = TextUtils.join(",", columnList);
		String oldTable = table + "_old";
		db.execSQL("ALTER TABLE "+table+" RENAME TO "+oldTable+";");
		db.execSQL(createTableSql);
		db.execSQL("INSERT INTO "+table+" SELECT "+columnNames+" FROM "+oldTable+";");
		db.execSQL("DROP TABLE "+oldTable+";");
	}
	
	private List<String> tableColumns(SQLiteDatabase db, String tableName, String excludingColumn) { 
		ArrayList<String> columns = new ArrayList<String>();
		String columnsCommand = "PRAGMA TABLE_INFO(" + tableName + ");";
		Cursor columnsCursor = db.rawQuery(columnsCommand, null);
		int nameColIndex = columnsCursor.getColumnIndex("name");
		while (columnsCursor.moveToNext()) {
			String column = columnsCursor.getString(nameColIndex);
			if (!column.equals(excludingColumn))
				columns.add(column);
		}
		columnsCursor.close();
		return columns;
	}

}
