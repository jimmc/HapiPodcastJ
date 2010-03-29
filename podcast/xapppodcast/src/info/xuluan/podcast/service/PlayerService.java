package info.xuluan.podcast.service;


import java.io.File;
import java.io.IOException;

import info.xuluan.podcast.AudioPlayer;
import info.xuluan.podcast.R;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.Log;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class PlayerService extends Service {

	private static final int FADEIN = 0;
	private static final int TRACK_ENDED = 1;
	private static final int SERVER_DIED = 2;

	private static final long REPEAT_MODE_NO_REPEAT = 0;
	private static final long REPEAT_MODE_REPEAT = 1;
	private static final long REPEAT_MODE_REPEAT_ONE = 2;
	
	private final Log log = Log.getLog(getClass());
	
	MyPlayer mPlayer = null;
    private NotificationManager mNotificationManager;
	

	FeedItem mItem = null;
	boolean  mUpdate = false;
    private boolean mResumeAfterCall = false;
	

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int ringvolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (ringvolume > 0) {
                    mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (mItem!=null);
                    pause();
                }
            } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                // pause the music while a conversation is in progress
                mResumeAfterCall = (isPlaying() || mResumeAfterCall) && (mItem!=null);
                pause();
            } else if (state == TelephonyManager.CALL_STATE_IDLE) {
                // start playing again
                if (mResumeAfterCall) {
                    // resume playback only if music was playing
                    // when the call was answered
                    startAndFadeIn();
                    mResumeAfterCall = false;
                }
            }
        }
    };
    
    private void startAndFadeIn() {
        handler.sendEmptyMessageDelayed(FADEIN, 10);
    }
    
    private class MyPlayer {
        private MediaPlayer mMediaPlayer = new MediaPlayer();
        private Handler mHandler;
        private boolean mIsInitialized = false;

        public MyPlayer() {
            //mMediaPlayer.setWakeMode(PlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSourceAsync(String path) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setOnPreparedListener(preparedlistener);
                mMediaPlayer.prepareAsync();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
        }
        
        public void setDataSource(String path) {
            try {
                mMediaPlayer.reset();
                mMediaPlayer.setOnPreparedListener(null);
                mMediaPlayer.setDataSource(path);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                mIsInitialized = false;
                return;
            }
            mMediaPlayer.setOnCompletionListener(listener);
            mMediaPlayer.setOnErrorListener(errorListener);
            
            mIsInitialized = true;
        }
        
        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
            mMediaPlayer.start();
        }

        public void stop() {
            mMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mMediaPlayer.release();
            mIsInitialized = false;
        }

        public boolean isPlaying() {
            return mMediaPlayer.isPlaying();
        }
        
        public void pause() {
            mMediaPlayer.pause();
        }
        
        public void setHandler(Handler handler) {
            mHandler = handler;
        }
        

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
                //mWakeLock.acquire(30000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                //mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
            }
        };

        MediaPlayer.OnPreparedListener preparedlistener = new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                //notifyChange(ASYNC_OPEN_COMPLETE);
            }
        };
 
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    mIsInitialized = false;
                    mMediaPlayer.release();
                    // Creating a new MediaPlayer and settings its wakemode does not
                    // require the media service, so it's OK to do this now, while the
                    // service is still being restarted
                    mMediaPlayer = new MediaPlayer(); 
                    //mMediaPlayer.setWakeMode(AudioPlayer.this, PowerManager.PARTIAL_WAKE_LOCK);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                    return true;
                default:
                    break;
                }
                return false;
           }
        };

        public long duration() {
            return mMediaPlayer.getDuration();
        }

        public long position() {
            return mMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mMediaPlayer.setVolume(vol, vol);
        }
    }	

	private final Handler handler = new Handler() {
        float mCurrentVolume = 1.0f;
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
            case FADEIN:
                if (!isPlaying()) {
                    mCurrentVolume = 0f;
                    mPlayer.setVolume(mCurrentVolume);
                    start();
                    handler.sendEmptyMessageDelayed(FADEIN, 10);
                } else {
                    mCurrentVolume += 0.01f;
                    if (mCurrentVolume < 1.0f) {
                    	handler.sendEmptyMessageDelayed(FADEIN, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                }
                break;
			case TRACK_ENDED:
				long repeat_mode = getPref();
				
				if(mItem != null) {
					FeedItem next_item = getNext(mItem);
					
		            mNotificationManager.cancel(R.layout.audio_player);
					mItem.played(getContentResolver());
					mPlayer.stop();
					mUpdate = true;
					
					if(repeat_mode==REPEAT_MODE_REPEAT_ONE){
						long id = mItem.id;
						mItem = null;
						play(id);
					} else if(repeat_mode==REPEAT_MODE_REPEAT){
						if(next_item==null)	{
							next_item = getFirst();
						}
						
						if(next_item!=null)
							play(next_item.id);
						
					} else if(repeat_mode==REPEAT_MODE_NO_REPEAT){
						if(next_item!=null)
							play(next_item.id);
						
					}
										
				}

				
				break;
				
			case SERVER_DIED:
				break;

				
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
	    mPlayer = new MyPlayer();
	    mPlayer.setHandler(handler);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		log.debug("onCreate()");
		//ftriggerNextTimer(1);
	}
	

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		log.debug("onStart()");
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mPlayer!=null){
			mPlayer.release();
		}

		log.debug("onDestroy()");
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		log.debug("onLowMemory()");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
    private void notifyStatus() {

        // choose the ticker text
        String tickerText = mItem == null ? "player" : mItem.title;

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.notify_player, tickerText, 0);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, AudioPlayer.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("moodimg", R.drawable.notify_player),
                PendingIntent.FLAG_UPDATE_CURRENT);   
        
        notification.setLatestEventInfo(this, tickerText,
        		null, contentIntent);
        
        

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNotificationManager.notify(R.layout.audio_player, notification);
    }	
	
	public void play(long id) {
		if(getSDCardStatus()==false){
			Toast.makeText(this, getResources().getString(R.string.sdcard_unmout), Toast.LENGTH_LONG).show();
			return;
		}
		
		if(mItem != null) {
			if((mItem.id == id) && mPlayer.isInitialized()) {
				if(mPlayer.isPlaying() == false) {
					mPlayer.start();
				}
				return;
			}
			
			if(mPlayer.isPlaying()){
				mItem.updateOffset(getContentResolver(), mPlayer.position());
				mPlayer.stop();
			}

		}
		
		mItem = FeedItem.getById(getContentResolver(), id);
		if(mItem==null){
			return;
		}
		
		File file = new File(mItem.pathname);
		if (!file.exists()) {
			Toast.makeText(this, getResources().getString(R.string.audio_no_found), Toast.LENGTH_LONG).show();	
			return;
		}			

		mPlayer.setDataSource(mItem.pathname);
		int offset = mItem.offset < 0 ? 0: mItem.offset;
		mPlayer.seek(offset);
		start();
	}

	public void start() {
		if(mPlayer.isPlaying()==false) {
			notifyStatus();
			mPlayer.start();
		}
	}
	
	public void pause() {
		if(mPlayer.isPlaying()==false) {
			return;
		}
		
		if((mItem != null)) {
			mItem.updateOffset(getContentResolver(), mPlayer.position());	
		} else {
    		log.error("playing but no item!!!");

		}
        mNotificationManager.cancel(R.layout.audio_player);
		
		mPlayer.pause();
	}
	
	public void stop() {
		pause();
		mPlayer.stop();		
		mItem = null;
        mNotificationManager.cancel(R.layout.audio_player);
		mUpdate = true;		
	}	
	
	public boolean isInitialized() {
		return mPlayer.isInitialized();
	}		

	public boolean isPlaying() {
			return mPlayer.isPlaying();
	}	
	
	public long seek(long offset) {
		 offset = offset < 0 ? 0: offset;
		
		return mPlayer.seek(offset);

	}	
	
	public long position() {
		return mPlayer.position();
	}	
	
	
	public long duration() {
		return mPlayer.duration();
	}	
	
	public FeedItem getCurrentItem() {
		return mItem;
	}	
	
	public void prev() {
		FeedItem item = mItem;
		
		item = getPrev(item);
		if(item==null){
			item = getFirst();				
		}

		if(item==null){
			stop();
		} else {
			play(item.id);
		}
	
	}
	
	public void next() {
		FeedItem item = mItem;
		
		item = getNext(item);
		if(item==null){
			if(getPref()!=REPEAT_MODE_NO_REPEAT){
				item = getFirst();				
			}
		}

		if(item==null){
			stop();
		} else {
			play(item.id);
		}
		
		
	}	
	
	
	public boolean getUpdateStatus() {
		return mUpdate;
	}
	
	public void setUpdateStatus(boolean update) {
		mUpdate = update;
	}
	
	private FeedItem getFirst() {
		Cursor cursor = null;		
		try{
			String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
			+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
			+ " AND " + ItemColumns.FAIL_COUNT + " > 100";
			
			String order = ItemColumns.FAIL_COUNT + " ASC";

			cursor = getContentResolver().query(ItemColumns.URI, ItemColumns.ALL_COLUMNS, where, null, order);
			if(cursor==null){
				return null;
			}
			cursor.moveToFirst();
			
			FeedItem item = FeedItem.getByCursor(cursor);
			return item;
		} catch (Exception e) {
			
		} finally {
			if(cursor!=null)
				cursor.close();
		}
		
		return null;
	}
		
	public FeedItem getNext(FeedItem item) {
		FeedItem next_item = null;
		Cursor cursor = null;
		
		if(item == null){
			FeedItem.getByCursor(cursor);
			return null;			
		}		
		
		
		
		try{
			String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
			+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
			+ " AND " + ItemColumns.FAIL_COUNT + " > 100";
			
			String order = ItemColumns.FAIL_COUNT + " ASC";

			cursor = getContentResolver().query(ItemColumns.URI, ItemColumns.ALL_COLUMNS, where, null, order);
			if(cursor==null){
				return null;
			}
			cursor.moveToFirst();

			do{

				next_item = FeedItem.getByCursor(cursor);

				if((next_item!=null )&& (item.id==next_item.id)){
					if(cursor.moveToNext()){
						next_item = FeedItem.getByCursor(cursor);
						return next_item;						
					}

				}

			}while (cursor.moveToNext());

			
		} catch (Exception e) {
			
		} finally {
			if(cursor!=null)
				cursor.close();
		}
		
		return null;
		
	}

	public FeedItem getPrev(FeedItem item) {
		FeedItem prev_item = null;
		FeedItem curr_item = null;

		Cursor cursor = null;
		
		if(item == null){
			FeedItem.getByCursor(cursor);
			return null;			
		}
		
		try{
			String where =  ItemColumns.STATUS  + ">" + ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW 
			+ " AND " + ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW
			+ " AND " + ItemColumns.FAIL_COUNT + " > 100";
			
			String order = ItemColumns.FAIL_COUNT + " ASC";

			cursor = getContentResolver().query(ItemColumns.URI, ItemColumns.ALL_COLUMNS, where, null, order);
			if(cursor==null){
				return null;
			}
			cursor.moveToFirst();

			do{
				prev_item = curr_item;
				curr_item = FeedItem.getByCursor(cursor);

				if((curr_item!=null )&& (item.id==curr_item.id)){
					return prev_item;
				}

			}while (cursor.moveToNext());

			
		} catch (Exception e) {
			
		} finally {
			if(cursor!=null)
				cursor.close();
		}
		
		return null;		
	}
	
    private long getPref() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);
		return pref.getLong("pref_repeat",0);

	}
    
	private boolean getSDCardStatus()
	{
		if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			return true;
		}else{
			return false;
		}

	}    
    
	private final IBinder binder = new PlayerBinder();

	public class PlayerBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}	
	
}
