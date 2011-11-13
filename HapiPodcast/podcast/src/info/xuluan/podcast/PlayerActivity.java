package info.xuluan.podcast;

import java.text.DecimalFormat;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.service.PlayerService;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.StrUtils;


public class PlayerActivity  extends ListActivity 
{
	protected  static PlayerService mServiceBinder = null;
	protected final Log log = Log.getLog(getClass());
	protected static ComponentName mService = null;
	
	private static final int MENU_OPEN_AUDIO = Menu.FIRST + 1;
	private static final int MENU_REPEAT = Menu.FIRST + 2;
	private static final int MENU_LOAD_ALL = Menu.FIRST + 3;
	private static final int MENU_LOAD_BY_CHANNEL = Menu.FIRST + 4;	
	private static final int MENU_REMOVE_ALL = Menu.FIRST + 5;
	
	private static final int MENU_PLAY = Menu.FIRST + 6;
	private static final int MENU_READ = Menu.FIRST + 7;	
	private static final int MENU_REMOVE = Menu.FIRST + 8;	

	private static final int MENU_MOVE_UP = Menu.FIRST + 9;	
	private static final int MENU_MOVE_DOWN = Menu.FIRST + 10;	
	
	private static final int STATE_MAIN = 0;
	private static final int STATE_VIEW = 1;
	
	private long rwnd_interval = 7*1000;
	private long ffwd_interval = 30*1000;
		

	private boolean mShow = false;
	private long mID;
	private long pref_repeat;
	private long pref_rwnd_interval;
	private long pref_fas_fwd_interval;
	private String mTitle = "Player";
	//private FeedItem mCurrentItem;

	protected SimpleCursorAdapter mAdapter;
	protected Cursor mCursor = null;
	private static HashMap<Integer, Integer> mIconMap;
	

	private ImageButton mRwndButton;
    private ImageButton mFfwdButton;
	
    private ImageButton mPauseButton;
    private ImageButton mPrevButton;
    private ImageButton mNextButton;

