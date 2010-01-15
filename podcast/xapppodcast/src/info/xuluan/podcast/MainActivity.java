package info.xuluan.podcast;


import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.ReadingService;


import android.app.ListActivity;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class MainActivity extends ListActivity {

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
    static final int MENU_DOWNLOADING = Menu.FIRST + 4;    
    static final int MENU_DOWNLOADED = Menu.FIRST + 5;      


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_REFRESH, 0, getResources().getString(R.string.menu_refresh)).setIcon(android.R.drawable.ic_menu_more);
        menu.add(0, MENU_SUBS, 1, getResources().getString(R.string.menu_subs)).setIcon(android.R.drawable.ic_menu_manage);
        menu.add(0, MENU_PREF, 2, getResources().getString(R.string.menu_pref)).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_DOWNLOADING, 3, getResources().getString(R.string.menu_downloading)).setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_DOWNLOADED, 4, getResources().getString(R.string.menu_downloaded)).setIcon(android.R.drawable.ic_menu_manage);        
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
    	Intent intent;
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
        case MENU_DOWNLOADING:
            intent = new Intent(this, DownloadingActivity.class);
            startActivity(intent);
            break;
        case MENU_DOWNLOADED:
            intent = new Intent(this, PlayListActivity.class);
            startActivity(intent);
            break;            
       
        }
        return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
       
		Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
		String action = getIntent().getAction();
		if (Intent.ACTION_PICK.equals(action)
				|| Intent.ACTION_GET_CONTENT.equals(action)) {
			setResult(RESULT_OK, new Intent().setData(uri));
		} else {
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
		}        
    }
	private boolean getWifiStatus(){
		log.warn("getWifiStatus");
		WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		log.warn("type: "+wifiManager.isWifiEnabled());

		
        
		
		ConnectivityManager cm = (ConnectivityManager)this.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		
		log.warn("type: "+info.getType());
		log.warn("name: "+info.getTypeName());
		log.warn("connect: "+info.isConnected());
		log.warn("available: "+info.isAvailable ());		
	
		if(info.isConnected()){
			return true;
		}else{
			return false;
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
