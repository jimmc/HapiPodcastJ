package info.xuluan.podcast.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ReadingOpenHelper extends SQLiteOpenHelper {

    public ReadingOpenHelper(Context context) {
        super(context, "reader.db", null, 8);
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
        db.execSQL(SubscriptionColumns.sql_insert_default2);
        db.execSQL(SubscriptionColumns.sql_insert_default3);
        
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion!=newVersion) {
            // drop db
            db.execSQL(cat("DROP TABLE ", ItemColumns.TABLE_NAME));
            db.execSQL(cat("DROP TABLE ", SubscriptionColumns.TABLE_NAME));
            onCreate(db);
        }
    }

    String cat(String... ss) {
        StringBuilder sb = new StringBuilder(ss.length << 3);
        for (String s : ss)
            sb.append(s);
        return sb.toString();
    }
}
