package info.xuluan.podcast;

import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.widget.SimpleCursorAdapter;

public class PodcastBaseActivity extends HapiListActivity {

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	
	public static final int COLUMN_INDEX_TITLE = 1;
	

	protected  static PodcastService mServiceBinder = null;
	protected final Log log = Log.getLog(getClass());

	protected SimpleCursorAdapter mAdapter;
	protected Cursor mCursor = null;

	protected static ComponentName mService = null;
	
	protected boolean mInit = false;
	protected Intent mPrevIntent = null;
	
	protected Intent mNextIntent = null;
	
	protected GestureDetector gestureDetector;
	protected View.OnTouchListener gestureListener;	
	
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        	//log.debug("onFling");
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	if(mPrevIntent!=null)
                		startActivity(mPrevIntent);
                	finish();

                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                	if(mPrevIntent!=null)
                		startActivity(mNextIntent);
                	finish();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }	

	protected static ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mServiceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
			//log.debug("onServiceConnected");
		}

		public void onServiceDisconnected(ComponentName className) {
			mServiceBinder = null;
			//log.debug("onServiceDisconnected");
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		
	

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
	protected void onResume() {
		super.onResume();
		if (mInit) {
			mInit = false;

			if (mCursor != null)
				mCursor.close();

			unbindService(serviceConnection);

			startInit();

		}

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mInit = true;

		log.debug("onLowMemory()");
		finish();
	}

	public void startInit() {

		log.debug("startInit()");

		mService = startService(new Intent(this, PodcastService.class));

		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		
		
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        
        getListView().setOnTouchListener(gestureListener);	
	}
}
