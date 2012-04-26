package info.xuluan.podcast.provider;

import info.xuluan.podcast.utils.Log;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class PodcastProvider extends ContentProvider {

	public static final String AUTHORITY = PodcastProvider.class.getName()
			.toLowerCase();

	private static final int TYPE_ALL_SUBSCRIPTIONS = 0;
	private static final int TYPE_SINGLE_SUBSCRIPTION = 1;
	private static final int TYPE_ALL_ITEMS = 2;
	private static final int TYPE_SINGLE_ITEM = 3;

	private final Log log = Log.getLog(getClass());

	private PodcastOpenHelper mHelper = null;

	private static HashMap<String, String> sItemProjectionMap = null;
	private static HashMap<String, String> sSubProjectionMap = null;

	private static final UriMatcher matcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		matcher.addURI(AUTHORITY, "subscriptions", TYPE_ALL_SUBSCRIPTIONS);
		matcher.addURI(AUTHORITY, "subscriptions/#", TYPE_SINGLE_SUBSCRIPTION);
		matcher.addURI(AUTHORITY, "items", TYPE_ALL_ITEMS);
		matcher.addURI(AUTHORITY, "items/#", TYPE_SINGLE_ITEM);
	}

	@Override
	public boolean onCreate() {
		mHelper = new PodcastOpenHelper(getContext());
		if (sItemProjectionMap == null) {
			sItemProjectionMap = new HashMap<String, String>();
			for (int i = 0; i < ItemColumns.ALL_COLUMNS.length; i++) {
				sItemProjectionMap.put(ItemColumns.ALL_COLUMNS[i],
						ItemColumns.ALL_COLUMNS[i]);
			}

		}
		if (sSubProjectionMap == null) {
			sSubProjectionMap = new HashMap<String, String>();
			for (int i = 0; i < SubscriptionColumns.ALL_COLUMNS.length; i++) {
				sSubProjectionMap.put(SubscriptionColumns.ALL_COLUMNS[i],
						SubscriptionColumns.ALL_COLUMNS[i]);
			}
		}
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sort) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String orderBy = null;

		switch (matcher.match(uri)) {
		case TYPE_ALL_SUBSCRIPTIONS:
			qb.setTables(SubscriptionColumns.TABLE_NAME);
			qb.setProjectionMap(sSubProjectionMap);

			if (TextUtils.isEmpty(sort)) {
				orderBy = SubscriptionColumns.DEFAULT_SORT_ORDER;
			} else {
				orderBy = sort;
			}
			// c = db.query(SubscriptionColumns.TABLE_NAME, projection,
			// selection, selectionArgs, null, null, orderBy);
			break;
		case TYPE_SINGLE_SUBSCRIPTION:
			qb.setTables(SubscriptionColumns.TABLE_NAME);
			qb.setProjectionMap(sSubProjectionMap);
			qb.appendWhere(SubscriptionColumns._ID + "="
					+ uri.getPathSegments().get(1));

			// String s_id = uri.getPathSegments().get(1);
			// c = db.query(SubscriptionColumns.TABLE_NAME, projection,
			// SubscriptionColumns._ID + "=" + s_id, null, null, null, null);
			break;
		case TYPE_ALL_ITEMS:
			qb.setTables(ItemColumns.TABLE_NAME);
			qb.setProjectionMap(sItemProjectionMap);

			if (TextUtils.isEmpty(sort)) {
				orderBy = ItemColumns.DEFAULT_SORT_ORDER;
			} else {
				orderBy = sort;
			}
			// c = db.query(ItemColumns.TABLE_NAME, projection, selection,
			// selectionArgs, null, null, orderBy);
			break;
		case TYPE_SINGLE_ITEM:
			qb.setTables(ItemColumns.TABLE_NAME);
			qb.setProjectionMap(sItemProjectionMap);
			qb
					.appendWhere(ItemColumns._ID + "="
							+ uri.getPathSegments().get(1));
			// String i_id = uri.getPathSegments().get(1);
			// c = db.query(ItemColumns.TABLE_NAME, projection, ItemColumns._ID
			// + "=" + i_id, null, null, null, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		Uri newUri = null;
		ContentValues values = null;
		long id = 0L;
		switch (matcher.match(uri)) {
		case TYPE_ALL_SUBSCRIPTIONS:
			try {
				values = SubscriptionColumns.checkValues(initialValues, uri);

				id = db.insertOrThrow(SubscriptionColumns.TABLE_NAME,
						SubscriptionColumns.URL, values);

				if (id > 0) {
					newUri = ContentUris.withAppendedId(
							SubscriptionColumns.URI, id);
					getContext().getContentResolver()
							.notifyChange(newUri, null);
					return newUri;
				}
			} catch (Exception e) {
				log.warn("Failed to insert sub into", e);
				return null;
			}

		case TYPE_ALL_ITEMS:

			try {
				values = ItemColumns.checkValues(initialValues, uri);
				id = db.insertOrThrow(ItemColumns.TABLE_NAME,
						ItemColumns.RESOURCE, values);
				if (id > 0) {
					newUri = ContentUris.withAppendedId(ItemColumns.URI, id);
					getContext().getContentResolver()
							.notifyChange(newUri, null);
					return newUri;
				}
			} catch (Exception e) {
				log.warn("Failed to insert item into", e);
				return null;
			}
		}
		throw new IllegalArgumentException("Illegal Uri: " + uri.toString());
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		int count = 0;
		switch (matcher.match(uri)) {
		case TYPE_ALL_SUBSCRIPTIONS:
			count = db.delete(SubscriptionColumns.TABLE_NAME, where, whereArgs);
			break;
		case TYPE_SINGLE_SUBSCRIPTION:
			String s_id = uri.getPathSegments().get(1);
			count = db.delete(SubscriptionColumns.TABLE_NAME,
					SubscriptionColumns._ID
							+ "="
							+ s_id
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			break;
		case TYPE_ALL_ITEMS:
			count = db.delete(ItemColumns.TABLE_NAME, where, whereArgs);
			break;
		case TYPE_SINGLE_ITEM:
			String i_id = uri.getPathSegments().get(1);
			count = db.delete(ItemColumns.TABLE_NAME,
					ItemColumns._ID
							+ "="
							+ i_id
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		int count;
		switch (matcher.match(uri)) {
		case TYPE_ALL_SUBSCRIPTIONS:
			count = db.update(SubscriptionColumns.TABLE_NAME, values, where,
					whereArgs);
			break;

		case TYPE_ALL_ITEMS:
			count = db.update(ItemColumns.TABLE_NAME, values, where, whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (matcher.match(uri)) {
		case TYPE_ALL_SUBSCRIPTIONS:
			return "vnd.android.cursor.dir/vnd.xuluan.podcast.subscription";
		case TYPE_SINGLE_SUBSCRIPTION:
			return "vnd.android.cursor.item/vnd.xuluan.podcast.subscription";
		case TYPE_ALL_ITEMS:
			return "vnd.android.cursor.dir/vnd.xuluan.podcast.item";
		case TYPE_SINGLE_ITEM:
			return "vnd.android.cursor.item/vnd.xuluan.podcast.item";
		}
		throw new IllegalArgumentException("Unsupported URI: " + uri);
	}

	@Override
	public void onLowMemory() {
		if (mHelper != null) {
			SQLiteDatabase db = mHelper.getWritableDatabase();
			db.close();
			mHelper.close();

		}
	}

}
