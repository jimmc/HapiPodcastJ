package info.xuluan.podcast.provider;

import info.xuluan.podcast.ChannelActivity;
import info.xuluan.podcast.PlayerActivity;
import info.xuluan.podcast.utils.FileUtils;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.SDCardMgr;
import info.xuluan.podcast.utils.ZipExporter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

public class FeedItem {
	
	public static final int MAX_DOWNLOAD_FAIL = 5;
	
	private final Log log = Log.getLog(getClass());

	public String url;
	public String title;
	public String author;
	public String date;
	public String content;
	public String resource;
	public String duration;

	public long id;

	public long sub_id;

	public String pathname;
	public int offset;
	public int status;
	public long failcount;
            //failcount is currently used for two purposes:
            //1. counts the number of times we fail to download, and
            //   when we exceed a predefined max, we pause the download.
            //2. when an item is in the player, failcount is used as
            //   the order of the item in the list.
	public int keep;	//1 if we should not expire this item
	
	public long length;

	public long update;

	public String uri;

	public String sub_title;
	public long created;

	public String type;
	
	private long m_date;

	static String[] DATE_FORMATS = { 
		"EEE, dd MMM yyyy HH:mm:ss Z",
		"EEE, d MMM yy HH:mm z", 
		"EEE, d MMM yyyy HH:mm:ss z",
		"EEE, d MMM yyyy HH:mm z", 
		"d MMM yy HH:mm z",
		"d MMM yy HH:mm:ss z", 
		"d MMM yyyy HH:mm z",
		"d MMM yyyy HH:mm:ss z", 
		"yyyy-MM-dd HH:mm", 
		"yyyy-MM-dd HH:mm:ss", 
		"EEE,dd MMM yyyy HH:mm:ss Z",};
	
	static String default_format = "EEE, dd MMM yyyy HH:mm:ss Z";

	public static void view(Activity act, long item_id) {
		Uri uri = ContentUris.withAppendedId(ItemColumns.URI, item_id);
		FeedItem item = FeedItem.getById(act.getContentResolver(), item_id);
		if ((item != null)
				&& (item.status == ItemColumns.ITEM_STATUS_UNREAD)) {
			item.status = ItemColumns.ITEM_STATUS_READ;
			item.update(act.getContentResolver());
		}    			
		act.startActivity(new Intent(Intent.ACTION_EDIT, uri));   
	}
	
	public static void viewChannel(Activity act, long item_id) {
		FeedItem item = FeedItem.getById(act.getContentResolver(), item_id);
		item.viewChannel(act);
	}

	public static void play(Activity act, long item_id) {
		FeedItem feeditem = FeedItem.getById(act.getContentResolver(), item_id);
		if (feeditem == null)
			return;
		feeditem.play(act);
	}
	
	//True if we found the item and added it to the playlist
	public static boolean addToPlaylist(Activity act, long item_id) {
		FeedItem feeditem = FeedItem.getById(act.getContentResolver(), item_id);
		if (feeditem != null) {
			feeditem.addtoPlaylist(act.getContentResolver());
			return true;
		} else {
			return false;
		}
	}

	public static FeedItem getBySQL(ContentResolver context,
			String where, String[] args, String order) {
		FeedItem item = null;
		Cursor cursor = null;
	
		try {
			cursor = context.query(
					ItemColumns.URI,
					ItemColumns.ALL_COLUMNS,
					where,
					args,
					order);
			if (cursor.moveToFirst()) {
				item = FeedItem.getByCursor(cursor);
			}
		}finally {
			if (cursor != null)
				cursor.close();
		}		
		return item;
						
	}
	
	public static FeedItem getBySQL(ContentResolver context,String where,String order) {
		return getBySQL(context, where, null, order);
	}
	
	public static FeedItem getByResource(ContentResolver context, String res) {
		String where = ItemColumns.RESOURCE + " =?";
		String[] args = new String[] { res };
		return getBySQL(context, where, args, null);
	}

