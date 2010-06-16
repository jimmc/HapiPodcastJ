package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.DialogMenu;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import java.io.File;
import java.util.HashMap;

public class DownloadingActivity extends PodcastBaseActivity {

	private static final int COLUMN_INDEX_TITLE = 1;

	private static final int MENU_RESTART = Menu.FIRST + 1;

	private static final int MENU_ITEM_VIEW = Menu.FIRST + 9;	
	private static final int MENU_ITEM_REMOVE = Menu.FIRST + 10;
	private static final int MENU_ITEM_PAUSE = Menu.FIRST + 11;
	private static final int MENU_ITEM_RESUME = Menu.FIRST + 12;
	
    private static final int REFRESH = 1;
	
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
			ItemColumns.LENGTH, ItemColumns.STATUS, };



	class MyListCursorAdapter extends SimpleCursorAdapter {

		protected int[] mFrom2;
		protected int[] mTo2;
		private HashMap<Integer, Integer> mIconMap;

		MyListCursorAdapter(Context context, int layout, Cursor cursor,
				String[] from, int[] to) {
			super(context, layout, cursor, from, to);

			mIconMap = new HashMap<Integer, Integer>();
			mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE,
					R.drawable.waiting);
			mIconMap.put(ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE,
					R.drawable.pause);

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
			final View[] holder = new View[count + 1];

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
			int offset = 0;
			int length = -1;

			for (int i = 0; i < count + 1; i++) {
				final View v = holder[i];
				// log.debug("offset = "+ offset+" length = "+length);
				if (i == count) {
					View v_icon = view.findViewById(R.id.icon);
					int status = cursor.getInt(from[i]);

					setViewImage2((ImageView) v_icon, mIconMap.get(status));

					break;
				} else if (i == 1) {
					offset = cursor.getInt(from[i]);
					if (v != null) {
						setViewText((TextView) v, "");

					}
				} else if (i == 2) {
					length = cursor.getInt(from[i]);
					String str = "0% ( 0 KB / 0 KB )";
					if (length > 0) {

						double d = 100.0 * offset / length;

						int status = (int) d;

						str = "" + status + "% ( " + (formatLength(offset))
								+ " / " + (formatLength(length)) + " )";
					}

					// log.debug("str = "+ str);

					if (v != null) {
						setViewText((TextView) v, str);

					}

					continue;
				} else {
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.download);
		setTitle(getResources().getString(R.string.title_download_list));

		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		mPrevIntent = new Intent(this, MainActivity.class);
		mNextIntent = new Intent(this, PlayListActivity.class);			
		startInit();

		

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
			DialogMenu dialog_menu = createDialogMenus(id);
			if( dialog_menu==null)
				return;
			
			
			 new AlertDialog.Builder(this)
             .setTitle(dialog_menu.getHeader())
             .setItems(dialog_menu.getItems(), new DLClickListener(dialog_menu,id)).show();		
			
		}
	}

	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE) {
			dialog_menu.addMenu(MENU_ITEM_PAUSE, 
					getResources().getString(R.string.menu_pause));			
		} else if (feed_item.status == ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE) {
			dialog_menu.addMenu(MENU_ITEM_RESUME, 
					getResources().getString(R.string.menu_resume));							
		}		
		
		dialog_menu.addMenu(MENU_ITEM_VIEW, 
				getResources().getString(R.string.menu_view));
	
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
    		case MENU_ITEM_VIEW: {
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
    			if (feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE
    					&& feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE) {
    				Toast.makeText(DownloadingActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return;
    			} else {
    				feed_item.status = ItemColumns.ITEM_STATUS_READ;
    				feed_item.update(getContentResolver());
    			}

    			try {
    				File file = new File(feed_item.pathname);

    				boolean deleted = file.delete();

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
    			if (feed_item.status != ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE) {
    				Toast.makeText(DownloadingActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return;
    			} else {
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
    				Toast.makeText(DownloadingActivity.this, getResources().getString(R.string.fail), Toast.LENGTH_SHORT).show();
    				return ;
    			} else {
    				feed_item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
    				feed_item.update(getContentResolver());
    			}
    			mServiceBinder.start_download();

    			return ;
    		}
    		}        	
        	
        }		
	}	

	private static String formatLength(int length) {

		length /= 1024;

		int i = (length % 1000);
		String s = "";
		if (i < 10) {
			s = "00" + i;
		} else if (i < 100) {
			s = "0" + i;
		} else {
			s += i;
		}

		String str = "" + (length / 1000) + "," + s + " KB";

		return str;
	}

	private void updateDownloadInfo(FeedItem item) {
		
		TextView title = (TextView) DownloadingActivity.this
				.findViewById(R.id.title);
		TextView dl_status = (TextView) DownloadingActivity.this
				.findViewById(R.id.dl_status);
		ProgressBar progress = (ProgressBar) findViewById(R.id.progress);
		
		if(item==null){
			title.setText("");
			dl_status.setText("0% ( 0 KB / 0 KB )");
			progress.setProgress(0);			
		}else{

			title.setText(item.title);
			if (item.length > 0) {
				double d = 100.0 * item.offset / item.length;
	
				int status = (int) d;
	
				String str = "" + status + "% ( " + (formatLength(item.offset))
						+ " / " + (formatLength((int) item.length)) + " )";
	
				dl_status.setText(str);
				progress.setProgress(status);
	
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
		mAdapter = new MyListCursorAdapter(this, R.layout.download_item,
				mCursor, new String[] { ItemColumns.TITLE, ItemColumns.OFFSET,
						ItemColumns.LENGTH, ItemColumns.STATUS }, new int[] {
						R.id.dtext1, R.id.dtext2, R.id.dtext3 });
		setListAdapter(mAdapter);

		super.startInit();
	}
}
