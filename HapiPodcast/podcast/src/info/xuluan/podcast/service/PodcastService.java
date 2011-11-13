package info.xuluan.podcast.service;

import info.xuluan.podcast.R;
import info.xuluan.podcast.fetcher.FeedFetcher;
import info.xuluan.podcast.parser.FeedHandler;


import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.LockHandler;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.SDCardMgr;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

public class PodcastService extends Service {

	public static final int NO_CONNECT = 1;
	public static final int WIFI_CONNECT = 2;
	public static final int MOBILE_CONNECT = 4;

	private static final int MSG_TIMER = 0;

	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;

	private static final long timer_freq = 3 * ONE_MINUTE;

	private long pref_update = 2 * 60 * ONE_MINUTE;

	public int pref_connection_sel = MOBILE_CONNECT|WIFI_CONNECT;

	public long pref_update_wifi = 0;
	public long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 0;
	public long pref_played_file_expire = 0;
	public int pref_max_valid_size = 0;
	

	private FeedItem mDownloadingItem = null;
	private static final LockHandler mDownloadLock = new LockHandler();

	private static final LockHandler mUpdateLock = new LockHandler();
	private static int mConnectStatus = NO_CONNECT;
	

	public static final String UPDATE_DOWNLOAD_STATUS = PodcastService.class
			.getName()
			+ ".UPDATE_DOWNLOAD_STATUS";

	private final Log log = Log.getLog(getClass());


	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMER:
				log.debug("Message: MSG_TIMER.");

				start_update();
				removeExpires();
				do_download(false);

				triggerNextTimer(timer_freq);

