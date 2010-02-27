package info.xuluan.podcast.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import info.xuluan.podcast.fetcher.DownloadItemListener;
import info.xuluan.podcast.fetcher.FeedFetcher;
import info.xuluan.podcast.fetcher.Response;
import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.FeedParserHandler;
import info.xuluan.podcast.parser.FeedParserListenerAdapter;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.Log;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;

public class PodcastService extends Service {

	public static final int NO_CONNECT = 1;
	public static final int WIFI_CONNECT = 2;
	public static final int MOBILE_CONNECT = 4;

	public static final int MAX_DOWNLOAD_FAIL = 5;

	private static final int MSG_TIMER = 0;

	private static final long ONE_MINUTE = 60L * 1000L;
	private static final long ONE_HOUR = 60L * ONE_MINUTE;
	private static final long ONE_DAY = 24L * ONE_HOUR;

	private static final long timer_freq = 3 * ONE_MINUTE;

	private long pref_update = 2 * 60 * ONE_MINUTE;

	public int pref_connection_sel = WIFI_CONNECT;

	public long pref_update_wifi = 0;
	public long pref_update_mobile = 0;
	public long pref_item_expire = 0;
	public long pref_download_file_expire = 0;
	public long pref_played_file_expire = 0;

	private DownloadItemListener mDownloadListener = null;

	private static boolean mDownloading = false;
	private FeedItem mDownloadingItem = null;
	private static final ReentrantReadWriteLock mDownloadLock = new ReentrantReadWriteLock();

	private static boolean mUpdate = false;
	private static final ReentrantReadWriteLock mUpdateLock = new ReentrantReadWriteLock();
	private static int mConnectStatus = NO_CONNECT;

	public static final String BASE_DOWNLOAD_DIRECTORY = "/sdcard/xuluan.podcast/download";

	public static final String UPDATE_DOWNLOAD_STATUS = PodcastService.class
			.getName()
			+ ".UPDATE_DOWNLOAD_STATUS";

	private final Log log = Log.getLog(getClass());

	class DisplayListener implements DownloadItemListener {

		private Intent set_intent(FeedItem item) {
			Intent intent = new Intent(UPDATE_DOWNLOAD_STATUS);
			intent.putExtra(ItemColumns.TITLE, item.title);
			intent.putExtra(ItemColumns.LENGTH, item.length);
			intent.putExtra(ItemColumns.OFFSET, item.offset);
			intent.putExtra(ItemColumns.DURATION, item.duration);
			return intent;
		}

		public void onBegin(FeedItem item) {

			Intent intent = set_intent(item);
			mDownloadingItem = item;

			sendBroadcast(intent);
		}

		public void onUpdate(FeedItem item) {

			Intent intent = set_intent(item);
			mDownloadingItem = item;

			sendBroadcast(intent);
		}

		public void onFinish() {
			FeedItem item = new FeedItem();
			item.title = "";
			item.offset = 0;
			item.length = -1;
			item.duration = "01:00";

			Intent intent = set_intent(item);

			sendBroadcast(intent);

			mDownloadingItem = item;

		}
	}

	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_TIMER:
				log.debug("Message: MSG_TIMER.");

				start_update();
				removeExpires();
				start_download();

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

