package info.xuluan.podcast.provider;

import info.xuluan.podcast.utils.FileUtils;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.ZipExporter;
import info.xuluan.podcast.utils.ZipImporter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class Subscription {
	
	public final static int ADD_SUCCESS = 0;
	public final static int ADD_FAIL_DUP = -1;
	public final static int ADD_FAIL_UNSUCCESS = -2;

	private final Log log = Log.getLog(getClass());
	
	public long id;
	public String title;
	public String link;
	public String comment;

	public String url;
	public String description;
	public long lastUpdated;
	public long lastItemUpdated;
	public long failCount;
	public long autoDownload;
	public long suspended;

	public static void view(Activity act, long channel_id) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, channel_id);
		//Subscription channel = Subscription.getById(act.getContentResolver(), channel_id);
		act.startActivity(new Intent(Intent.ACTION_VIEW, uri));
	}

	public static void viewEpisodes(Activity act, long channel_id) {
		Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, channel_id);
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));
	}

	public static Subscription getBySQL(ContentResolver context,
			String where, String[] args, String order) {
		
		Subscription sub = null;
		Cursor cursor = null;
	
		try {		
			cursor = context.query(SubscriptionColumns.URI,
					SubscriptionColumns.ALL_COLUMNS, where, args, order);
			if (cursor.moveToFirst()) {
				sub =Subscription.getByCursor(cursor);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}		
		return sub;			
	}
	
	public static Subscription getBySQL(ContentResolver context, String where, String order) {
		return getBySQL(context, where, null, order);
	}
	
	public static Subscription getByUrl(ContentResolver context, String url) {
		String where = SubscriptionColumns.URL + "=?";
		String[] args = new String[] { url };
		return getBySQL(context, where, args, null);
	}

	public static Subscription getById(ContentResolver context, long id) {
		String where = SubscriptionColumns._ID + " = " + id;
		return getBySQL(context, where, null);
	}
	
	public static Subscription getByCursor(Cursor cursor) {
		//if (cursor.moveToFirst() == false)
		//	return null;
		Subscription sub = new Subscription();
		fetchFromCursor(sub, cursor);
		return sub;
	}

	private void init() {
		id = -1;
		title = null;
		url = null;
		link = null;
		comment = "";
		description = null;
		lastUpdated = -1;
		failCount = -1;
		lastItemUpdated = -1;
		autoDownload = -1;
		suspended = -1;
	}
	
	public Subscription() {
		init();
	}
	
	public Subscription(String url_link) {
		
		init();
		url = url_link;
		title = url_link;
		link = url_link;

	}	
	
	public int subscribe(ContentResolver context){
		Subscription sub = Subscription.getByUrl(context, url);
		if (sub != null) {
			return ADD_FAIL_DUP;
		}

		ContentValues cv = new ContentValues();
		cv.put(SubscriptionColumns.TITLE, title);
		cv.put(SubscriptionColumns.URL, url);
		cv.put(SubscriptionColumns.LINK, link);
		cv.put(SubscriptionColumns.LAST_UPDATED, 0L);
		cv.put(SubscriptionColumns.COMMENT, comment);
		cv.put(SubscriptionColumns.DESCRIPTION, description);
		Uri uri = context.insert(SubscriptionColumns.URI, cv);
		if (uri == null) {
			return ADD_FAIL_UNSUCCESS;
		}
		
		return ADD_SUCCESS;	
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

			if(failCount<=0){
				lastUpdated = Long.valueOf(System.currentTimeMillis());
			}else{
				lastUpdated = 0;
			}
				cv.put(SubscriptionColumns.LAST_UPDATED, lastUpdated);
			
			if (failCount >= 0)
				cv.put(SubscriptionColumns.FAIL_COUNT, failCount);

			if (lastItemUpdated >= 0)
				cv.put(SubscriptionColumns.LAST_ITEM_UPDATED, lastItemUpdated);

			if (autoDownload >= 0)
				cv.put(SubscriptionColumns.AUTO_DOWNLOAD, autoDownload);
			
			if (suspended >= 0)
				cv.put(SubscriptionColumns.SUSPENDED, suspended);
			
			return context.update(SubscriptionColumns.URI, cv,
					SubscriptionColumns._ID + "=" + id, null);

		} finally {
		}
	}

	public Map<Integer,Integer> getEpisodeCounts(Context context) {
		PodcastOpenHelper dbOpenHelper = new PodcastOpenHelper(context);
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		String table = ItemColumns.TABLE_NAME;
		String where = ItemColumns.SUBS_ID + " = " + id;
		String groupBy = ItemColumns.STATUS;
		Cursor cursor = null;
		Map<Integer,Integer> countByStatus = new HashMap<Integer,Integer>();
		try {
			String query = "select count (*),"+groupBy+" from "+table+
					" where "+where+" group by "+groupBy;
			cursor = db.rawQuery(query, null);
			while (cursor.moveToNext()) {
				int count = cursor.getInt(0);
				int status = cursor.getInt(1);
				countByStatus.put(status, count);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return countByStatus;
	}
	
	private static void fetchFromCursor(Subscription sub, Cursor cursor) {
		//assert cursor.moveToFirst();
		//cursor.moveToFirst();
		sub.id = cursor.getLong(cursor.getColumnIndex(SubscriptionColumns._ID));
		sub.lastUpdated = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.LAST_UPDATED));
		sub.title = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.TITLE));
		sub.url = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.URL));		
		sub.comment = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.COMMENT));		
		sub.description = cursor.getString(cursor
				.getColumnIndex(SubscriptionColumns.DESCRIPTION));		
		sub.failCount = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.FAIL_COUNT));
		sub.lastItemUpdated = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.LAST_ITEM_UPDATED));
		sub.autoDownload = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.AUTO_DOWNLOAD));
		sub.suspended = cursor.getLong(cursor
				.getColumnIndex(SubscriptionColumns.SUSPENDED));
	}

	public void exportAllToZipFile(Activity act) {
		exportToZipFile(act, true);
	}
	
	public void exportUnplayedToZipFile(Activity act) {
		exportToZipFile(act, false);
	}
		
	public void exportToZipFile(final Activity act, final boolean all) {
		String filename = ZipExporter.getExportZipFileName(this.title + "_" + this.id);
		ZipExporter.ContentWriter cw = new ZipExporter.ContentWriter() {
			public void writeContent(ZipOutputStream zos) throws IOException{
				exportToZipStream(act,zos,all);
			}
		};
		ZipExporter.exportToZipFile(act, filename, cw);
	}
	
	public void exportToZipStream(Activity act, ZipOutputStream zos, boolean all) throws IOException {
		exportMetaToZip(zos);	//export the header first
		exportEpisodesToZip(act, zos, all);
	}
	
	private void exportEpisodesToZip(Activity act, ZipOutputStream zos, boolean all) throws IOException {
		String baseWhere = "subs_id="+id;
		String where;
		if (all)
			where = baseWhere;	//write out all episodes
		else {
			//write out all episodes that we have downloaded but not yet listened to
			String statusWhere = "status>"+ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW+" AND "+
					"status<="+ItemColumns.ITEM_STATUS_PLAY_PAUSE;
			where = "("+baseWhere+") AND ("+statusWhere+")";
		}
		String order = null;	//we don't care yet about order
		Cursor cursor = act.getContentResolver().query(ItemColumns.URI,
				ItemColumns.ALL_COLUMNS, where, null, order);
		while (cursor.moveToNext()) {
			FeedItem episode = FeedItem.getByCursor(cursor);
			episode.exportToZipStream(zos, this);
		}
		cursor.close();
	}
	
	public void exportMetaToZip(ZipOutputStream zos) throws IOException {
		String filename = FileUtils.getExportFileName(this.title, this.id, "xml");
		ZipEntry ze = new ZipEntry(filename);
		zos.putNextEntry(ze);
		PrintWriter pw = new PrintWriter(zos);
		writeXml(pw);
		pw.flush();
		zos.closeEntry();
	}

	private void writeXml(PrintWriter out) {
		int subcriptionLevel = 1;
		out.print("<subscription>\n");
		
		ZipExporter.writeXmlField(out, "id", Long.toString(id), subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.TITLE, title, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.URL, url, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.LINK, link, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.LAST_UPDATED, lastUpdated, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.COMMENT, comment, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.DESCRIPTION, description, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.LAST_ITEM_UPDATED, lastItemUpdated, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.FAIL_COUNT, failCount, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.AUTO_DOWNLOAD, autoDownload, subcriptionLevel);
		ZipExporter.writeXmlField(out, SubscriptionColumns.SUSPENDED, suspended, subcriptionLevel);
		
		out.print("</subscription>");
	}

	//Given a map of fields read from XML, find or create a Subscription
    public static Subscription getOrAddSubscription(ContentResolver context, Map<String,String> contents) {
    	String url = contents.get(SubscriptionColumns.URL);
    	if (url==null) {
    		throw new RuntimeException("No url for subscription");
    	}
		Subscription sub = new Subscription(url);
		sub.title = contents.get(SubscriptionColumns.TITLE);
		sub.link = contents.get(SubscriptionColumns.LINK);
		sub.lastUpdated = ZipImporter.parseLong(contents.get(SubscriptionColumns.LAST_UPDATED));
		sub.comment = contents.get(SubscriptionColumns.COMMENT);
		sub.description = contents.get(SubscriptionColumns.DESCRIPTION);
		
		int rc = sub.subscribe(context);
		switch (rc) {
		case ADD_SUCCESS:
			sub.log.debug("New subscription created");
			sub.lastItemUpdated = ZipImporter.parseLong(contents.get("lastItemUpdated"));
			sub.failCount = ZipImporter.parseLong(contents.get("failCount"));
			sub.autoDownload = ZipImporter.parseLong(contents.get("autoDownload"));
			sub.suspended = ZipImporter.parseLong(contents.get("suspended"));
			return sub;
		case ADD_FAIL_DUP:
			sub.log.debug("Found existing subscription");
			return sub;
		default:
			throw new RuntimeException("no subscription");	//TBD - better message or handling
		}
    }

}