				break;
			}
		}
	};

	void triggerNextTimer(long delay) {
		Message msg = Message.obtain();
		msg.what = MSG_TIMER;
		handler.sendMessageDelayed(msg, delay);
	}
	
	private int updateConnectStatus() {
		log.debug("updateConnectStatus");
		try {

			ConnectivityManager cm = (ConnectivityManager) this
					.getSystemService(CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			if (info == null) {
				mConnectStatus = NO_CONNECT;
				return mConnectStatus;
			}

			if (info.isConnected() && (info.getType() == 1)) {
				mConnectStatus = WIFI_CONNECT;
				pref_update = pref_update_wifi;
				return mConnectStatus;
			} else {
				mConnectStatus = MOBILE_CONNECT;
				pref_update = pref_update_mobile;

				return mConnectStatus;
			}
		} catch (Exception e) {
			e.printStackTrace();
			mConnectStatus = NO_CONNECT;

			return mConnectStatus;
		}

	}

	private Subscription findSubscription() {

			Long now = Long.valueOf(System.currentTimeMillis());
			log.debug("pref_update = " + pref_update);

			String where = SubscriptionColumns.LAST_UPDATED + "<"
					+ (now - pref_update);
			String order = SubscriptionColumns.LAST_UPDATED + " ASC,"
			+ SubscriptionColumns.FAIL_COUNT +" ASC";
			Subscription sub = Subscription.getBySQL(getContentResolver(),where,order);

			return sub;

	}

	private FeedItem getDownloadItem() {

		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE + " AND "
				+ ItemColumns.STATUS + "<"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW;
		
		String order =ItemColumns.STATUS + " DESC , " + ItemColumns.LAST_UPDATE
		+ " ASC";
		return FeedItem.getBySQL(getContentResolver(),where,order);
	}

	
	public FeedItem getDownloadingItem() {
		return mDownloadingItem;
	}
	
	public void start_update() {
		if (updateConnectStatus() == NO_CONNECT)
			return;

		log.debug("start_update()");
		if(mUpdateLock.locked()==false)
			return;


		new Thread() {
			public void run() {
				try {
					int add_num;
					Subscription sub = findSubscription();
					while (sub != null) {
					if (updateConnectStatus() == NO_CONNECT)
							break;
						FeedHandler handler = new FeedHandler(getContentResolver(),pref_max_valid_size);
						add_num = handler.update(sub);
						if((add_num>0)&&(sub.auto_download>0))
							do_download(false);

						sub = findSubscription();
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					mUpdateLock.release();
				}

			}
		}.start();
	}

	public void start_download() {

		 do_download(true);
		
	}
	
	private void do_download(boolean show){
		if (SDCardMgr.getSDCardStatusAndCreate()==false){
			
			if(show)
				Toast.makeText(this, getResources().getString(R.string.sdcard_unmout), Toast.LENGTH_LONG).show();
			return;
		}

		
		if (updateConnectStatus() == NO_CONNECT){
			if(show)
				Toast.makeText(this, getResources().getString(R.string.no_connect), Toast.LENGTH_LONG).show();
			return;
		}
		
		if(mDownloadLock.locked()==false)
			return;


		new Thread() {
			public void run() {
				try {
					while ((updateConnectStatus() & pref_connection_sel) > 0) {

						mDownloadingItem = getDownloadItem();

						if (mDownloadingItem == null) {
							break;
						}



						try {
							mDownloadingItem.startDownload(getContentResolver());
							FeedFetcher fetcher = new FeedFetcher();

							fetcher.download(mDownloadingItem);

						} catch (Exception e) {
							e.printStackTrace();
						}
						
						log.debug(mDownloadingItem.title + "  " + mDownloadingItem.length + "  "
								+ mDownloadingItem.offset);


						mDownloadingItem.endDownload(getContentResolver());

					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					mDownloadingItem = null;
					mDownloadLock.release();
				}

			}

		}.start();
	}


	private void deleteExpireFile(Cursor cursor) {
		
		if(cursor==null)
			return;
		
		if (cursor.moveToFirst()) {
			do{
				FeedItem item = FeedItem.getByCursor(cursor);
				if(item!=null){
					item.delFile(getContentResolver());
				}
			}while (cursor.moveToNext());
		}
		cursor.close();
		
	}

	private void removeExpires() {
		long expiredTime = System.currentTimeMillis() - pref_item_expire;
		try {
			String where = ItemColumns.CREATED + "<" + expiredTime + " and "
					+ ItemColumns.STATUS + "<"
					+ ItemColumns.ITEM_STATUS_MAX_READING_VIEW;

			getContentResolver().delete(ItemColumns.URI, where, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (SDCardMgr.getSDCardStatus()==false){
			return;
		}

		expiredTime = System.currentTimeMillis() - pref_played_file_expire;
		try {
			String where = ItemColumns.LAST_UPDATE + "<" + expiredTime
					+ " and " + ItemColumns.STATUS + "="
					+ ItemColumns.ITEM_STATUS_PLAYED;

			Cursor cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}

		expiredTime = System.currentTimeMillis() - pref_download_file_expire;
		try {
			String where = ItemColumns.LAST_UPDATE + "<" + expiredTime
					+ " and " + ItemColumns.STATUS + "="
					+ ItemColumns.ITEM_STATUS_NO_PLAY;

			Cursor cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			String where = ItemColumns.STATUS + "="
					+ ItemColumns.ITEM_STATUS_DELETE;

			Cursor cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			deleteExpireFile(cursor);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String where = ItemColumns.STATUS + "="
		+ ItemColumns.ITEM_STATUS_DELETED;		
		getContentResolver().delete(ItemColumns.URI, where, null);

	}

	@Override
	public void onCreate() {
		super.onCreate();
		updateSetting();
		SDCardMgr.getSDCardStatusAndCreate();
		triggerNextTimer(1);

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		log.debug("onStart()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

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

	private final IBinder binder = new PodcastBinder();

	public class PodcastBinder extends Binder {
		public PodcastService getService() {
			return PodcastService.this;
		}
	}

	public void updateSetting() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);

		boolean b = pref.getBoolean("pref_download_only_wifi", false);
		pref_connection_sel = b ? WIFI_CONNECT
				: (WIFI_CONNECT | MOBILE_CONNECT);

		pref_update_wifi = Integer.parseInt(pref.getString("pref_update_wifi",
				"60"));
		pref_update_wifi *= ONE_MINUTE;

		pref_update_mobile = Integer.parseInt(pref.getString(
				"pref_update_mobile", "120"));
		pref_update_mobile *= ONE_MINUTE;

		pref_item_expire = Integer.parseInt(pref.getString("pref_item_expire",
				"7"));
		pref_item_expire *= ONE_DAY;
		pref_download_file_expire = Integer.parseInt(pref.getString(
				"pref_download_file_expire", "7"));
		pref_download_file_expire *= ONE_DAY;
		pref_played_file_expire = Integer.parseInt(pref.getString(
				"pref_played_file_expire", "24"));
		pref_played_file_expire *= ONE_HOUR;
		
		pref_max_valid_size= Integer.parseInt(pref.getString(
				"pref_max_new_items", "10"));
	}

}
