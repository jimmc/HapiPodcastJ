package info.xuluan.podcast;

import info.xuluan.podcast.utils.Log;
import android.content.Intent;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class FlingGestureDetector extends SimpleOnGestureListener {

	private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;

	private Flingable flingable;

	private final Log log = Log.getLog(getClass());
	
	public FlingGestureDetector(Flingable f) {
		flingable = f;
	}
	
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		//log.debug("onFling");
		try {
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;	//swipe was not horizontal
			if (Math.abs(velocityX) <= SWIPE_THRESHOLD_VELOCITY)
				return false;	//too slow
			if (Math.abs(e1.getX() - e2.getX()) <= SWIPE_MIN_DISTANCE)
				return false;	//too short
			// right to left swipe
			if(e1.getX() > e2.getX()) {
				Intent nextIntent = flingable.nextIntent();
				if (nextIntent!=null) {
					// right to left swipe, move to the activity on the right
					flingable.startActivity(nextIntent);
					flingable.finish();
					return true;
				}
			} else if (e2.getX() > e1.getX()) {
				Intent prevIntent = flingable.prevIntent();
				if (prevIntent!=null) {
					// left to right swipe, move to the activity on the left
					flingable.startActivity(prevIntent);
					flingable.finish();
					return true;
				}
			}
		} catch (Exception e) {
			// nothing
		}
		return false;
	}

	@Override
	public boolean onDown(MotionEvent ev)
	{
		//We need to return true to ensure that our onFLing method gets called.
		return true;
	}
	
	public View.OnTouchListener createOnTouchListener() {
        final GestureDetector gestureDetector = new GestureDetector(this);
        View.OnTouchListener gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
            	log.debug("fling MotionEvent: "+event);
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
        return gestureListener;
	}
}	
