package info.xuluan.podcast.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import info.xuluan.podcast.Log;
import info.xuluan.podcast.Utils;
import info.xuluan.podcast.fetcher.FeedFetcher;
import info.xuluan.podcast.fetcher.Response;
import info.xuluan.podcast.parser.FeedItem;
import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.FeedParserListenerAdapter;

import info.xuluan.podcast.provider.Item;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;


public class ReadingService extends Service {

    private static final int MSG_TIMER = 0;
    
    private static boolean mDownloading = false;    
    private static Item  mItem = null;    
    //public static final String BASE_DOWNLOAD_DIRECTORY = "/sdcard/xuluan.posdcast/download";
    
    public static final String BASE_DOWNLOAD_DIRECTORY = "/sdcard";

    public static final int REQUEST_CODE_PREF_UNCHANGED = 1;
    public static final int REQUEST_CODE_PREF_CHANGED = 2;

    public static final int FREQ_MAX = 10;
    public static final int FREQ_DEFAULT = 3;

    public static final int EXPIRES_MAX = 400;
    public static final int EXPIRES_DEFAULT = 1;
    

    public static final String NOTIFY_NEW_ITEMS = ReadingService.class.getName() + ".NOTIFY_NEW_ITEMS";
    public static final String NOTIFY_PREF_CHANGED = ReadingService.class.getName() + ".NOTIFY_PREF_CHANGED";
    public static final String NOTIFY_SUB_REMOVED = ReadingService.class.getName() + ".NOTIFY_SUB_REMOVED";

    private final Log log = Utils.getLog(getClass());

    private long ONE_MINUTE = 60L * 1000L;
    private long ONE_WEEK = 7L * 24L * 60L * ONE_MINUTE;
    private long delayed = FREQ_DEFAULT * ONE_MINUTE;
    private long expires = EXPIRES_DEFAULT * ONE_WEEK;
    private long freq = 1* 60 * ONE_MINUTE;
    
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_TIMER:
                log.info("Message: MSG_TIMER");
                removeExpires();
                refreshFeeds();
                download_res();
            	triggerNextTimer(delayed);
                
