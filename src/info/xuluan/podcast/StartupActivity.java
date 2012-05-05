package info.xuluan.podcast;

import info.xuluan.podcast.utils.Log;
import android.os.Bundle;
import android.os.Handler;

public class StartupActivity extends HapiActivity {
	
	private final Log log = Log.getLog(getClass());
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log.debug("In StartupActivity");
		setContentView(R.layout.startup);

		long delayMillis = 2000;
	    Handler handler = new Handler(); 
	    handler.postDelayed(new Runnable() { 
	         public void run() { 
	              delayedStart(); 
	         } 
	    }, delayMillis);
	}
	
	private void delayedStart() {
		tapHome();
	}
}
