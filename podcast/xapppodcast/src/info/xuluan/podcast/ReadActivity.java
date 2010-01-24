package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.ReadingService;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class ReadActivity extends Activity {

    static final int MENU_MORE = Menu.FIRST + 1;
    static final int MENU_DOWNLOAD = Menu.FIRST + 2;
    static final int MENU_PLAY = Menu.FIRST + 6;
    static final int MENU_BACK = Menu.FIRST+5;
    static final int MENU_PREF = Menu.FIRST + 3;	    
    String item_id;

    static final String CSS;
    static {

        CSS = new StringBuilder(256)
                .append("<style type=\"text/css\">\n")
                .append("<!--\n")
                .append("body { color: #FFF; background-color: #000; }\n")
                .append("a:link,a:visited { color:#09F; }\n")
                .append("a:active,a:hover { color:#F60; }\n")
                .append("-->\n")
                .append("</style>\n")
                .toString();
    }

    private final Log log = Utils.getLog(getClass());
    private String url = null;

    ComponentName service = null;
    
    private ReadingService serviceBinder = null;
    

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            serviceBinder = ((ReadingService.ReadingBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            serviceBinder = null;
        }
    };
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_MORE, 0, getResources().getString(R.string.menu_orig_link)).setIcon(android.R.drawable.ic_menu_directions);
        menu.add(0, MENU_DOWNLOAD, 1, getResources().getString(R.string.menu_downloading)).setIcon(android.R.drawable.ic_menu_set_as);
        //menu.add(0, MENU_PLAY, 2, getResources().getString(R.string.menu_play)).setIcon(android.R.drawable.ic_menu_slideshow);
        
        menu.add(0, MENU_PREF, 3, getResources().getString(R.string.menu_pref)).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_BACK, 4, getResources().getString(R.string.menu_back)).setIcon(android.R.drawable.ic_menu_revert);
         
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem item = menu.findItem(MENU_DOWNLOAD);
        FeedItem feed_item = new FeedItem(getContentResolver(),Integer.parseInt(item_id));
        if(feed_item.status < ItemColumns.ITEM_STATUS_MAX_READING_VIEW){
            item.setEnabled(true);
        	
        }else{
            item.setEnabled(false);
        	
        }
        /*
        item = menu.findItem(MENU_PLAY);
        
        if(feed_item.status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW){
            item.setEnabled(true);        
        	
        }else{
            item.setEnabled(false);        
        	
        }
        */

        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==MENU_MORE) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }else if(item.getItemId()==MENU_DOWNLOAD){

            ContentValues cv = new ContentValues();

            cv.put(ItemColumns.STATUS, ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE);
            getContentResolver().update(ItemColumns.URI, cv, "_ID=?",
					new String[] { item_id });
            serviceBinder.start_download();
            return true;
        }else if(item.getItemId()==MENU_BACK){
            finish();
            return true;
        }else if(item.getItemId()==MENU_PREF){
        	startActivity(new Intent(this, Pref.class));
        	return true;
        }else if(item.getItemId()==MENU_PLAY){
        	play(Integer.parseInt(item_id));
        	return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.read);

        Intent intent = getIntent();
        
        Uri uri = intent.getData();
        Cursor cursor = getContentResolver().query(uri, ItemColumns.ALL_COLUMNS, null, null, null);
        if (!cursor.moveToFirst()) {
            show404();
            return;
        }
        url = cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
        item_id = cursor.getString(cursor.getColumnIndex(ItemColumns._ID));

        String title = cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
        String content = cursor.getString(cursor.getColumnIndex(ItemColumns.CONTENT));
        cursor.close();

        // set title:
        setTitle(title);
/*
        // load html:
        StringBuilder html = new StringBuilder(content.length()+200);
        html.append("<html><head><title>")
            .append(title)
            .append("</title>\n")
            .append(CSS)
            .append("</head><body>")
            .append(content)
            .append("<p>")
            .append("</p>");
        if(url!=null){
        	html.append("<p><a target='_blank' href='")
            .append(url)
            .append("'>")
            .append(getResources().getString(R.string.menu_orig_link))
            .append("</a></p>");
        }else
        	url = "";
        html.append("</body></html>");
        String baseUrl = getBaseUrl(url);
        log.info(url);
        log.info("base url:" + baseUrl);
*/
        WebView web = (WebView) this.findViewById(R.id.webview);
        
        web.loadData(content, "text/html", "utf-8");
/*        
        //web.getSettings().setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                log.info("onLoadResource: " + url);
            }
        });
        web.loadDataWithBaseURL(
                url,
                html.toString(),
                "text/html",
                "UTF-8",
                null
        );
*/	
/*        
        TextView contentView = (TextView) findViewById(R.id.content);  
        	contentView.setText(Html.fromHtml(content));	
*/        	
        service = startService(new Intent(this, ReadingService.class));

        // bind service:
        Intent bindIntent = new Intent(this, ReadingService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        //stopService(new Intent(this, service.getClass()));
    }    

    void show404() {
  	
        WebView web = (WebView) this.findViewById(R.id.webview);
        web.loadData(
                "<html><body><h1>404 Not Found</h1><p>The item was deleted.</p></body></html>",
                "text/html",
                "UTF-8"
        );
    }

    String getBaseUrl(String url) {
        int n = url.lastIndexOf('/');
        if (n > "https://".length()) {
            return url.substring(0, n+1);
        }
        return url + "/";
    }
    private void play(long id){
        
		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, id);
		Cursor cursor = managedQuery(uri, ItemColumns.ALL_COLUMNS, null, null ,null);
		if(cursor.moveToFirst()){
			FeedItem item = new FeedItem(cursor);
			cursor.close();
			
			if( (item!=null) &&(item.status==ItemColumns.ITEM_STATUS_NO_PLAY)){
				item.status = ItemColumns.ITEM_STATUS_PLAYED;
				item.update(getContentResolver());
			}			
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
		//getContentResolver().query("content://media/external/audio/media"
	    //			ItemColumns.ALL_COLUMNS, where, null, null);
		//Uri data = Media.getContentUriForPath("file://"+item.pathname);
		 //log.warn("Uri =  " + data);	
				//Uri data = Uri.parse("file://"+item.pathname);
		Uri data = Uri.parse(item.uri);
		
				intent.setDataAndType(data,"audio/mp3"); 
	    		 log.warn("palying " + item.pathname);				
				try { 
						  startActivity(intent); 
				   } catch (ActivityNotFoundException e) { 
						  e.printStackTrace(); 
							Intent intent2 = new Intent(android.content.Intent.ACTION_VIEW);
						  
						  data = Uri.parse("file://"+item.pathname);
							intent2.setDataAndType(data,"audio/mp3");
							try{
								startActivity(intent2); 
								
							}catch (Exception e2){
								  e2.printStackTrace(); 
								
							}
						  
				   } 
		}
		
		if(cursor!=null)
			cursor.close();
    	
    }
}
