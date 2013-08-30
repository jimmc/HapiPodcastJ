package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;
import info.xuluan.podcast.utils.StrUtils;
import info.xuluan.podcastj.R;

import java.io.File;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadActivity extends PodcastBaseActivity implements PodcastTab {

	private static final int MENU_RESTART = Menu.FIRST + 1;

	private static final int MENU_ITEM_DETAILS = Menu.FIRST + 9;	
	private static final int MENU_ITEM_REMOVE = Menu.FIRST + 10;
	private static final int MENU_ITEM_PAUSE = Menu.FIRST + 11;
	private static final int MENU_ITEM_RESUME = Menu.FIRST + 12;
	
    private static final int REFRESH = 1;
	private static HashMap<Integer, Integer> mIconMap;
    static {
    	mIconMap = new HashMap<Integer, Integer>();
    	EpisodeIcons.initFullIconMap(mIconMap);
    }
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
            		if (mServiceBinder != null)
            			updateDownloadInfo(mServiceBinder.getDownloadingItem());
                    queueNextRefresh(500);
                    
                    break;

                default:
                    break;
            }
        }
    };  
    
    private void queueNextRefresh(long delay) {
        Message msg = mHandler.obtainMessage(REFRESH);
        mHandler.removeMessages(REFRESH);
       	mHandler.sendMessageDelayed(msg, delay);
    }    
    
	
	private static final String[] PROJECTION = new String[] {
			ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.OFFSET,
			ItemColumns.LENGTH, ItemColumns.STATUS, ItemColumns.KEEP };

	private int offset;
	class OffsetFieldHandler implements IconCursorAdapter.FieldHandler {
		public void setViewValue(IconCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			offset = cursor.getInt(fromColumnId);
			if (v != null) {
				adapter.setViewText((TextView) v, "");
			}
		}
	}
	class LengthFieldHandler implements IconCursorAdapter.FieldHandler {
		public void setViewValue(IconCursorAdapter adapter, Cursor cursor,
				View v, int fromColumnId) {
			int length = cursor.getInt(fromColumnId);
			String str = "0% ( 0 KB / 0 KB )";
			if (length > 0) {
				str = StrUtils.formatDownloadString( offset , length);				
			}

			// log.debug("str = "+ str);

			if (v != null) {
				adapter.setViewText((TextView) v, str);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download_activity);
		setTitle(getResources().getString(R.string.title_episodes));

		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		TabsHelper.setEpisodeTabClickListeners(this, R.id.episode_bar_download_button);
		findViewById(R.id.dl_group).setOnClickListener(new CurrentClickListener());
		startInit();		

	}
	
	class CurrentClickListener implements OnClickListener {
		public void onClick(View v) {
			FeedItem item = mServiceBinder.getDownloadingItem();
			if (item==null)
				Toast.makeText(DownloadActivity.this, "No current download", Toast.LENGTH_SHORT).show();
			else {
				//Toast.makeText(DownloadActivity.this, "Touch!", Toast.LENGTH_SHORT).show();
				showListItemMenu(item.id);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mServiceBinder == null)
			return;
		/*
		FeedItem item = mServiceBinder.getDownloadingItem();
		if (item != null)
			updateDownloadInfo(item);
		*/
		queueNextRefresh(1);
		mServiceBinder.start_download();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_RESTART, 0,
				getResources().getString(R.string.menu_refresh)).setIcon(
				android.R.drawable.ic_menu_rotate);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_RESTART:
			mServiceBinder.start_download();
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
			showListItemMenu(id);
		}
	}

	private void showListItemMenu(long id) {
		DialogMenu dialog_menu = createDialogMenus(id);
		if( dialog_menu==null)
			return;			
		new AlertDialog.Builder(this)
           .setTitle(dialog_menu.getHeader())
           .setItems(dialog_menu.getItems(), new DLClickListener(dialog_menu,id)).show();					
	}
	
	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE ||
				feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOADING_NOW) {
			dialog_menu.addMenu(MENU_ITEM_PAUSE, 
					getResources().getString(R.string.menu_pause));			
		} else if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE) {
			dialog_menu.addMenu(MENU_ITEM_RESUME, 
					getResources().getString(R.string.menu_resume));							
		}		
		
		dialog_menu.addMenu(MENU_ITEM_DETAILS, 
				getResources().getString(R.string.menu_details));
	
		dialog_menu.addMenu(MENU_ITEM_REMOVE, 
				getResources().getString(R.string.menu_cancel));

		return dialog_menu;
	}
	
	class DLClickListener implements DialogInterface.OnClickListener {
		
		public DialogMenu mMenu;
		public long item_id;
		public DLClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}

		
        public void onClick(DialogInterface dialog, int select) {
        	
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_DETAILS: {
    			Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));   
    			return;
    		}
    		
    		case MENU_ITEM_REMOVE: {
    			// TODO are you sure?

    			FeedItem feed_item = FeedItem
    					.getById(getContentResolver(), item_id);
    			if (feed_item == null)
    				return;
    			Boolean okToRemove = feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE ||
    					feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE ||
    					feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
    			if (!okToRemove) {
    				Toast.makeText(DownloadActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return;
    			} else {
    				if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOADING_NOW) {
    					mServiceBinder.stop_download();
    					updateDownloadInfo(null);
    					feed_item = FeedItem.getById(getContentResolver(), item_id);
    				}
    				feed_item.status = ItemColumns.ITEM_STATUS_READ;
    				feed_item.update(getContentResolver());
    			}

    			try {
    				File file = new File(feed_item.pathname);

    				boolean deleted = file.delete();
    				feed_item.offset = 0;
    				feed_item.update(getContentResolver());

    			} catch (Exception e) {
    				log.warn("del file failed : " + feed_item.pathname + "  " + e);

    			}

    			return ;
    		}
    		case MENU_ITEM_PAUSE: {

    			FeedItem feed_item = FeedItem
    					.getById(getContentResolver(), item_id);
    			if (feed_item == null)
    				return;
    			Boolean okToPause = feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE ||
    					feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
    			if (!okToPause) {
    				Toast.makeText(DownloadActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return;
    			} else {
    				if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOADING_NOW) {
    					mServiceBinder.stop_download();
    					updateDownloadInfo(null);
    					feed_item = FeedItem.getById(getContentResolver(), item_id);
    				}
    				feed_item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
    				feed_item.update(getContentResolver());
    			}

    			return;
    		}
    		case MENU_ITEM_RESUME: {

    			FeedItem feed_item = FeedItem
    					.getById(getContentResolver(), item_id);
    			if (feed_item == null)
    				return;
    			if (feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE) {
    				Toast.makeText(DownloadActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return ;
    			} else {
    				feed_item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
    				feed_item.update(getContentResolver());
    			}
    			mServiceBinder.start_download();
    			updateDownloadInfo(mServiceBinder.getDownloadingItem());

    			return ;
    		}
    		}        	
        	
        }		
	}	


	private void updateDownloadInfo(FeedItem item) {
		TextView title = (TextView) DownloadActivity.this
				.findViewById(R.id.title);
		TextView dl_status = (TextView) DownloadActivity.this
				.findViewById(R.id.dl_status);
		TextView dl_op = (TextView) DownloadActivity.this
				.findViewById(R.id.dl_op);
		ProgressBar progress = (ProgressBar) findViewById(R.id.progress);

		String downloadingStatus = mServiceBinder.getDownloadingStatus();
		if(item==null){
			title.setText("");
			dl_op.setText(downloadingStatus);
			dl_status.setText(""/*"0% ( 0 KB / 0 KB )"*/);
			progress.setProgress(0);			
		}else{

			title.setText(item.title);
			dl_op.setText(downloadingStatus);
			if (item.length > 0) {
				String str = StrUtils.formatDownloadString( item.offset , item.length);
				dl_status.setText(str);
				progress.setProgress(StrUtils.formatDownloadPrecent(item.offset , item.length));
	
			} else {
				dl_status.setText("0% ( 0 KB / 0 KB )");
	
				progress.setProgress(0);
			}
		}
	}
	
    
	@Override
	public void startInit() {

		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW + " AND "
				+ ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
		String order = ItemColumns.STATUS + " DESC, " + ItemColumns.LAST_UPDATE
				+ " ASC";
		mCursor = managedQuery(getIntent().getData(), PROJECTION, where, null,
				order);

		// Used to map notes entries from the database to views
		String[] fromColNames = { ItemColumns.TITLE, ItemColumns.OFFSET,
				ItemColumns.LENGTH, ItemColumns.STATUS, ItemColumns.KEEP };
		int[] toColIds = { R.id.dtext1, R.id.dtext2, R.id.dtext3, R.id.icon, R.id.keep_icon };
		IconCursorAdapter.FieldHandler[] fieldHandlers = {
				IconCursorAdapter.defaultTextFieldHandler,
				new OffsetFieldHandler(),
				new LengthFieldHandler(),
				new IconCursorAdapter.IconFieldHandler(mIconMap),
				new IconCursorAdapter.IconFieldHandler(EpisodeIcons.mKeepIconMap)
				};
		mAdapter = new IconCursorAdapter(this, R.layout.download_item, mCursor,
				fromColNames, toColIds, fieldHandlers);

		setListAdapter(mAdapter);

		super.startInit();
	}
	
	//PodcastTab interface
	public int iconResource() { return R.drawable.download_big_pic; }
	public int tabLabelResource(boolean isLandscape) { return R.string.episode_bar_button_download; }
}
