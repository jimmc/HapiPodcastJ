package info.xuluan.podcast.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;

public class Subscription  {
	public long id;
    public String title;
	
    public String url;
    public String description;
    public long lastUpdated;

	public Subscription(ContentResolver context, long id ) {
		Cursor cursor = null;
		try {
			String where = SubscriptionColumns._ID + " = " + id ;

			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				fetchFromCursor(cursor);
				cursor.close();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}


	}        
    public Subscription(Cursor cursor) {
    	fetchFromCursor(cursor);
    }  
    
    public void delete(ContentResolver context){
        Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, id);
        context.delete(uri, null, null);    	
    }
    private void fetchFromCursor(Cursor cursor) {
    	id = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns._ID));
        lastUpdated  =  cursor.getLong(cursor.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
        title  =  cursor.getString(cursor.getColumnIndex(SubscriptionColumns.TITLE));    	
    	
    }

    

}