	public static FeedItem getById(ContentResolver context, long id) {
		String where = ItemColumns._ID + " = " + id;
		return getBySQL(context, where, null);
	}

	public static FeedItem getByCursor(Cursor cursor) {
		//if (cursor.moveToFirst() == false)
		//	return null;
		FeedItem item = new FeedItem();
		fetchFromCursor(item, cursor);
		return item;
	}

	public FeedItem() {
		url = null;
		title = null;
		author = null;
		date = null;
		content = null;
		resource = null;
		duration = null;
		pathname = null;
		uri = null;
		type = null;

		id = -1;
		offset = -1;
		status = -1;
		failcount = -1;
		length = -1;
		update = -1;
		keep = -1;

		created = -1;
		sub_title = null;
		sub_id = -1;
		
		m_date = -1;
	}
	
	public void updateOffset(ContentResolver context, long i)
	{
		offset = (int)i;
		update = -1;
		update(context);
	}
	
	public void playingOrPaused(boolean isPlaying, ContentResolver context)
	{
		if (isPlaying)
			playing(context);
		else
			paused(context);				
	}
	
	public void playing(ContentResolver context)
	{
		if(status != ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAYING_NOW;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void paused(ContentResolver context)
	{
		if(status == ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAY_PAUSE;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void played(ContentResolver context)
	{
		offset = 0;
		if(status == ItemColumns.ITEM_STATUS_NO_PLAY ||
           status == ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			status = ItemColumns.ITEM_STATUS_PLAYED;
			update = Long.valueOf(System.currentTimeMillis());
		}else{
			update = -1;
		}
		update(context);		
	}
	
	public void addtoPlaylistByOrder(ContentResolver context, long order)
	{
		failcount = order;
		if(status == ItemColumns.ITEM_STATUS_NO_PLAY) {
			status = ItemColumns.ITEM_STATUS_PLAY_READY;
		}
		update = -1;
		update(context);	
	}
	
	public void addtoPlaylist(ContentResolver context)
	{
		addtoPlaylistByOrder(context,Long.valueOf(System.currentTimeMillis()));		
	}		

	public void removeFromPlaylist(ContentResolver context)
	{
		failcount = 0;
		if(status == ItemColumns.ITEM_STATUS_PLAY_READY) {
			status = ItemColumns.ITEM_STATUS_NO_PLAY;
		}
		update = -1;
		update(context);		
	}

	public void markKeep(ContentResolver context) {
		if (this.keep <= 1) {
			this.keep = 1;
			this.update(context);
		}
	}
	public void markUnkeep(ContentResolver context) {
		if (this.keep > 0) {
			this.keep = 0;
			this.update(context);
		}
	}
	public void markNew(ContentResolver context) {
		if (this.status > ItemColumns.ITEM_STATUS_NO_PLAY &&
				this.status!=ItemColumns.ITEM_STATUS_PLAYING_NOW) {
			this.status = ItemColumns.ITEM_STATUS_NO_PLAY;
			this.failcount = 0;
			this.updateOffset(context,0);
		}
	}
	
	public void update(ContentResolver context) {
		log.debug("item update start");
		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (status >= 0)
				cv.put(ItemColumns.STATUS, status);
			if (failcount >= 0)
				cv.put(ItemColumns.FAIL_COUNT, failcount);
			
			if(update >= 0){
				update = Long.valueOf(System.currentTimeMillis());
				cv.put(ItemColumns.LAST_UPDATE, update);
			}
			if (created >= 0)
				cv.put(ItemColumns.CREATED, created);
			if (length >= 0)
				cv.put(ItemColumns.LENGTH, length);
			if (uri != null)
				cv.put(ItemColumns.MEDIA_URI, uri);
			if (type != null)
				cv.put(ItemColumns.TYPE, type);
			if (keep>=0)
				cv.put(ItemColumns.KEEP, keep);

			context.update(ItemColumns.URI, cv, ItemColumns._ID + "=" + id,
					null);

			log.debug("update OK");
		} finally {
		}
	}

	public Uri insert(ContentResolver context) {
		log.debug("item insert start");
		try {

			ContentValues cv = new ContentValues();
			if (pathname != null)
				cv.put(ItemColumns.PATHNAME, pathname);
			if (offset >= 0)
				cv.put(ItemColumns.OFFSET, offset);
			if (status >= 0)
				cv.put(ItemColumns.STATUS, status);
			if (failcount >= 0)
				cv.put(ItemColumns.FAIL_COUNT, failcount);
			if (update >= 0)
				cv.put(ItemColumns.LAST_UPDATE, update);
			if (length >= 0)
				cv.put(ItemColumns.LENGTH, length);

			if (sub_id >= 0)
				cv.put(ItemColumns.SUBS_ID, sub_id);
			if (url != null)
				cv.put(ItemColumns.URL, url);
			if (title != null)
				cv.put(ItemColumns.TITLE, title);

			if (author != null)
				cv.put(ItemColumns.AUTHOR, author);
			if (date != null)
				cv.put(ItemColumns.DATE, date);
			if (content != null)
				cv.put(ItemColumns.CONTENT, content);
			if (resource != null)
				cv.put(ItemColumns.RESOURCE, resource);
			if (duration != null) {
				// Log.w("ITEM","  duration: " + duration);
				cv.put(ItemColumns.DURATION, duration);
			}
			if (sub_title != null) {
				cv.put(ItemColumns.SUB_TITLE, sub_title);
			}
			if (uri != null)
				cv.put(ItemColumns.MEDIA_URI, uri);
			if (keep >= 0)
				cv.put(ItemColumns.KEEP, keep);

			Uri newUri = context.insert(ItemColumns.URI, cv);
    		log.debug("Uri for new item is "+newUri);
    		String idString = newUri.getLastPathSegment();
    		id = Integer.valueOf(idString);
    		return newUri;

		} finally {
		}
	}

	public void viewChannel(Activity act) {
		//Subscription sub = Subscription.getSubbyId(getContentResolver(), item.sub_id);
		Uri chUri = ContentUris.withAppendedId(SubscriptionColumns.URI, this.sub_id);
		if (ChannelActivity.channelExists(act,chUri))
			act.startActivity(new Intent(Intent.ACTION_EDIT, chUri));
		else {
			String subTitle = this.sub_title;
			if (subTitle==null || subTitle.equals(""))
				subTitle = "(no channel title)";
			String tstr = String.format("Channel not found: '%s'", subTitle);
			Toast.makeText(act, tstr, Toast.LENGTH_SHORT).show();
		}
	}

	public void playedBy(Activity act) {
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
	    Uri data = Uri.parse("file://"+this.pathname); 
		log.error(this.pathname);
	 
	    intent.setDataAndType(data,"audio/mp3"); 
	    try { 
	         act.startActivity(intent); 
	    } catch (Exception e) { 
	         e.printStackTrace();
	    }
	}

	public void export(Activity act) {
		String filename = FileUtils.getExportFileName(this.title, this.id, fileType());
		filename = SDCardMgr.getExportDir()+"/"+filename;
		log.error(filename);   			
			 Toast.makeText(act, "Please wait... ", 
				 Toast.LENGTH_LONG).show();  
			 
		boolean b  = FileUtils.copyFile(this.pathname,filename);
		if(b)
		 Toast.makeText(act, "Exported audio file to : "+ filename, 
				 Toast.LENGTH_LONG).show();
		else
			 Toast.makeText(act, "Export failed ", 
				 Toast.LENGTH_LONG).show();    				
	}

	public void exportToZipFile(Activity act) {
		final Subscription sub = Subscription.getById(act.getContentResolver(),sub_id);
		String filename = ZipExporter.getExportZipFileName(this.title + "_" + this.id);
		ZipExporter.ContentWriter cw = new ZipExporter.ContentWriter() {
			public void writeContent(ZipOutputStream zos) throws IOException{
				sub.exportMetaToZip(zos);	//start by exporting the subscription details
				exportToZipStream(zos,sub);
			}
		};
		ZipExporter.exportToZipFile(act, filename, cw);
	}
	
	public void exportToZipStream(ZipOutputStream zos, Subscription sub) throws IOException {
		exportMetaToZip(zos, sub);
		exportMusicToZip(zos);
	}

	private void exportMetaToZip(ZipOutputStream zos, Subscription sub) throws IOException {
		String filename = FileUtils.getExportFileName(this.title, this.id, "xml");
		ZipEntry ze = new ZipEntry(filename);
		zos.putNextEntry(ze);
		PrintWriter pw = new PrintWriter(zos);
		writeXml(pw, sub);
		pw.flush();
		zos.closeEntry();
	}
	
	private void exportMusicToZip(ZipOutputStream zos) throws IOException {
		boolean isDownloaded = (status > ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW);
		boolean isDeleted = (status >= ItemColumns.ITEM_STATUS_MIN_DELETE);
		boolean contentAvailable = isDownloaded && !isDeleted;
		if (!contentAvailable)
			return;	//no content to output
		String filename = FileUtils.getExportFileName(this.title, this.id, fileType());
		FileInputStream fileInputStream = null;
		try {
            File readFile = new File(this.pathname);
            fileInputStream = new FileInputStream(readFile);

			ZipEntry ze = new ZipEntry(filename);
			zos.putNextEntry(ze);
			FileUtils.copyFile(fileInputStream,zos);
			zos.closeEntry();
		} finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception ex) {}
		}
	}

	private void writeXml(PrintWriter out, Subscription sub) {
		int episodeLevel = 1;
		out.print("<episode>\n");
		
		int subcriptionLevel = episodeLevel + 1;
		ZipExporter.writeTagLine(out,"subscription",episodeLevel,true);
		ZipExporter.writeXmlField(out,"id",Long.toString(sub_id),subcriptionLevel);
		ZipExporter.writeXmlField(out,SubscriptionColumns.TITLE,sub.title,subcriptionLevel);
		ZipExporter.writeXmlField(out,SubscriptionColumns.URL,sub.url,subcriptionLevel);
		ZipExporter.writeTagLine(out,"subscription",episodeLevel,false);
		
		ZipExporter.writeXmlField(out,"id",Long.toString(id),episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.URL,url,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.MEDIA_URI,uri,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.TYPE,type,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.TITLE,title,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.SUB_TITLE,sub_title,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.AUTHOR,author,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.DATE,date,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.CONTENT,content,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.RESOURCE,resource,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.DURATION,duration,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.PATHNAME,pathname,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.CREATED,created,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.OFFSET,offset,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.LENGTH,length,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.STATUS,status,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.KEEP,keep,episodeLevel);
		ZipExporter.writeXmlField(out,ItemColumns.FAIL_COUNT,failcount,episodeLevel);
		out.print("</episode>");
	}

	private void loadXmlFields(Map<String, String> contents) {
		uri = contents.get(ItemColumns.MEDIA_URI);
		type = contents.get(ItemColumns.TYPE);
		title = contents.get(ItemColumns.TITLE);
		sub_title = contents.get(ItemColumns.SUB_TITLE);
		author = contents.get(ItemColumns.AUTHOR);
		date = contents.get(ItemColumns.DATE);
		content = contents.get(ItemColumns.CONTENT);
		resource = contents.get(ItemColumns.RESOURCE);
		duration = contents.get(ItemColumns.DURATION);
		//We don't change the pathname - that will get updated when we actually read in the mp3 file.
		//If this is an existing record, there will be a pathname that points to the old file.
		//That file will be deleted when we read in the new file.
		//TODO - might need to delete it here if the imported data indicates that the episode
		// data is not yet downloaded or already deleted.
		//pathname = contents.get(ItemColumns.PATHNAME);
		String createdStr = contents.get(ItemColumns.CREATED);
		if (createdStr!=null) {
			created = Long.valueOf(createdStr);
		}
		String offsetStr = contents.get(ItemColumns.OFFSET);
		if (offsetStr!=null) {
			offset = Integer.valueOf(offsetStr);
		}
		String lengthStr = contents.get(ItemColumns.LENGTH);
		if (lengthStr!=null) {
			length = Long.valueOf(lengthStr);
		}
		String statusStr = contents.get(ItemColumns.STATUS);
		if (statusStr!=null) {
			status = Integer.valueOf(statusStr);
		}
		String keepStr = contents.get(ItemColumns.KEEP);
		if (keepStr!=null) {
			keep = Integer.valueOf(keepStr);
		}
		String failCountStr = contents.get(ItemColumns.FAIL_COUNT);
		if (failCountStr!=null) {
			failcount = Integer.valueOf(failCountStr);
		}
	}
	
	//Given a map of fields read from XML, find or create a Subscription
    public static FeedItem getOrAddEpisode(ContentResolver context,
    		Map<String,String> contents, Subscription sub) {
    	String res = contents.get(ItemColumns.RESOURCE);
    	if (res==null) {
    		throw new RuntimeException("No resource for episode");
    	}
    	FeedItem item = FeedItem.getByResource(context, res);
    	if (item==null) {
    		item = new FeedItem();
    		item.loadXmlFields(contents);
    		item.sub_id = sub.id;
    		item.log.debug("Creaed new FeedItem id="+item.id+" sub_id="+item.sub_id+" for resource="+res);
    		item.insert(context);
    	} else {
    		item.log.debug("Found existing FeedItem id="+item.id+" for resource="+res);
    		item.loadXmlFields(contents);
    		item.sub_id = sub.id;
    		item.update(context);
    	}    	
    	return item;
    }
    
	public long getDate() {
		//log.debug(" getDate() start");
		if(m_date<0){
			m_date  = parse();
			//log.debug(" getDate() end " + default_format);	
		}
		return m_date;
	}

	private long parse() {
		long l = 0;
		try{
			return  new SimpleDateFormat(default_format, Locale.US).parse(date)
			.getTime();
		} catch (ParseException e) {
			log.debug(" first fail");
		}	

		for (String format : DATE_FORMATS) {
			try {
				l = new SimpleDateFormat(format, Locale.US).parse(date)
						.getTime();
				default_format = format;
				return l;
			} catch (ParseException e) {
			}
		}
		log.warn("cannot parser date: " + date);
		return 0L;
	}

	private static void fetchFromCursor(FeedItem item, Cursor cursor) {
		//assert cursor.moveToFirst();
		//cursor.moveToFirst();
		item.id = cursor.getLong(cursor.getColumnIndex(ItemColumns._ID));
		item.resource = cursor.getString(cursor
				.getColumnIndex(ItemColumns.RESOURCE));
		item.pathname = cursor.getString(cursor
				.getColumnIndex(ItemColumns.PATHNAME));
		item.offset = cursor.getInt(cursor.getColumnIndex(ItemColumns.OFFSET));
		item.status = cursor.getInt(cursor.getColumnIndex(ItemColumns.STATUS));
		item.failcount = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.FAIL_COUNT));

