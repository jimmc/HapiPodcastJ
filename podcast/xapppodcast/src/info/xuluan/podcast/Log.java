package info.xuluan.podcast;

public class Log {

    private final String clazz;
    private static final String TAG = "RSS";

    public Log(Class<?> clazz) {
        this.clazz = "[" + clazz.getSimpleName() + "] ";
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

    public void debug(String message, Throwable t) {
        if (message!=null)
            android.util.Log.d(TAG, clazz + message);
        if (t!=null)
            android.util.Log.d(TAG, clazz + t.toString());
    }

    public void info(String message, Throwable t) {
        if (message!=null)
            android.util.Log.i(TAG, clazz + message);
        if (t!=null)
            android.util.Log.i(TAG, clazz + t.toString());
    }

    public void warn(String message, Throwable t) {
        if (message!=null)
            android.util.Log.w(TAG, clazz + message);
        if (t!=null)
            android.util.Log.w(TAG, clazz + t.toString());
    }

    public void error(String message, Throwable t) {
        if (message!=null)
            android.util.Log.e(TAG, clazz + message);
        if (t!=null)
            android.util.Log.e(TAG, clazz + t.toString());
    }
}
