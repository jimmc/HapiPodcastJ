package info.xuluan.podcast.utils;

public class Log {

	public final static int  VERBOSE = 0;
	
	public final static int  DEBUG = 1;
	public final static int  INFO = 2;
	public final static int  WARN = 3;
	public final static int  ERROR = 4;
	
	public final static int  DEFAULT_LEVEL = 3;
	private static int initialLevel = DEFAULT_LEVEL;
	
	private final String clazz;
	
	private int  level;
	private static final String TAG = "PODCAST";
	
	public static Log getDebugLog(Class<?> clazz, int l) {
		Log log = new Log(clazz);
		log.level = l;
		return log;
	}	

	public static Log getLog(Class<?> clazz) {
		return new Log(clazz);
	}
	
	public static void setInitialLevel(int level) {
		initialLevel = level;
	}
	public static int initialLevel() {
		return initialLevel;
	}

	public Log(Class<?> clazz) {
		this.clazz = "[" + clazz.getSimpleName() + "] ";
		level = initialLevel;
	}
	
	public void verbose(String message) {
		verbose(message, null);
	}
	
	public void debug(String message) {
		debug(message, null);
	}

	public void info(String message) {
		info(message, null);
	}

	public void warn(String message) {
		warn(message, null);
	}

	public void error(String message) {
		error(message, null);
	}

	public void verbose(String message, Throwable t) {
		if(VERBOSE<level)
			return;
		if (message != null)
			android.util.Log.v(TAG, clazz + message);
		if (t != null)
			android.util.Log.v(TAG, clazz + t.toString());		
	}
	
	public void debug(String message, Throwable t) {
		if(DEBUG<level)
			return;		
		if (message != null)
			android.util.Log.d(TAG, clazz + message);
		if (t != null)
			android.util.Log.d(TAG, clazz + t.toString());		
	}

	public void info(String message, Throwable t) {
		if(INFO<level)
			return;			
		if (message != null)
			android.util.Log.i(TAG, clazz + message);
		if (t != null)
			android.util.Log.i(TAG, clazz + t.toString());
	}

	public void warn(String message, Throwable t) {
		if(WARN<level)
			return;			
		if (message != null)
			android.util.Log.w(TAG, clazz + message);
		if (t != null)
			android.util.Log.w(TAG, clazz + t.toString());
	}

	public void error(String message, Throwable t) {
		if(ERROR<level)
			return;			
		if (message != null)
			android.util.Log.e(TAG, clazz + message);
		if (t != null)
			android.util.Log.e(TAG, clazz + t.toString());
	}
}