		item.length = cursor.getLong(cursor.getColumnIndex(ItemColumns.LENGTH));

		item.url = cursor.getString(cursor.getColumnIndex(ItemColumns.URL));
		item.title = cursor.getString(cursor.getColumnIndex(ItemColumns.TITLE));
		item.author = cursor.getString(cursor
				.getColumnIndex(ItemColumns.AUTHOR));
		item.date = cursor.getString(cursor.getColumnIndex(ItemColumns.DATE));
		item.content = cursor.getString(cursor
				.getColumnIndex(ItemColumns.CONTENT));
		item.duration = cursor.getString(cursor
				.getColumnIndex(ItemColumns.DURATION));
		item.uri = cursor.getString(cursor
				.getColumnIndex(ItemColumns.MEDIA_URI));

		item.created = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.CREATED));
		item.update = cursor.getLong(cursor
				.getColumnIndex(ItemColumns.LAST_UPDATE));		
		item.sub_title = cursor.getString(cursor
				.getColumnIndex(ItemColumns.SUB_TITLE));
		item.sub_id = cursor.getLong(cursor.getColumnIndex(ItemColumns.SUBS_ID));
		item.type = cursor.getString(cursor.getColumnIndex(ItemColumns.TYPE));
		item.keep = cursor.getInt(cursor.getColumnIndex(ItemColumns.KEEP));
	}

	@Override
	public String toString() {
		return title;
	}

	public String fileType() {
		return "mp3";
	}
	
	public String getType() {
		if (type == null) {
			return "audio/mpeg";
		}

		if (!type.equalsIgnoreCase("")) {
			return type;
		}

		return "audio/mpeg";
	}
	
	public void play(Activity act) {
		//item.play(ReadActivity.this);
		Intent intent = new Intent(act, PlayerActivity.class);
		intent.putExtra("item_id", id);
		act.startActivity(intent);
	}
	
	public void delFile(ContentResolver context){
		if(status<ItemColumns.ITEM_STATUS_DELETE){
			status = ItemColumns.ITEM_STATUS_DELETE;
			update(context);	
		}

		if (SDCardMgr.getSDCardStatus()) {
			try {
				File file = new File(pathname);
				
				boolean deleted = true;
				if(file.exists()==true)
				{
					deleted = file.delete();					
				}
				if(deleted){
					if(status<ItemColumns.ITEM_STATUS_DELETED){
						status = ItemColumns.ITEM_STATUS_DELETED;
						update(context);	
					}						
				}
			} catch (Exception e) {
				log.warn("del file failed : " + pathname + "  " + e);
			}
		}		
	}
	
	private String getMailBody(){
		
		String text;
		text = "audio title: "+title+" \n";
		text +="download address: "+resource;
		
		text +="\n-------------------------------------------------------------\n";
		text +="from Hapi Podcast http://market.android.com/search?q=pname:info.xuluan.podcast";
		
		return text;
		
	}
	
	public void startDownload(ContentResolver context)
	{
		if (pathname.equals("")) {
			pathname = SDCardMgr.getDownloadDir()
					+ "/podcast_" + id + "." + fileType();
		}
		status = ItemColumns.ITEM_STATUS_DOWNLOADING_NOW;
		update(context);
		
	}

	public void downloadSuccess()
	{
		status = ItemColumns.ITEM_STATUS_NO_PLAY;
	}	
	
	public void endDownload(ContentResolver context)
	{
		
		if (status == ItemColumns.ITEM_STATUS_NO_PLAY) {
			update = Long.valueOf(System.currentTimeMillis());
			failcount = 0;
			offset = 0;

		} else {
			if (status != ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE)
				status = ItemColumns.ITEM_STATUS_DOWNLOAD_QUEUE;
			failcount++;
			if (failcount > MAX_DOWNLOAD_FAIL) {
				status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
				failcount = 0;
			}
		}

		update(context);		
	}	

	public void sendMail(Activity act){
	
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND); 
		emailIntent .setType("plain/text"); 
		//emailIntent .putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"xuluan.android@gmail.com"}); 
		emailIntent .putExtra(android.content.Intent.EXTRA_SUBJECT, "please listen..."); 
		emailIntent .putExtra(android.content.Intent.EXTRA_TEXT, getMailBody()); 
		act.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
	}
}