	private String findSubscriptionUrlByFreq() {
		Cursor cursor = null;
		try {
			Long now = Long.valueOf(System.currentTimeMillis());
			log.debug("pref_update = " + pref_update);

			String where = SubscriptionColumns.LAST_UPDATED + "<"
					+ (now - pref_update);

			cursor = getContentResolver().query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {

				String url = cursor.getString(cursor
						.getColumnIndex(SubscriptionColumns.URL));
				cursor.close();
				return url;

			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;

	}

	private FeedItem getDownloadItem() {
		Cursor cursor = null;
		try {
			String where = ItemColumns.STATUS + ">"
					+ ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE + " AND "
					+ ItemColumns.STATUS + "<"
					+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW;

			cursor = getContentResolver().query(
					ItemColumns.URI,
					ItemColumns.ALL_COLUMNS,
					where,
					null,
					ItemColumns.STATUS + " DESC , " + ItemColumns.LAST_UPDATE
							+ " ASC");
			if (cursor.moveToFirst()) {
				FeedItem item = FeedItem.getByCursor(cursor);
				cursor.close();
				return item;
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}

		return null;

	}

	public FeedItem getDownloadingItem() {
		return mDownloadingItem;
	}

	public void start_update() {
		if (updateConnectStatus() == NO_CONNECT)
			return;

		log.debug("start_update()");
		mUpdateLock.readLock().lock();
		if (mUpdate) {
			log.debug("mUpdate = true ");
			mUpdateLock.readLock().unlock();
			return;

		}
		mUpdateLock.readLock().unlock();

		mUpdateLock.writeLock().lock();
		mUpdate = true;
		mUpdateLock.writeLock().unlock();

		new Thread() {
			public void run() {
				try {

					String url = findSubscriptionUrlByFreq();
					while (url != null) {
						FeedParserListenerAdapter listener = fetchFeed(url);
						if (listener != null)
							updateFeed(url, listener);
						else
							updateFetch(url);

						url = findSubscriptionUrlByFreq();
					}

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					mUpdateLock.writeLock().lock();
					log.debug("mUpdate = false ");

					mUpdate = false;
					mUpdateLock.writeLock().unlock();
				}

			}
		}.start();
	}

	public void start_download() {
		if (updateConnectStatus() == NO_CONNECT)
			return;

		mDownloadLock.readLock().lock();
		if (mDownloading) {
			mDownloadLock.readLock().unlock();
			return;

		}
		mDownloadLock.readLock().unlock();

		mDownloadLock.writeLock().lock();
		mDownloading = true;
		mDownloadLock.writeLock().unlock();

		new Thread() {
			public void run() {
				try {
					while ((updateConnectStatus() & pref_connection_sel) > 0) {

						FeedItem item = getDownloadItem();
						if (item == null) {
							break;
						}
						File file = new File(BASE_DOWNLOAD_DIRECTORY);
						if (!file.exists()) {
							break;
						}

						// log.debug("start_download start");
						if (item.pathname.equals("")) {
							String path_name = BASE_DOWNLOAD_DIRECTORY
									+ "/podcast_" + item.id + ".mp3";
							item.pathname = path_name;
						}

						// if(MAX_DOWNLOAD_FAIL<item.failcount){

						mDownloadListener.onBegin(item);
						try {
							item.status = ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
							item.update(getContentResolver());
							FeedFetcher fetcher = new FeedFetcher();

							fetcher.download(item, mDownloadListener);

						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							if (item.status != ItemColumns.ITEM_STATUS_NO_PLAY) {
								item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
							}
							mDownloadListener.onFinish();

						}
						// }
						log.debug(item.title + "  " + item.length + "  "
								+ item.offset);

						if (item.status == ItemColumns.ITEM_STATUS_NO_PLAY) {

							ContentValues values = new ContentValues(3);

							values
									.put(MediaStore.Audio.Media.TITLE,
											item.title);
							values.put(MediaStore.Audio.Media.MIME_TYPE, item
									.getType());
							values.put(MediaStore.Audio.Media.DATA,
									item.pathname);

							Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
							Uri newUri = getContentResolver().insert(base,
									values);
							if (newUri != null)
								item.uri = newUri.toString();

							item.created = Long.valueOf(System
									.currentTimeMillis());

						} else {
							item.failcount++;
							if (item.failcount > MAX_DOWNLOAD_FAIL) {
								item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
								item.failcount = 0;
							}
						}

						item.update(getContentResolver());

					}

					// log.debug("start_download end");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					mDownloadLock.writeLock().lock();
					mDownloading = false;
					mDownloadLock.writeLock().unlock();
				}

			}

		}.start();

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

		expiredTime = System.currentTimeMillis() - pref_played_file_expire;
		try {
			String where = ItemColumns.LAST_UPDATE + "<" + expiredTime
					+ " and " + ItemColumns.STATUS + "="
					+ ItemColumns.ITEM_STATUS_PLAYED;

			Cursor cursor = getContentResolver().query(ItemColumns.URI,
					ItemColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				while (cursor.moveToNext()) {
					String path = cursor.getString(cursor
							.getColumnIndex(ItemColumns.PATHNAME));
					try {
						File file = new File(path);
						boolean deleted = file.delete();

					} catch (Exception e) {
						log.warn("del file failed : " + path + "  " + e);

					}
				}

			}
			cursor.close();
			getContentResolver().delete(ItemColumns.URI, where, null);

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
			if (cursor.moveToFirst()) {
				while (cursor.moveToNext()) {
					String path = cursor.getString(cursor
							.getColumnIndex(ItemColumns.PATHNAME));
					try {
						File file = new File(path);
						boolean deleted = file.delete();

					} catch (Exception e) {
						log.warn("del file failed : " + path + "  " + e);

					}
				}

			}
			cursor.close();
			getContentResolver().delete(ItemColumns.URI, where, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FeedParserListenerAdapter fetchFeed(String url) {
		log.debug("fetchFeed start");

		FeedFetcher fetcher = new FeedFetcher();
		FeedParserListenerAdapter listener = new FeedParserListenerAdapter();
		FeedParserHandler handler = new FeedParserHandler(listener);

		try {
			Response response = fetcher.fetch(url);

			log.debug("fetcher.fetch end");
			if (response != null)
				FeedParser.getDefault().parse(
						new ByteArrayInputStream(response.content), handler);
			else
				log.debug("response == null");

		} catch (Exception e) {
			log.debug("Parse XML error:", e);
			// e.printStackTrace();

		}

		log
				.debug("fetchFeed getFeedItemsSize = "
						+ listener.getFeedItemsSize());

		if (listener.getFeedItemsSize() > 0) {
			return listener;
		}

		return null;
	}

	private void updateFetch(String url) {
		Cursor cursor = null;
		log.debug("updateFetch start");
		try {
			cursor = getContentResolver().query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, null, null,
					SubscriptionColumns.LAST_UPDATED + " asc");
			if (cursor.moveToFirst()) {
				Subscription sub = Subscription.getByCursor(cursor);
				sub.fail_count++;
				sub.update(getContentResolver());
				log.debug("updateFetch OK");
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
	}

	public void updateFeed(String url, FeedParserListenerAdapter listener) {
		// sort feed items:
		String feedTitle = listener.getFeedTitle();
		String feedDescription = listener.getFeedDescription();
		FeedItem[] feedItems = listener.getFeedItems();
		log.debug("updateFeed start");
		/*
		 * for (FeedItem item : feedItems) {
		 * 
		 * log.warn("item_date: " + item.date); }
		 */
		Arrays.sort(feedItems, new Comparator<FeedItem>() {
			public int compare(FeedItem i1, FeedItem i2) {
				long d1 = i1.getDate();
				long d2 = i2.getDate();

				if (d1 == d2)
					return i1.title.compareTo(i2.title);
				return d1 > d2 ? (-1) : 1;
			}
		});
		/*
		 * for (FeedItem item : feedItems) {
		 * 
		 * log.warn("item_date: " + item.date); }
		 */
		Subscription subscription = Subscription.getByUrl(getContentResolver(),
				url);

		if (subscription == null)
			return;

		log.debug("feedItems length: " + feedItems.length);

		List<FeedItem> added = new ArrayList<FeedItem>(feedItems.length);
		for (FeedItem item : feedItems) {
			long d = item.getDate();
			log.debug("item_date: " + item.date);

			if (d <= subscription.lastUpdated) {
				log.debug("item lastUpdated =" + d + " feed lastUpdated = "
						+ subscription.lastUpdated);
				log.debug("item date =" + item.date);
				break;
			}
			log.debug("subscription.id : " + subscription.id);

			log.debug("add item = " + item.title);
			added.add(item);

		}
		log.debug("added size: " + added.size());
		subscription.fail_count = 0;
		subscription.update(getContentResolver());
		subscription.title = feedTitle;
		subscription.description = feedDescription;

		if (!added.isEmpty()) {
			subscription.lastItemUpdated = added.get(0).getDate();
			log.debug("MAX item date:==" + added.get(0).date);
		}

		int n = subscription.update(getContentResolver());
		if (n == 1) {
			log.debug("Feed updated: " + url);
		}
		if (added.isEmpty())
			return;
		addItems(subscription.id, added);

	}

	void addItems(Long sub_id, List<FeedItem> items) {
		ContentResolver cr = getContentResolver();
		int len = items.size();
		for (int i = len - 1; i >= 0; i--) {

			FeedItem item = items.get(i);
			item.sub_id = sub_id;
			String where = ItemColumns.SUBS_ID + "=" + sub_id + " and "
					+ ItemColumns.RESOURCE + "= '" + item.resource + "'";

			Cursor cursor = cr.query(ItemColumns.URI,
					new String[] { ItemColumns._ID }, where, null, null);

			if (cursor.moveToFirst()) {
				cursor.close();
				log.debug("dup resource: " + where);

				continue;
			} else {
				if(cursor!=null)
					cursor.close();
				Uri uri = item.insert(cr);
				if (uri != null)
					log.debug("Inserted new item: " + uri.toString());
			}

		}
	}

	public Uri addSubscription(String url) {
		Subscription sub = Subscription.getByUrl(getContentResolver(), url);

		if (sub != null) {
			return null;
		}
		ContentValues cv = new ContentValues();
		cv.put(SubscriptionColumns.TITLE, url);
		cv.put(SubscriptionColumns.URL, url);
		cv.put(SubscriptionColumns.LAST_UPDATED, 0L);
		return getContentResolver().insert(SubscriptionColumns.URI, cv);

	}

	public void removeSubscription(String sub_id) {
		ContentResolver cr = getContentResolver();
		cr.delete(SubscriptionColumns.URI, SubscriptionColumns._ID + "="
				+ sub_id, null);
		cr.delete(ItemColumns.URI, ItemColumns.SUBS_ID + "=" + sub_id, null);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log.debug("onCreate()");
		updateSetting();
		log.debug("pref_update_mobile " + pref_update_mobile);

		File file = new File(BASE_DOWNLOAD_DIRECTORY);
		boolean exists = (file.exists());
		if (exists) {
			if (!file.isDirectory())
				log.error("cannot change file to directory:"
						+ BASE_DOWNLOAD_DIRECTORY);

		} else {
			if (!file.mkdirs())
				log.error("cannot create the directory:"
						+ BASE_DOWNLOAD_DIRECTORY);

		}

		triggerNextTimer(1);

		mDownloadListener = new DisplayListener();

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

	private final IBinder binder = new ReadingBinder();

	public class ReadingBinder extends Binder {
		public PodcastService getService() {
			return PodcastService.this;
		}
	}

	public void updateSetting() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);

		boolean b = pref.getBoolean("pref_download_only_wifi", true);
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

	}

}
