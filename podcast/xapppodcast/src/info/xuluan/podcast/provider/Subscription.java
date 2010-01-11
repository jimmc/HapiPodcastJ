package info.xuluan.podcast.provider;

import android.database.Cursor;

public class Subscription  {
	public long id;
    public String url;
    public String description;
    public long lastUpdated;

    
    public Subscription(Cursor cursor) {
    	id = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns._ID));
        lastUpdated  =  cursor.getLong(cursor.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));    	
    }    


    

}
