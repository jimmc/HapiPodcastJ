package info.xuluan.podcast.provider;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class FeedItem {

    public String url;
    public String title;
    public String author;
    public String date;
    public String content;
    public String resource;
    public String duration;
    
	public long id;
	
	public long sub_id;

    public String pathname;
    public int offset;
    public int status;
    public int failcount;
    
    public long length;    
    
    public long update;    
    
    public String uri;
    
    public String sub_title;
    public long created;
    
    public String type;
    

    static String[] DATE_FORMATS = {
        "EEE, dd MMM yyyy HH:mm:ss Z",
    	"EEE, d MMM yy HH:mm z",
    	"EEE, d MMM yyyy HH:mm:ss z",
    	"EEE, d MMM yyyy HH:mm z",
    	"d MMM yy HH:mm z",
    	"d MMM yy HH:mm:ss z",
    	"d MMM yyyy HH:mm z",
    	"d MMM yyyy HH:mm:ss z",	
    };

    public FeedItem(){
        url = null;
        title = null;
        author = null;
        date = null;
        content = null;        
        resource = null;
        duration = null;    
        pathname = null;  
        uri = null;
        type =null;
        

        
    	id = -1 ;
    	offset = -1 ;
    	status = -1 ;
    	failcount = -1 ;
    	length = -1 ;
    	update = -1 ;  	
    	
    	created = -1;
    	sub_title = null;
    	
    }
	public FeedItem(ContentResolver context, long id ) {
		Cursor cursor = null;
		try {
			String where = ItemColumns._ID + " = " + id ;

			cursor = context.query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				fetchFromCursor(cursor);
				cursor.close();
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}


	}    
    public FeedItem(Cursor cursor){
    		fetchFromCursor(cursor);
    }   
    
    public void update(ContentResolver context){
        Log.i("ITEM","item update start");        
        try {

                ContentValues cv = new ContentValues();
                if(pathname!=null)
                	cv.put(ItemColumns.PATHNAME, pathname);
                if(offset>=0)
                	cv.put(ItemColumns.OFFSET, offset);
                if(status>=0)
                	cv.put(ItemColumns.STATUS, status);
                if(failcount>=0)
                	cv.put(ItemColumns.FAIL_COUNT, failcount);
                
                update = Long.valueOf(System.currentTimeMillis());
                cv.put(ItemColumns.LAST_UPDATE, update);
                if(created>=0)
                	cv.put(ItemColumns.CREATED, created);                
                if(length>=0)
                	cv.put(ItemColumns.LENGTH, length);
                if(uri!=null)
                	cv.put(ItemColumns.MEDIA_URI, uri);
                if(type!=null)
                	cv.put(ItemColumns.TYPE, type);                
                
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
    
    public Uri insert(ContentResolver context){
        Log.i("ITEM","item update start");        
        try {

                ContentValues cv = new ContentValues();
                if(pathname!=null)
                	cv.put(ItemColumns.PATHNAME, pathname);
                if(offset>=0)
                	cv.put(ItemColumns.OFFSET, offset);
                if(status>=0)
                	cv.put(ItemColumns.STATUS, status);
                if(failcount>=0)
                	cv.put(ItemColumns.FAIL_COUNT, failcount);
                if(update>=0)
                	cv.put(ItemColumns.LAST_UPDATE, update);
                if(length>=0)
                	cv.put(ItemColumns.LENGTH, length);
                
                if(sub_id>=0)
                	cv.put(ItemColumns.SUBS_ID, sub_id);
                if(url!=null)
                	cv.put(ItemColumns.URL, url);
                if(title!=null)
                	cv.put(ItemColumns.TITLE, title);
                
                if(author!=null)
                	cv.put(ItemColumns.AUTHOR, author);
                if(date!=null)
                	cv.put(ItemColumns.DATE, date);
                if(content!=null)
                	cv.put(ItemColumns.CONTENT, content);
                if(resource!=null)
                	cv.put(ItemColumns.RESOURCE, resource);
                if(duration!=null){
        			//Log.w("ITEM","  duration: " + duration);
                	cv.put(ItemColumns.DURATION, duration);
                	
                }
                if(sub_title!=null){
                	cv.put(ItemColumns.SUB_TITLE, sub_title);
                	
                }
                if(uri!=null)
                	cv.put(ItemColumns.MEDIA_URI, uri);                

    			return  context.insert(ItemColumns.URI, cv);                

        }
        finally {
        }    	
    }    

    
    public long getDate() {
        return parse();
    }

    private long parse() {

        for (String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format, Locale.US).parse(date).getTime();
            }
            catch (ParseException e) {
            }
        }

        return 0L;
    }
    private void fetchFromCursor(Cursor cursor) {
    	id = cursor.getLong(cursor.getColumnIndex(ItemColumns._ID));
        resource  =  cursor.getString(cursor.getColumnIndex(ItemColumns.RESOURCE));    	
        pathname  =  cursor.getString(cursor.getColumnIndex(ItemColumns.PATHNAME));   
        offset  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
        status  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.STATUS));
        failcount  =  cursor.getInt(cursor.getColumnIndex(ItemColumns.FAIL_COUNT));
        
        length  =  cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));
        
        url  =  cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
        title  =  cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
        author  =  cursor.getString(cursor.getColumnIndex(ItemColumns.AUTHOR)); 
        date  =  cursor.getString(cursor.getColumnIndex(ItemColumns.DATE));
        content  =  cursor.getString(cursor.getColumnIndex(ItemColumns.CONTENT));
        duration  =  cursor.getString(cursor.getColumnIndex(ItemColumns.DURATION));  
        uri = cursor.getString(cursor.getColumnIndex(ItemColumns.MEDIA_URI)); 
 
        created = cursor.getLong(cursor.getColumnIndex(ItemColumns.CREATED));
        sub_title = cursor.getString(cursor.getColumnIndex(ItemColumns.SUB_TITLE));
        type = cursor.getString(cursor.getColumnIndex(ItemColumns.TYPE));
    }

    @Override
    public String toString() {
        return title;
    }
    
    public String getType(){
    	if(type == null){
    		return "audio/mp3";
    	}

		if(!type.equalsIgnoreCase("")){
			return type;
		}  
		
		return "audio/mp3";
    }
}
