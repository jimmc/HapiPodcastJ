package info.xuluan.podcast.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

public class Item {
	public long id;

    public String url;
    public String res;
    public String pathname;
    public int offset;
    public int status;
    public int failcount;
    
    public long length;    
    
    public long lastUpdated;

    public Item(Cursor cursor) {
    	id = cursor.getLong(cursor.getColumnIndex(ItemColumns._ID));
        res  =  cursor.getString(cursor.getColumnIndex(ItemColumns.RESOURCE));    	
        pathname  =  cursor.getString(cursor.getColumnIndex(ItemColumns.PATHNAME));   
        offset  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
        status  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.STATUS));
        failcount  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.FAIL_COUNT));
        
        length  =  cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));
        
    }   
    
    public void update(ContentResolver context){
        Log.i("ITEM","item update start");        
        try {

                ContentValues cv = new ContentValues();
                cv.put(ItemColumns.PATHNAME, pathname);
                
                cv.put(ItemColumns.OFFSET, offset);
                cv.put(ItemColumns.STATUS, status);                
                cv.put(ItemColumns.FAIL_COUNT, failcount);
                
                cv.put(ItemColumns.LENGTH, length);

                context.update(
                        ItemColumns.URI,
                        cv,
                        ItemColumns._ID + "=" + id,
                        null
                );

                Log.i("ITEM","update OK");                 
        }
        finally {
        }    	
    }


}
