package info.xuluan.podcast;


import java.util.HashMap;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.ReadingService;


import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;

public class MainActivity extends ListActivity {

    private final Log log = Utils.getLog(getClass());

    IconCursorAdapter mAdapter;
    ComponentName service = null;
    
    private static HashMap<Integer,Integer> mIconMap;
    static {


    	mIconMap = new HashMap<Integer, Integer>();
    	mIconMap.put(ItemColumns.ITEM_STATUS_UNREAD,R.drawable.new_item);
    	mIconMap.put(ItemColumns.ITEM_STATUS_READ,R.drawable.open_item);    	

    	
    }
    
	private static final String[] PROJECTION = new String[] {
		ItemColumns._ID, // 0
		ItemColumns.TITLE, // 1
		ItemColumns.DURATION,
		ItemColumns.SUB_TITLE,
		ItemColumns.STATUS, // 1
		
	};    
    private static final int COLUMN_INDEX_TITLE = 1;

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
    static final int MENU_DOWNLOADING = Menu.FIRST + 4;    
    static final int MENU_DOWNLOADED = Menu.FIRST + 5;      

    public static final int MENU_ITEM_START_DOWNLOAD = Menu.FIRST+10;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.add(0, MENU_REFRESH, 0, getResources().getString(R.string.menu_refresh)).setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(0, MENU_SUBS, 1, getResources().getString(R.string.menu_subs)).setIcon(android.R.drawable.ic_menu_agenda);
        menu.add(0, MENU_PREF, 2, getResources().getString(R.string.menu_pref)).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_DOWNLOADING, 3, getResources().getString(R.string.menu_downloading)).setIcon(android.R.drawable.ic_menu_set_as);
        menu.add(0, MENU_DOWNLOADED, 4, getResources().getString(R.string.menu_play_list)).setIcon(android.R.drawable.ic_menu_slideshow);        
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
        switch (item.getItemId()) {
        case MENU_REFRESH:
            refreshItems();
            return true;
        case MENU_SUBS:
            startActivity(new Intent(this, SubsActivity.class));
            return true;
       
        case MENU_PREF:
            startActivity(new Intent(this, Pref.class));
            return true;
        case MENU_DOWNLOADING:
            intent = new Intent(this, DownloadingActivity.class);
            startActivity(intent);
            return true;
        case MENU_DOWNLOADED:
            intent = new Intent(this, PlayListActivity.class);
            startActivity(intent);
            return true;           
       
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
       
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {
			FeedItem item = new FeedItem(getContentResolver(),id);
			if( (item!=null) &&(item.status==ItemColumns.ITEM_STATUS_UNREAD)){
				item.status = ItemColumns.ITEM_STATUS_READ;
				item.update(getContentResolver());
			}
				
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
		}        
    }

	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setTitle(getResources().getString(R.string.app_name));
        
        getListView().setOnCreateContextMenuListener(this);
        
        Intent intent = getIntent();
        intent.setData(ItemColumns.URI);
        String where = ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_READING_VIEW;

        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, where, null ,null);

        // Used to map notes entries from the database to views
        mAdapter = new IconCursorAdapter(this, R.layout.list_item, cursor,
                new String[] { ItemColumns.TITLE,ItemColumns.SUB_TITLE,ItemColumns.DURATION, ItemColumns.STATUS },
                new int[] { R.id.text1,R.id.text2,R.id.text3},
        mIconMap);
        setListAdapter(mAdapter);
        
        service = startService(new Intent(this, ReadingService.class));
        
        // bind service:
        Intent bindIntent = new Intent(this, ReadingService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            log.error("bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_START_DOWNLOAD, 0, R.string.menu_download);


        
        
    }
        
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
        	log.error("bad menuInfo", e);
            return false;
        }

        switch (item.getItemId()) {
        case MENU_ITEM_START_DOWNLOAD: {
        	
        	FeedItem feeditem = new FeedItem(getContentResolver(),info.id);
        	if(feeditem==null)
        		return true;
        	
        	feeditem.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
        	feeditem.update(getContentResolver());
            serviceBinder.start_download();            
            return true;
        }
    }
    return false;
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
