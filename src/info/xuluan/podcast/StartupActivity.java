package info.xuluan.podcast;

import info.xuluan.podcastj.R;
import info.xuluan.podcast.utils.Log;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class StartupActivity extends HapiActivity {
	
	private final Log log = Log.getLog(getClass());
	long delayMillis = 500;
    Handler handler = new Handler();
    Runnable r;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log.debug("In StartupActivity");
		setContentView(R.layout.startup);

	    r = new Runnable() { 
	         public void run() { 
	              delayedStart(); 
	         } 
	    };
	    handler.postDelayed(r, delayMillis);

	    Button iconButton = (Button)findViewById(R.id.iconButton);
		iconButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {            
                delayedStart();
            }
        });

	    Button debugButton = (Button)findViewById(R.id.debugButton);
		debugButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {            
                HomeActivity.toggleShowDebugMenu();
            }
        });

	}
	
	private void delayedStart() {
		handler.removeCallbacks(r);
		tapHome();
	}
}
