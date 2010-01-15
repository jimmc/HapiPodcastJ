package info.xuluan.podcast;


import java.io.File;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.ReadingService;


import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class PlayListActivity extends ListActivity {

    private final Log log = Utils.getLog(getClass());

    SimpleCursorAdapter mAdapter;
    ComponentName service = null;
    
	private static final String[] PROJECTION = new String[] {
		ItemColumns._ID, // 0
		ItemColumns.TITLE, // 1
	};    

    private ReadingService serviceBinder = null;

    private ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                serviceBinder = ((ReadingService.ReadingBinder)service).getService();
            }

            public void onServiceDisconnected(ComponentName className) {
                serviceBinder = null;
            }
    };

    static final int MENU_REFRESH = Menu.FIRST + 1;
    static final int MENU_SUBS = Menu.FIRST + 2;
    static final int MENU_PREF = Menu.FIRST + 3;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, getResources().getString(R.string.menu_refresh)).setIcon(android.R.drawable.ic_menu_more);
        menu.add(0, MENU_SUBS, 1, getResources().getString(R.string.menu_subs)).setIcon(android.R.drawable.ic_menu_manage);
        menu.add(0, MENU_PREF, 2, getResources().getString(R.string.menu_pref)).setIcon(android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(MENU_REFRESH);
        item.setEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_REFRESH:
            refreshItems();
            break;
        case MENU_SUBS:
            startActivity(new Intent(this, SubsActivity.class));
            break;
       
        case MENU_PREF:
            //startActivity(new Intent(this, PrefActivity.class));
            break;
       
        }
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
       
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		Cursor cursor = managedQuery(uri, ItemColumns.ALL_COLUMNS, null, null ,null);
		if(cursor.moveToFirst()){
			FeedItem item = new FeedItem(cursor);
			cursor.close();
			
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
				   } 
		}
		
		if(cursor!=null)
			cursor.close();
		
    }
    
	 private void openFile(File aFile) { 
  		if (aFile==null){
  	    		 log.warn("file object is null");
  	    		 return;  				
  		}
  			
    	 if (!aFile.exists()) {
    		 log.warn("file is not exist" + aFile.getName());
    		 return;
    	 }
    	 
          Intent intent = new Intent(android.content.Intent.ACTION_VIEW);


          Uri data =  Uri.fromFile(aFile);
          String type = "audio/mp3";
          intent.setDataAndType(data, type);

     	 // Were we in GET_CONTENT mode?
     	 Intent originalIntent = getIntent();
     	 
     	 if (originalIntent != null && originalIntent.getAction() != null && originalIntent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
    		 // In that case, we should probably just return the requested data.
     		 setResult(RESULT_OK, intent);
     		 finish();
    		 return;
    	 }
    	 

          
          try {
        	  //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	  intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        	  //intent.addFlags(intent.FLAG_ACTIVITY_SINGLE_TOP );
        	  
        	  
        	  startActivity(intent); 

          } catch (Exception e) {
     		 log.warn("Exception " +e);
          };
     }     

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle("Play List");
        
        getListView().setOnCreateContextMenuListener(this);
        
        Intent intent = getIntent();
        intent.setData(ItemColumns.URI);
        String where = ItemColumns.STATUS + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW;

        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, where, null ,null);

        // Used to map notes entries from the database to views
        mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor,
                new String[] { ItemColumns.TITLE },
                new int[] { android.R.id.text1});
        setListAdapter(mAdapter);
        
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

    private void refreshItems() {

    }

}