                break;
            }
        }
    };
    
    void triggerNextTimer(long delay) {
        Message msg = Message.obtain();
        msg.what = MSG_TIMER;
        handler.sendMessageDelayed(msg, delay);
    }
    
    private Item getDownloadItem(){
    	Cursor cursor = null;
    	try{
	        String where = ItemColumns.STATUS + ">" + ItemColumns.ITEM_STATUS_MAX_READING_VIEW
	        +" AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW;
	        
	        log.info("getDownloadItem");
	        cursor = getContentResolver().query(ItemColumns.URI, ItemColumns.ALL_COLUMNS, where, null ,null);
	        if (cursor.moveToFirst()) {
	        	Item item = new Item(cursor);
	        	cursor.close();
	        	return item;
	        }    
    	}finally {
        	if(cursor!=null)
        		close(cursor);    		
        }  

        return null;

    	
    }
    private void download_res() {
    	//log.info("download_res start");
    	if(mDownloading)
    		return;

    	try{
    		//log.info("download_res query");
    		Item item = getDownloadItem();
	        if (item!=null) {
	        	//log.info("download_res start_download");
	        	mItem = item;
	            start_download();
	        }    
    	}
        finally {

        }        
        //log.info("download_res end");
    
    }
    private void start_download() {
    	mDownloading = true;

    	new Thread() {
            public void run() {
            	try{
                	//log.info("start_download start");
                	if(mItem.pathname.equals("") ){
                    	String path_name = BASE_DOWNLOAD_DIRECTORY+"/podcast_"+mItem.id+".mp3";
                    	mItem.pathname = path_name;                		
                	}

                	int advance_len = FeedFetcher.download(mItem);
                	
                	if(advance_len==0){
                		mItem.failcount++;
                	}else{
                		mItem.offset += advance_len;
                	}
                	
                	mItem.update(getContentResolver());
                	
                	//log.info("start_download end");                		
            	}finally{
                	mDownloading = false;
            	}
	
            }
        
        }.start();    	

    	
    }
    
    private void removeExpires() {
        long expiredTime = System.currentTimeMillis() - this.expires;
        try {
            String where = ItemColumns.CREATED_DATE + "<" + expiredTime ;
            getContentResolver().delete(ItemColumns.URI, where, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshFeeds() {
        final String url = findSubscriptionUrlByFreq();
        if (url==null) {
            return;
        }
        new Thread() {
            public void run() {
            	FeedParserListenerAdapter listener = fetchFeed(url);
            	if(listener!=null)
            		updateFeed(url, listener);
            	else
            		updateFetch(url);
            }
        
        }.start();
    }    

    private String findSubscriptionUrlByFreq() {
        Cursor cursor = null;
        try {
            Long now = Long.valueOf(System.currentTimeMillis());
            
            String where =SubscriptionColumns.LAST_UPDATED +"<" + (now - freq);
            //log.info("where = " + where); 
            //log.info("freq = " + freq); 
            //log.info("now = " + now); 
            
            
            cursor = getContentResolver().query(SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS 
            		, null, null, SubscriptionColumns.LAST_UPDATED + " desc");
            if (cursor.moveToFirst()) {
            	long ldate = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns.LAST_UPDATED));
            	
            	//log.info("ldate = " + ldate); 	
            	if(ldate < (now - freq)){
            		String url = cursor.getString(cursor.getColumnIndex(SubscriptionColumns.URL));
            		cursor.close();
            		return url;
            	}
            		
            }
        }
        finally {
        	if(cursor!=null)
        		cursor.close();
        }
        return null;
        
    }

    public FeedParserListenerAdapter fetchFeed(String url) {
        log.info("fetchFeed start");

        FeedFetcher fetcher = new FeedFetcher();
        FeedParserListenerAdapter listener = new FeedParserListenerAdapter();
        
        try {        
            Response response = fetcher.fetch(url, 0L, null);

            log.info("fetcher.fetch end");
            if(response!=null)
            	FeedParser.getDefault().parse(new ByteArrayInputStream(response.content), listener);
            else
            	log.info("response == null");
            
        }
        catch (Exception e) {
            log.info("Parse XML error:", e);
        }
        
        log.info("fetchFeed getFeedItemsSize = "+listener.getFeedItemsSize());
                        
        if(listener.getFeedItemsSize()>0){
        	return listener; 	
        }

        return null;
    }

    private void updateFetch(String url) {
        Cursor cursor = null;
        log.info("updateFetch start");        
        try {
            cursor = getContentResolver().query(SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, null, null, SubscriptionColumns.LAST_UPDATED + " asc");
            if (cursor.moveToFirst()) {
                String sub_id = cursor.getString(cursor.getColumnIndex(SubscriptionColumns._ID));    
                log.warn("Feed updated: " + cursor.getString(cursor.getColumnIndex(SubscriptionColumns.URL)));                
                int fail_count = cursor.getInt(cursor.getColumnIndex(SubscriptionColumns.FAIL_COUNT));   
                ContentValues cv = new ContentValues();

                Long now = Long.valueOf(System.currentTimeMillis());
                cv.put(SubscriptionColumns.LAST_UPDATED, now);
                fail_count++;
                cv.put(SubscriptionColumns.FAIL_COUNT, fail_count);

                getContentResolver().update(
                        SubscriptionColumns.URI,
                        cv,
                        SubscriptionColumns._ID + "=" + sub_id,
                        null
                );

                log.info("updateFetch OK");                 
            }
        }
        finally {
            close(cursor);
        }
    }    

    public void updateFeed(String url, FeedParserListenerAdapter listener) {
        // sort feed items:
    	String feedTitle = listener.getFeedTitle();
    	String feedDescription = listener.getFeedDescription();
    	FeedItem[] feedItems = listener.getFeedItems(); 
/*    	
    	for (FeedItem item : feedItems) {
    		
    		log.warn("item_date: " + item.date);
    	}
*/    	
        Arrays.sort(
                feedItems,
                new Comparator<FeedItem>() {
                    public int compare(FeedItem i1, FeedItem i2) {
                        long d1 = i1.getDate();
                        long d2 = i2.getDate();

                        if (d1==d2)
                            return i1.title.compareTo(i2.title);
                        return d1 > d2 ? (-1) : 1;
                    }
                }
        );
/*        
    	for (FeedItem item : feedItems) {
    		
    		log.warn("item_date: " + item.date);
    	}        
*/
        Subscription subscription = querySubscriptionByUrl(url);
        if (subscription==null)
            return;

        log.info("feedItems length: " + feedItems.length);
       
        List<FeedItem> added = new ArrayList<FeedItem>(feedItems.length);
        ContentResolver cr = getContentResolver();
        for (FeedItem item : feedItems) {
            long d = item.getDate();
            log.info("item_date: " + item.date);
            
            if (d <= subscription.lastUpdated){
            	log.info("item lastUpdated =" + d+ " feed lastUpdated = "+subscription.lastUpdated);
            	log.info("item date =" + item.date);
                break;
            }
            log.info("subscription.id" );
            log.info("subscription.id : " + subscription.id);   
            String where = ItemColumns.SUBS_ID + "="+ subscription.id + " and " 
            + ItemColumns.RESOURCE + "= '" + item.resource +"'";
            log.info("where : " + where);   
            
            
            Cursor cursor = cr.query(
                    ItemColumns.URI,
                    new String[] { ItemColumns._ID },
                    where,
                    null,
                    null
            );
            log.info("cursor");
            
            if (cursor.moveToFirst()) {
                // exist, so no need continue:
                cursor.close();
            	log.info("exist break" );

                break;
            }
            else {
                cursor.close();
                // add to database:
                log.info("add item = " + item.title );
                added.add(item);
            }
            
        }
        log.info("added size: " + added.size());

        ContentValues cv = new ContentValues();
        cv.put(SubscriptionColumns.TITLE, feedTitle);
        cv.put(SubscriptionColumns.DESCRIPTION, feedDescription);
        cv.put(SubscriptionColumns.FAIL_COUNT, 0);
        Long now = Long.valueOf(System.currentTimeMillis());

        cv.put(SubscriptionColumns.LAST_UPDATED, now);
        if (!added.isEmpty()){
        	log.warn("MAX item date:==" + added.get(0).date );
            cv.put(SubscriptionColumns.LAST_ITEM_UPDATED, added.get(0).getDate());        
        	
        }

        int n = getContentResolver().update(
                SubscriptionColumns.URI,
                cv,
                SubscriptionColumns._ID + "=" + subscription.id,
                null
        );
        if (n==1) {
            log.warn("Feed updated: " + url);
        }
        if (added.isEmpty())
            return;
        addItems(subscription.id, added);

    }

    public Subscription querySubscriptionByUrl(String feedUrl) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    SubscriptionColumns.URI,
                    SubscriptionColumns.ALL_COLUMNS,
                    SubscriptionColumns.URL + "=?",
                    new String[] { feedUrl },
                    null
            );
            if (cursor.moveToFirst()) {
            	Subscription sub = new Subscription(cursor);
            	cursor.close();
                return sub;
            }
        }
        catch(Exception e) { 
        }
        
        if(cursor!=null)
        	cursor.close();
        return null;
    }

    void addItems(Long sub_id, List<FeedItem> items) {
        ContentResolver cr = getContentResolver();
        int len = items.size();
        for(int i=len-1;i>=0;i--){
        	
        	FeedItem item = items.get(i);
 //       for (FeedItem item : items) {
            ContentValues cv = new ContentValues();
            cv.put(ItemColumns.SUBS_ID, sub_id);
            cv.put(ItemColumns.URL, item.url); 
            cv.put(ItemColumns.TITLE, item.title);
            cv.put(ItemColumns.AUTHOR, item.author);
            cv.put(ItemColumns.DATE, item.date);
            cv.put(ItemColumns.CONTENT, item.content);
            cv.put(ItemColumns.RESOURCE, item.resource);
            cv.put(ItemColumns.DURATION, item.duration);
            
            
            Uri uri = cr.insert(ItemColumns.URI, cv);
            if(uri!=null)
            log.info("Inserted new item: " + uri.toString());
        }
    }

    public Uri addSubscription(String url) {
        if ( querySubscriptionByUrl(url)!=null){
            return null;
        }
        ContentValues cv = new ContentValues();
        cv.put(SubscriptionColumns.TITLE, url);
        cv.put(SubscriptionColumns.URL, url);
        cv.put(SubscriptionColumns.DESCRIPTION, url);
        cv.put(SubscriptionColumns.LAST_UPDATED, 0L);
        return getContentResolver().insert(SubscriptionColumns.URI, cv);
        
    }

    public void removeSubscription(String sub_id) {
        ContentResolver cr = getContentResolver();
        cr.delete(SubscriptionColumns.URI, SubscriptionColumns._ID + "=" + sub_id, null);
        cr.delete(ItemColumns.URI, ItemColumns.SUBS_ID + "=" + sub_id, null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log.info("ReadingService.onCreate()");
        triggerNextTimer(1);
        
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.info("ReadingService.onDestroy()");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        log.info("ReadingService.onLowMemory()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void close(Cursor cursor) {
        if (cursor!=null) {
            cursor.close();
        }
    }

    private final IBinder binder = new ReadingBinder();

    public class ReadingBinder extends Binder {
        public ReadingService getService() {
            return ReadingService.this;
        }
    }

}
