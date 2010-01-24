package info.xuluan.podcast;


import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.ReadingService;


import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;

import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;

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
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadingActivity extends ListActivity {

    private final Log log = Utils.getLog(getClass());
    private ReadingService serviceBinder = null;
	DownloadItemListener mDownloadListener = null;

	MyListCursorAdapter mAdapter;
    ComponentName service = null;
    
	private static final String[] PROJECTION = new String[] {
		ItemColumns._ID, // 0
		ItemColumns.TITLE, // 1
		ItemColumns.DURATION,
		ItemColumns.SUB_TITLE,
		ItemColumns.OFFSET, 
		ItemColumns.LENGTH, 	
		ItemColumns.STATUS, 
	};
    private static final int COLUMN_INDEX_TITLE = 1;
	
    static final int MENU_RESTART = Menu.FIRST + 1;
    static final int MENU_BACK = Menu.FIRST + 2;
    static final int MENU_PREF = Menu.FIRST + 3;
    
    public static final int MENU_ITEM_REMOVE = Menu.FIRST+10;
    public static final int MENU_ITEM_PAUSE = Menu.FIRST+11;
    public static final int MENU_ITEM_RESUME = Menu.FIRST+12;    
    
    
    
    static class MyListCursorAdapter extends SimpleCursorAdapter {
    	
        protected int[] mFrom2;
        protected int[] mTo2;    	
        private static HashMap<Integer,Integer> mIconMap;
        static {


        	mIconMap = new HashMap<Integer, Integer>();
        	mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE,R.drawable.waiting);
        	mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE,R.drawable.pause);    	

        	
        };        
    	MyListCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            mTo2 = to;
            if (cursor != null) {
                int i;
                int count = from.length;
                if (mFrom2 == null || mFrom2.length != count) {
                	mFrom2 = new int[count];
                }
                for (i = 0; i < count; i++) {
                	mFrom2[i] = cursor.getColumnIndexOrThrow(from[i]);
                }
            }            

        }    
   	
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			final int[] to = mTo2;
			final int count = to.length;
			final View[] holder = new View[count+1];

			for (int i = 0; i < count; i++) {
				holder[i] = v.findViewById(to[i]);
			}
			holder[count] = v.findViewById(R.id.icon);
			v.setTag(holder);

			return v;

		}
		public void setViewImage2(ImageView v, int value) {
			
			v.setImageResource(value);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final View[] holder = (View[]) view.getTag();
			final int count = mTo2.length;
			final int[] from = mFrom2;
			int offset=0;
			int length=-1;
			

			for (int i = 0; i < count+1; i++) {
				final View v = holder[i];
				//log.info("offset = "+ offset+" length = "+length);
				if(i == count){
					View v_icon = view.findViewById(R.id.icon);    
					int status = cursor.getInt(from[i]);
					
					setViewImage2((ImageView) v_icon, mIconMap.get(status));

					break;
				}else if(i == 1){
					offset = cursor.getInt(from[i]);
					if (v != null) {
						setViewText((TextView) v, "");					

					}
				}else if(i==2){
					length = cursor.getInt(from[i]);
					String str = "0% ( 0 KB / 0 KB )";
			        if(length>0){

		            double d = 100.0*offset/length;
		            
		            int status = (int)d;
		            
		            str = ""+status+"% ( "+(formatLength(offset))+" / "
		            			+(formatLength(length))+" )";
			        }
			        
					//log.info("str = "+ str);

					if (v != null) {
						setViewText((TextView) v, str);

					}
					
					continue;					
				}else{
					if (v != null) {
						String text = cursor.getString(from[i]);
						
						if (text == null) {
							text = "";
						}
										
						if (v instanceof TextView) {
							setViewText((TextView) v, text);
						} else if (v instanceof ImageView) {
							setViewImage((ImageView) v, text);
						}
					}					
				}

			}
			
		}

	}	
	
    private final BroadcastReceiver mDownloadStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	FeedItem item = new FeedItem();
        	
	        item.title  = intent.getStringExtra(ItemColumns.TITLE);
	        item.offset = intent.getIntExtra(ItemColumns.OFFSET,0);
	        item.length = intent.getLongExtra(ItemColumns.LENGTH,0);
	        item.duration = intent.getStringExtra(ItemColumns.DURATION);
	        
	        updateDownloadInfo(item);
	        
        }
    };
    

    private ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                serviceBinder = ((ReadingService.ReadingBinder)service).getService();
                // set listener
    	        //TextView title = (TextView) DownloadingActivity.this.findViewById(R.id.title);  
    	        //String data = serviceBinder.getDownloadTitle();
    	        //title.setText(data);                
               
            }

            public void onServiceDisconnected(ComponentName className) {
                serviceBinder = null;
            }
    };


    
    
    private static String formatLength(int length){
    	
    	length /=1024;
    	
    	int i = (length%1000);
    	String s = "";
    	if(i<10){
    		s = "00"+i;
    	}else if(i<100){
    		s = "0"+i;
    	}else{
    		s += i ;
    	}
    		
    	
    	String str =""+(length/1000)+","+s+" KB";
    	
    	
    	
    	return str;
    }

    private void updateDownloadInfo(FeedItem item){
        TextView title = (TextView) DownloadingActivity.this.findViewById(R.id.title);  
        TextView dl_status = (TextView) DownloadingActivity.this.findViewById(R.id.dl_status);  
        ProgressBar progress = (ProgressBar) findViewById(R.id.progress);  
    	
        title.setText(item.title);
        if(item.length>0){
            double d = 100.0*item.offset/item.length;
            
            int status = (int)d;
            
            String str = ""+status+"% ( "+(formatLength(item.offset))+" / "
            			+(formatLength((int)item.length))+" )";
            
            dl_status.setText(str);
            progress.setProgress(status);          	
        	
        }else{
        	dl_status.setText("0% ( 0 KB / 0 KB )");
        	
        	progress.setProgress(0); 
        }
        	
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_RESTART, 0, getResources().getString(R.string.menu_refresh)).setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(0, MENU_PREF, 1, getResources().getString(R.string.menu_pref)).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_BACK, 2, getResources().getString(R.string.menu_back)).setIcon(android.R.drawable.ic_menu_revert);
        
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_RESTART:
        	serviceBinder.start_download();  
        	return true;
        case MENU_BACK:
            finish();
            return true;
       
        case MENU_PREF:
            startActivity(new Intent(this, Pref.class));
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
			startActivity(new Intent(Intent.ACTION_EDIT, uri));
		}        
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);
        setTitle("Downloading");
        
        getListView().setOnCreateContextMenuListener(this);
        
        Intent intent = getIntent();
        intent.setData(ItemColumns.URI);
		String where = ItemColumns.STATUS + ">"
		+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW + " AND "
		+ ItemColumns.STATUS + "<"
		+ ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;        
		String order = ItemColumns.STATUS + " DESC, " + ItemColumns.LAST_UPDATE + " ASC";
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, where, null , 
        		order);

        // Used to map notes entries from the database to views
        mAdapter = new MyListCursorAdapter(this, R.layout.download_item, cursor,
                new String[] { ItemColumns.TITLE,ItemColumns.OFFSET,ItemColumns.LENGTH, ItemColumns.STATUS   },
                new int[] { R.id.dtext1,R.id.dtext2,R.id.dtext3});
        setListAdapter(mAdapter);
        
        service = startService(new Intent(this, ReadingService.class));

        // bind service:
        Intent bindIntent = new Intent(this, ReadingService.class);
        bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        registerReceiver(mDownloadStatusReceiver, new IntentFilter(ReadingService.UPDATE_DOWNLOAD_STATUS));
        

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mDownloadStatusReceiver);
        
        unbindService(serviceConnection);
        //stopService(new Intent(this, service.getClass()));
    }

    private void refreshItems() {

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
        FeedItem feed_item = new FeedItem(getContentResolver(),info.id);
        if(feed_item== null){
        	return;
        }

        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Add a menu item to delete the note
        menu.add(0, MENU_ITEM_REMOVE, 0, R.string.menu_cancel);
        
        if(feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE){
            menu.add(0, MENU_ITEM_PAUSE, 0, R.string.menu_pause);
        	
        }else if(feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE){
        	menu.add(0, MENU_ITEM_RESUME, 0, R.string.menu_resume);
        }


        
        
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
        case MENU_ITEM_REMOVE: {
            //TODO are you sure?
        	
            FeedItem feed_item = new FeedItem(getContentResolver(),info.id);
            if(feed_item == null)
            	return true;
            if(feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE
            		&& feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE){
				Toast.makeText(this, "failed!",
						Toast.LENGTH_SHORT).show();	 
				return true;
            }else{
            	feed_item.status = ItemColumns.ITEM_STATUS_READ;
            	feed_item.update(getContentResolver());
            }
            
            try{
                	File file = new File(feed_item.pathname);
         
                	boolean deleted = file.delete();                		

            }catch(Exception e){
            	log.warn("del file failed : "+ feed_item.pathname+ "  "+ e);
            	
            }

            return true;
        }
        case MENU_ITEM_PAUSE: {
                    	
            FeedItem feed_item = new FeedItem(getContentResolver(),info.id);
            if(feed_item == null)
            	return true;
            if(feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE){
				Toast.makeText(this, "failed!",
						Toast.LENGTH_SHORT).show();	 
				return true;
            }else{
            	feed_item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
            	feed_item.update(getContentResolver());
            }

            return true;
        }        
        case MENU_ITEM_RESUME: {
                    	
            FeedItem feed_item = new FeedItem(getContentResolver(),info.id);
            if(feed_item == null)
            	return true;
            if(feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE){
				Toast.makeText(this, "failed!",
						Toast.LENGTH_SHORT).show();	 
				return true;
            }else{
            	feed_item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
            	feed_item.update(getContentResolver());
            }


            return true;
        }             
    }
        return false;
    }        
   

}