	private TextView mCurrentTime;
	private TextView mTotalTime;	
	private ProgressBar mProgress;
	
	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
		ItemColumns.TITLE, // 1
		ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS, // 1

	};
	
	static {
		mIconMap = new HashMap<Integer, Integer>();
		AllItemActivity.initFullIconMap(mIconMap);
/*
		mIconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.music);
		mIconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.music);		
*/
		}	
	

	protected static ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceBinder = ((PlayerService.PlayerBinder) service)
					.getService();
			//log.debug("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBinder = null;
			//log.debug("onServiceDisconnected");
		}
	};	
    
    private long mLastSeekEventTime;
    private boolean mFromTouch;
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
            log.debug("mFromTouch = false; ");
            
        }
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            log.debug("onProgressChanged");
       	
            if (!fromuser || (mServiceBinder == null)) return;

            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                //mPosOverride = mp.duration * progress / 1000;
                try {
                	if(mServiceBinder.isInitialized())
                		mServiceBinder.seek(mServiceBinder.duration() * progress / 1000);
                } catch (Exception ex) {
                }

                if (!mFromTouch) {
                    refreshNow();
                    //mPosOverride = -1;
                }
            }
            
        }
        
        public void onStopTrackingTouch(SeekBar bar) {
            //mPosOverride = -1;
            mFromTouch = false;
            log.debug("mFromTouch = false; ");

        }
    };  
    
    private static final int REFRESH = 1;
    private static final int PLAYITEM = 2;

    private void queueNextRefresh(long delay) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            if(mShow)
            	mHandler.sendMessageDelayed(msg, delay);
    }
    

    
    private void play(FeedItem item) {
    	if(item==null)
    		return;
		if(mServiceBinder!=null)
			mServiceBinder.play(item.id);
		
        updateInfo();
		
    }    

    private long refreshNow() {
        if(mServiceBinder == null)
            return 500;
        if(mID>=0){
        	startPlay();
        }
        if(mServiceBinder.getUpdateStatus()){
        	updateInfo();
        	mServiceBinder.setUpdateStatus(false);
        	
        }
        try {
        	if(mServiceBinder.isInitialized()==false){
                mCurrentTime.setVisibility(View.INVISIBLE);
                mTotalTime.setVisibility(View.INVISIBLE);
                mProgress.setProgress(0);
                return 500;
        	}
        	long pos = mServiceBinder.position();
        	long duration = mServiceBinder.duration();
            
            //mTotalTime.setVisibility(View.VISIBLE);
            //mTotalTime.setText(formatTime( duration ));
            
        	if(mServiceBinder.isPlaying() == false) {
                mCurrentTime.setVisibility(View.VISIBLE);
                mCurrentTime.setText(StrUtils.formatTime( pos ));

                mProgress.setProgress((int) (1000 * pos / duration));
                return 500;
        	}
        	
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (duration > 0)) {
                mCurrentTime.setText(StrUtils.formatTime( pos ));
                
                if (mServiceBinder.isInitialized()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                    //mTotalTime.setVisibility(View.VISIBLE);
                } 

                mProgress.setProgress((int) (1000 * pos / mServiceBinder.duration()));
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }

            return remaining;
        } catch (Exception ex) {
        }
        return 500;
    }    
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    log.debug("REFRESH: "+next);
                    break;

                default:
                    break;
            }
        }
    };  
    
    private void updateInfo() {
    	FeedItem item;
    	if(mServiceBinder == null){
            mTotalTime.setVisibility(View.INVISIBLE);
    		setTitle(mTitle);	
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);    		
    		return;

    	}
    	
    	if(mServiceBinder.isInitialized() == false){
            mTotalTime.setVisibility(View.INVISIBLE);
    		setTitle(mTitle);	
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
            return;
    	}

    	item = mServiceBinder.getCurrentItem();
    	if(item==null){
    		log.error("isInitialized but no item!!!");
    		return;
    	}
    	
        mTotalTime.setVisibility(View.VISIBLE);
        mTotalTime.setText(StrUtils.formatTime( mServiceBinder.duration() ));    
		setTitle(item.title);	
        
    	if(mServiceBinder.isPlaying() == false){    	
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
    	} else {
            mPauseButton.setImageResource(android.R.drawable.ic_media_pause);   		
    	}

    }    

    private void doPauseResume() {
        try {
            if(mServiceBinder != null) {
            	if(mServiceBinder.isInitialized()){
	                if (mServiceBinder.isPlaying()) {
	                	mServiceBinder.pause();
	                } else {
	                	mServiceBinder.start();
	                }
            	}
                refreshNow();
                updateInfo();
            }
        } catch (Exception ex) {
        }
    }
    
    private View.OnClickListener mNextListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (mServiceBinder != null && mServiceBinder.isInitialized()) {
                	mServiceBinder.next();

                }
            } catch (Exception ex) {
            }   
            
            updateInfo();

        }
    };   
    
    private View.OnClickListener mPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
        }
    };
    
    private View.OnClickListener mPrevListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (mServiceBinder != null && mServiceBinder.isInitialized()) {
                	if(mServiceBinder.position()>5000)
                		mServiceBinder.seek( 0 );
                	else{
                		mServiceBinder.prev();
                	}
                }
            } catch (Exception ex) {
            } 
            updateInfo();

       }
    };    
    
    
    private View.OnClickListener mRwndListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (mServiceBinder != null && mServiceBinder.isInitialized()) {
                	long pos = mServiceBinder.position();
                	long newPos = pos - rwnd_interval;
                	if (newPos<0) newPos = 0;
                	mServiceBinder.seek( newPos );

                }
            } catch (Exception ex) {
            } 
            updateInfo();
       }
    };    
    
    private View.OnClickListener mFfwdListener = new View.OnClickListener() {
        public void onClick(View v) {
            try {
                if (mServiceBinder != null && mServiceBinder.isInitialized()) {
                	long pos = mServiceBinder.position();
                	mServiceBinder.seek( pos+ ffwd_interval );

                }
            } catch (Exception ex) {
            } 
            updateInfo();
       }
    };    

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		startService(new Intent(this, PlayerService.class));
        setContentView(R.layout.audio_player);
		getListView().setOnCreateContextMenuListener(this);
        
        final Intent intent = getIntent();
        mID = intent.getLongExtra("item_id", -1);
        
       
        mPauseButton = (ImageButton) findViewById(R.id.pause);
        mPauseButton.requestFocus();
        mPauseButton.setOnClickListener(mPauseListener);

        mRwndButton = (ImageButton) findViewById(R.id.rwnd);
        mRwndButton.requestFocus();
        mRwndButton.setOnClickListener(mRwndListener);        
        
        mFfwdButton = (ImageButton) findViewById(R.id.ffwd);
        mFfwdButton.requestFocus();
        mFfwdButton.setOnClickListener(mFfwdListener);        
        
        mPrevButton = (ImageButton) findViewById(R.id.prev);
        mPrevButton.requestFocus();
        mPrevButton.setOnClickListener(mPrevListener);        
        
        mNextButton = (ImageButton) findViewById(R.id.next);
        mNextButton.requestFocus();
        mNextButton.setOnClickListener(mNextListener);        
        
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = (SeekBar) mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);    
        mTotalTime = (TextView) findViewById(R.id.totaltime); 
        mCurrentTime = (TextView) findViewById(R.id.currenttime); 
        startInit();

        //updateInfo();
   
    }
    
	public void startInit() {

		mService = startService(new Intent(this, PlayerService.class));
		Intent bindIntent = new Intent(this, PlayerService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		
		String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
		+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
		+ " AND " + ItemColumns.FAIL_COUNT + " > 100";
		
		String order = ItemColumns.FAIL_COUNT + " ASC";

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, order);

		mAdapter = new IconCursorAdapter(this, R.layout.channel_list_item, mCursor,
				new String[] { ItemColumns.TITLE,ItemColumns.STATUS }, new int[] {
						R.id.text1}, mIconMap);
		setListAdapter(mAdapter);

	}   
	
	@Override
	protected void onResume() {
		super.onResume();
        getPref();
		
		mShow = true;
        if(mID>=0) {
        	startPlay();
        }
        queueNextRefresh(1);
        updateInfo();

	}

	@Override
	protected void onPause() {
		super.onPause();
		mShow = false;

	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
		finish();
	}	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			unbindService(serviceConnection);
		} catch (Exception e) {
			e.printStackTrace();

		}

		// stopService(new Intent(this, service.getClass()));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_REPEAT, 0,
				getResources().getString(R.string.menu_repeat)).setIcon(
				android.R.drawable.ic_menu_rotate);
		menu.add(0, MENU_LOAD_ALL, 1,
				getResources().getString(R.string.menu_load_all)).setIcon(
				android.R.drawable.ic_menu_agenda);		
		menu.add(0, MENU_LOAD_BY_CHANNEL, 2,
				getResources().getString(R.string.menu_load_by_channel)).setIcon(
				R.drawable.ic_menu_mark);		
		menu.add(0, MENU_REMOVE_ALL, 3,
				getResources().getString(R.string.menu_remove_all)).setIcon(
				R.drawable.ic_menu_clear_playlist);			
	
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case MENU_REPEAT:
			getPref();
			 new AlertDialog.Builder(this)
             .setTitle("Chose Repeat Mode")
             .setSingleChoiceItems(R.array.repeat_select, (int) pref_repeat, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int select) {
         			
                	pref_repeat = select;
         			SharedPreferences prefsPrivate = getSharedPreferences("info.xuluan.podcast_preferences", Context.MODE_PRIVATE);
    				Editor prefsPrivateEditor = prefsPrivate.edit();
    				prefsPrivateEditor.putLong("pref_repeat", pref_repeat);
    				prefsPrivateEditor.commit();
         			dialog.dismiss();
    				
                 }
             })
            .show();
			return true;
		case MENU_LOAD_ALL:
			loadItem(null);
 			return true;	
		case MENU_REMOVE_ALL:
			removeAll() ;
 			return true;	 
		case MENU_LOAD_BY_CHANNEL:
			 loadChannel();

 			return true; 			
		}
		return super.onOptionsItemSelected(item);
	}	
    
	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		
		dialog_menu.addMenu(MENU_PLAY, 
				getResources().getString(R.string.menu_play));
		
		dialog_menu.addMenu(MENU_MOVE_UP, 
				getResources().getString(R.string.menu_move_up));

		dialog_menu.addMenu(MENU_MOVE_DOWN, 
				getResources().getString(R.string.menu_move_down));
		
		dialog_menu.addMenu(MENU_READ, 
				getResources().getString(R.string.menu_view));		

		dialog_menu.addMenu(MENU_REMOVE, 
				getResources().getString(R.string.menu_remove));				

		return dialog_menu;
	}		
	
	class PlayClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public PlayClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		FeedItem feeditem = FeedItem.getById(getContentResolver(), item_id);
    		if (feeditem == null)
    			return;   
    		
    		switch (mMenu.getSelect(select)) {
			case MENU_PLAY: {
				play(feeditem);
				return ;
			}
			case MENU_MOVE_UP: {
		    	if(mServiceBinder != null){
		    		FeedItem pre = mServiceBinder.getPrev(feeditem);
		    		if(pre!=null){
		    			long ord = pre.failcount;
		    			pre.addtoPlaylistByOrder(getContentResolver(), feeditem.failcount);
		    			feeditem.addtoPlaylistByOrder(getContentResolver(), ord);
		    		}
		    	}
				return ;
			}	
			case MENU_MOVE_DOWN: {
		    	if(mServiceBinder != null){
		    		FeedItem next = mServiceBinder.getNext(feeditem);
		    		if(next!=null){
		    			long ord = next.failcount;
		    			next.addtoPlaylistByOrder(getContentResolver(), feeditem.failcount);
		    			feeditem.addtoPlaylistByOrder(getContentResolver(), ord);
		    		}
		    	}
				return ;
			}			
			case MENU_READ: {
				Uri uri = ContentUris.withAppendedId(ItemColumns.URI, feeditem.id);
				startActivity(new Intent(Intent.ACTION_EDIT, uri));
				return ;
			}
			case MENU_REMOVE: {
		    	if(mServiceBinder != null){
		    		FeedItem curr = mServiceBinder.getCurrentItem();
		    		if(curr!=null) {
		    			if(curr.id == feeditem.id)
		    				mServiceBinder.stop();
		    		}
		    	}
				feeditem.addtoPlaylistByOrder(getContentResolver(), 0);
				return ;
			}			
		}
      }
	}

	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, id);
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
             .setItems(dialog_menu.getItems(), new PlayClickListener(dialog_menu,id)).show();	
		}

	}	
    
    static DecimalFormat mTimeDecimalFormat = new DecimalFormat("00");
    
    private void startPlay() {
    	if(mServiceBinder!=null){
        	FeedItem item = FeedItem.getById(getContentResolver(), mID);
        	if(item!=null){
        		item.addtoPlaylist(getContentResolver());
            	play(item);        		
        	}

        	mID = -1;        		
    	}    	
    }
    
    private void loadChannel() {
    	String[] arr = new String[100];
    	final long[] id_arr = new long[100];
    	
		String where =  null;
		Cursor cursor = managedQuery(SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS, where, null, null);      	
		int size = 0;
		if(cursor!=null && cursor.moveToFirst()){
			do{
				Subscription sub = Subscription.getByCursor(cursor);
				if(sub!=null){
					arr[size] = new String(sub.title);
					id_arr[size] = sub.id;
					size++;
					if(size>=100)
						break;
				}
			}while (cursor.moveToNext());			
		}
		String[] select_arr = new String[size];
        for (int i = 0; i < size; i++) {
        	select_arr[i] = arr[i];
        }
        
		 new AlertDialog.Builder(this)
         .setTitle("Select Channel")
         .setSingleChoiceItems(select_arr, 0, new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface dialog, int select) {
     			loadItem(" AND " + ItemColumns.SUBS_ID+ "=" + id_arr[select]);

      			dialog.dismiss();
             }
         })
        .show();    	
    }
    
    private void loadItem(String channel_condition) {
    	
		String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
		+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
		+ " AND " + ItemColumns.FAIL_COUNT + " < 101 ";
		if(channel_condition!= null)
			where += channel_condition;
		
		final String sel = where;
		
		new Thread() {
			public void run() {
				Cursor cursor = null;
				try {

					String order = ItemColumns.FAIL_COUNT + " ASC";

					cursor = managedQuery(ItemColumns.URI, ItemColumns.ALL_COLUMNS, sel, null, order);  
					long ord = Long.valueOf(System.currentTimeMillis());

					if((cursor!=null) && cursor.moveToFirst()){
						do{
							FeedItem item = FeedItem.getByCursor(cursor);
							if(item!=null)
								item.addtoPlaylistByOrder(getContentResolver(), ord++);

						}while (cursor.moveToNext());			
					}


				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(cursor!=null)
						cursor.close();	
				}

			}
		}.start();			
		
	

	
		
    }
    
    private void removeAll() {
    	if(mServiceBinder!=null)
    		mServiceBinder.stop();
		String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
		+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
		+ " AND " + ItemColumns.FAIL_COUNT + " > 100 ";
		final String sel = where;

		
		new Thread() {
			public void run() {
				Cursor cursor = null;
				try {

					cursor = managedQuery(ItemColumns.URI, ItemColumns.ALL_COLUMNS, sel, null, null);  

					if((cursor!=null) && cursor.moveToFirst()){
						do{
							FeedItem item = FeedItem.getByCursor(cursor);
							if(item!=null)
								item.addtoPlaylistByOrder(getContentResolver(), 0);

						}while (cursor.moveToNext());			
					}    	

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if(cursor!=null)
						cursor.close();	
				}

			}
		}.start();	

    }
    
    
    private void getPref() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);
		pref_repeat = pref.getLong("pref_repeat",0);
		pref_fas_fwd_interval = Integer.parseInt(pref.getString("pref_fast_forward_interval","30"));		
		ffwd_interval = pref_fas_fwd_interval*1000;
		pref_rwnd_interval = Integer.parseInt(pref.getString("pref_rewind_interval","7"));		
		rwnd_interval = pref_rwnd_interval*1000;

	}    

}
