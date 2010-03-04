package info.xuluan.podcast.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class Subscription {
	public long id;
	public String title;

	public String url;
	public String description;
	public long lastUpdated;
	public long lastItemUpdated;
	public long fail_count;
	public long auto_download;

	public static Subscription getByUrl(ContentResolver context, String url) {
		Cursor cursor = null;
		try {
			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, SubscriptionColumns.URL
							+ "=?", new String[] { url }, null);
			if (cursor.moveToFirst()) {
				Subscription sub = new Subscription();
				fetchFromCursor(sub, cursor);
				cursor.close();
				return sub;
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return null;

	}

	public static Subscription getByCursor(Cursor cursor) {
		if (cursor.moveToFirst() == false)
			return null;
		Subscription sub = new Subscription();
		fetchFromCursor(sub, cursor);
		return sub;
	}

	public static Subscription getSubbyId(ContentResolver context, long id) {
		Cursor cursor = null;
		Subscription sub = null;

		try {
			String where = SubscriptionColumns._ID + " = " + id;

			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, null, null);
			if (cursor.moveToFirst()) {
				sub = new Subscription();
				fetchFromCursor(sub, cursor);
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (cursor != null)
				cursor.close();
		}

		return sub;

	}

	public Subscription() {
		id = -1;
		title = null;
		url = null;
		description = null;
		lastUpdated = -1;
		fail_count = -1;
		lastItemUpdated = -1;
	}

	public void delete(ContentResolver context) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, id);
		context.delete(uri, null, null);
	}

	public int update(ContentResolver context) {
		try {

			ContentValues cv = new ContentValues();
			if (title != null)
				cv.put(SubscriptionColumns.TITLE, title);
			if (url != null)
				cv.put(SubscriptionColumns.URL, url);
			if (description != null)
				cv.put(SubscriptionColumns.DESCRIPTION, description);

			lastUpdated = Long.valueOf(System.currentTimeMillis());
			cv.put(SubscriptionColumns.LAST_UPDATED, lastUpdated);

			if (fail_count >= 0)
				cv.put(SubscriptionColumns.FAIL_COUNT, fail_count);

			if (lastItemUpdated >= 0)
				cv.put(SubscriptionColumns.LAST_ITEM_UPDATED, lastItemUpdated);

			if (auto_download >= 0)
				cv.put(SubscriptionColumns.AUTO_DOWNLOAD, auto_download);
			
			return context.update(SubscriptionColumns.URI, cv,
					SubscriptionColumns._ID + "=" + id, null);

		} finally {
		}
	}

	private static void fetchFromCursor(Subscription sub, Cursor cursor) {
		assert cursor.moveToFirst();
		cursor.moveToFirst();
		sub.id = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns._ID));
		sub.lastUpdated = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
		sub.title = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.TITLE));
		sub.fail_count = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.FAIL_COUNT));
		sub.lastItemUpdated = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
		sub.auto_download = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.AUTO_DOWNLOAD));
	}

}
