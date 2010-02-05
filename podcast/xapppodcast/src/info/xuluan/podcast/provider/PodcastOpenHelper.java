package info.xuluan.podcast.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PodcastOpenHelper extends SQLiteOpenHelper {

	public PodcastOpenHelper(Context context) {
		super(context, "podcast.db", null, 1);
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
		db.execSQL(SubscriptionColumns.sql_insert_default3);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion != newVersion) {
			// drop db
			db.execSQL("DROP TABLE " + ItemColumns.TABLE_NAME);
			db.execSQL("DROP TABLE " + SubscriptionColumns.TABLE_NAME);
			onCreate(db);
		}
	}

}
