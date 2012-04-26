package info.xuluan.podcast;

import info.xuluan.podcast.utils.Log;
import android.os.Bundle;

public class StartupActivity extends HapiActivity {
	
	private final Log log = Log.getLog(getClass());
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		log.debug("In StartupActivity");
		tapHome();
	}
}
