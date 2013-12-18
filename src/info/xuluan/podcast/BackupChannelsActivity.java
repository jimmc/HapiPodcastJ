package info.xuluan.podcast;

import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.OPMLParserHandler;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.FileUtils;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.SDCardMgr;
import info.xuluan.podcast.utils.ZipImporter;
import info.xuluan.podcastj.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

public class BackupChannelsActivity extends HapiActivity implements PodcastTab, Flingable {

	public static String OPML_FILE = "hapi_podcast.opml"; 
	
	private final Log log = Log.getLog(getClass());
	
	public static boolean importExportZipEabled = false;

	//Map from zip entry base name to FeedItem so that when we get an MP3 file
	//in the zip stream we know to which episode it belongs.
	private Map<String,FeedItem> zipImportMap;
	
	class FileOnlyFileFilter implements FileFilter{
		//@Override
		public boolean accept(File pathname) {
			return pathname.isFile();
		}	
	}

	class ZipFileFilter implements FileFilter{
		//@Override
		public boolean accept(File pathname) {
			return pathname.isFile() && pathname.toString().toLowerCase().endsWith(".zip");
		}	
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backup_channels_activity);
		
		Button importOpmlButton = (Button) findViewById(R.id.importOpmlButton);
		importOpmlButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				importOpml();
			}
		});
		
		Button exportOpmlButton = (Button) findViewById(R.id.exportOpmlButton);
		exportOpmlButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				exportOpml();
			}
		});
		
		Button importZipButton = (Button) findViewById(R.id.importZipButton);
		importZipButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				importZip();
			}
		});
		if (!BackupChannelsActivity.importExportZipEabled) {
			View zipFrame = (View)findViewById(R.id.zipFrame);
			zipFrame.setVisibility(View.GONE);
		}
		
		TabsHelper.setChannelTabClickListeners(this, R.id.channel_bar_backup_button);
		if (PodcastBaseActivity.ENABLE_FLING_TABS)
        	findViewById(R.id.topView).setOnTouchListener((new FlingGestureDetector(this).createOnTouchListener()));	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.backup_channels_activity, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
/*		case R.id.add_channel:
			startActivity(new Intent(this, AddChannelActivity.class));
			return true;
		case R.id.search_channels:
			startActivity(new Intent(this, SearchActivity.class));
			return true;
		case R.id.list_channels:
			startActivity(new Intent(this, ChannelsActivity.class));
			return true;*/
		}
		return super.onOptionsItemSelected(item);
	}
	
	private boolean pre_dir_handle()
	{
    	if(SDCardMgr.getSDCardStatusAndCreate()==false){
    		return false;
    	}
    	
		File file = new File(SDCardMgr.getAppDir());
		
		boolean exists = (file.exists());
		if (exists==false) {
			file.mkdirs();
		} 
		return true;
	}

	abstract class ImportSelectedFile {
		abstract String fileType();
		abstract String getDirectory();
		abstract void readSelectedFile(File selectedFile);
		abstract FileFilter fileFilter();
	}
	
    private void importFile(final ImportSelectedFile isf)
    {
    	if (pre_dir_handle()==false){
    		Toast.makeText(this, getResources().getString(R.string.sdcard_unmout), Toast.LENGTH_LONG).show();
    		return;   		
    	}
    	
        File directory = new File(isf.getDirectory());
        final File[] filesArray = directory.listFiles(isf.fileFilter());    
        if (filesArray == null){
        	Toast.makeText(BackupChannelsActivity.this, " No "+isf.fileType()+" file found!\n  " 
        			, Toast.LENGTH_LONG).show();	
        	return;
        }
        if (filesArray.length<=0) {
        	Toast.makeText(BackupChannelsActivity.this, " No "+isf.fileType()+" file found. \n  Please copy your file to the directory:\n"
        			+ SDCardMgr.getAppDir()+"\n", Toast.LENGTH_LONG).show();	
        	return;
        }
        
        String[] arr = new String[filesArray.length];
        for (int i = 0; i < filesArray.length; i++) {
        	arr[i] = new String(filesArray[i].getName());
        }
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int select) {
        		File selectedFile = filesArray[select];
        		isf.readSelectedFile(selectedFile);
        		dialog.dismiss();
            	Toast.makeText(BackupChannelsActivity.this,
            			"File  "+selectedFile.toString()+" imported.",
            			Toast.LENGTH_LONG).show();	
        	}
        };
        ArrayAdapter<String> choicesAdapter = new ArrayAdapter<String>(this,
				R.layout.select_item_multiline, arr);
        new AlertDialog.Builder(this)
	        .setTitle("Select "+isf.fileType()+" File")
	        .setAdapter(choicesAdapter, clickListener)
	        //.setSingleChoiceItems(arr, 0, clickListener)
	        .show();	
    }
    
	private void importOpml() {
		ImportSelectedFile imp = new ImportSelectedFile() {
			String fileType() { return "OPML"; };
			String getDirectory() { return SDCardMgr.getAppDir(); }
			void readSelectedFile(File selectedFile) { readOpml(selectedFile); }
			FileFilter fileFilter() { return new FileOnlyFileFilter(); }
		};
		importFile(imp);
	}
	
    private void readOpml(File selectedFile) {
    	FileInputStream fileInputStream = null;
    	try {
    		fileInputStream = new FileInputStream(selectedFile);
    		OPMLParserHandler handler = new OPMLParserHandler();
    		handler.context = getContentResolver();
    		try {
    			FeedParser.getDefault().parse(
    					fileInputStream, handler);							
    		} catch (Exception e) {
    			log.debug("OPMLParserHandler Exception.");							
    		}


    		if (handler.success_num>0){
    			Toast.makeText(BackupChannelsActivity.this, " Success!\n  " 
    					+ handler.success_num + " added"
    					, Toast.LENGTH_LONG).show();							
    		} else {
    			Toast.makeText(BackupChannelsActivity.this, " No New Channel Added.\n  " 
    					, Toast.LENGTH_LONG).show();								
    		}

    	} catch (Exception e) {
    		e.printStackTrace();						
    	} finally {
    		try {
    			if (fileInputStream != null) {
    				fileInputStream.close();
    			}
    		} catch (IOException ex) {}
    	}
    }

    private String writeOpml(){
		XmlSerializer serializer = Xml.newSerializer();
		StringWriter writer = new StringWriter();
		try {
			serializer.setOutput(writer);
			serializer.startDocument("UTF-8", true);
			serializer.startTag("", "opml");
			serializer.attribute("", "version", "1.1");
			serializer.startTag("", "head");
			serializer.startTag("", "title");
			serializer.text("Hapi Podcast Feeds");			
			serializer.endTag("", "title");
			serializer.endTag("", "head");
			
			
			serializer.startTag("", "body");			
			//serializer.startTag("", "outline");

			Cursor cursor = managedQuery(SubscriptionColumns.URI, SubscriptionColumns.ALL_COLUMNS,
					null, null, null);
			
			if (cursor !=null & cursor.moveToFirst()) {
				do{

					Subscription channel = Subscription.getByCursor(cursor);

					if(channel!=null){
						serializer.startTag("", "outline");
						serializer.attribute("", "title", channel.title);
						serializer.attribute("", "xmlUrl", channel.url);						
						serializer.endTag("", "outline");
					}

				}while (cursor.moveToNext());

			}
			
			if(cursor!=null)
				cursor.close();
			
			
			//serializer.endTag("", "outline");
			serializer.endTag("", "body");		
			serializer.endTag("", "opml");
			serializer.endDocument();

			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			
		} 
		
		return null;
	}
    
    private void exportOpml()
    {
    	if(pre_dir_handle()==false){
    		return;
    	}
    	String xml = writeOpml();
    	if(xml!=null){

            FileOutputStream fileOutputStream = null;
            try {
                File writeFile = new File(SDCardMgr.getAppDir(), OPML_FILE);
                fileOutputStream = new FileOutputStream(writeFile);

                fileOutputStream.write(xml.getBytes());
                
        		
        		new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Export to : "+ SDCardMgr.getAppDir() + "/" + OPML_FILE)
                .setPositiveButton(R.string.menu_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                       }
                }).show();        		

            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();   
                    }
                } catch (IOException ex) {}
            }
    	}
    	
    }
    
	private void importZip() {
		zipImportMap = new HashMap<String,FeedItem>();
		ImportSelectedFile imp = new ImportSelectedFile() {
			String fileType() { return "ZIP"; };
			String getDirectory() { return SDCardMgr.getExportDir(); }
			void readSelectedFile(File selectedFile) { readZip(selectedFile); }
			FileFilter fileFilter() { return new ZipFileFilter(); }
		};
		importFile(imp);
	}
	
    private void readZip(File selectedFile)
    {
    	ZipImporter.ContentReader cr = new ZipImporter.ContentReader() {
    		public void readContent(ZipInputStream zis, ZipEntry entry)
    				throws IOException {
    			readZipEntry(zis, entry);				
    		}
    	};
    	ZipImporter.importFromZipFile(this, selectedFile, cr);
    }
    
    private void readZipEntry(ZipInputStream zis, ZipEntry entry) {
    	String entryNameLower = entry.getName().toLowerCase();
    	if (entryNameLower.endsWith(".xml"))
    		readZipXmlEntry(zis,entry,entryNameLower.substring(0,entryNameLower.length()-4));
    	else if (entryNameLower.endsWith(".mp3"))
    		readZipMp3Entry(zis,entryNameLower.substring(0,entryNameLower.length()-4));
    	else {
    		//Unknown file type in zip file
    		//TODO - what should we do with this?
    		//Ignore it for now
    		log.debug("Unknown file "+entry.getName()+" in ZIP input");
    	}
    }

    private void readZipXmlEntry(ZipInputStream zis, ZipEntry entry, String baseName) {
    	try {
    		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    		DocumentBuilder builder = factory.newDocumentBuilder();
    		//DocumentBuilder.parse(InputStream) closes the stream when it is done,
    		//which fails for us because we need to keep it open for the next entry,
    		//so we read the current input data into a buffer and parse it from there.
    		ByteArrayOutputStream bos = new ByteArrayOutputStream();
    		FileUtils.copyFile(zis, bos);
    		byte[] contents = bos.toByteArray();
    		ByteArrayInputStream bis = new ByteArrayInputStream(contents);
    		Document dom = builder.parse(bis);
    		Element root = dom.getDocumentElement();
    		String rootName = root.getTagName();
    		if (rootName.equals("subscription"))
    			readZipXmlSubscription(zis,root);
    		else if (rootName.equals("episode"))
    			readZipXmlEpisode(zis,root,baseName);
    		else {
    			throw new RuntimeException("Unknown root element '"+rootName+"'");
    		}
    	}
    	catch (Exception e) {
    		e.printStackTrace(System.err);						
    	}
    }
    
    private void readZipXmlSubscription(ZipInputStream zis, Element root) {
    	Map<String,String> contents = ZipImporter.getChildrenContent(root);
    	log.debug("read XML for subscription: "+contents.toString());
    	Subscription sub = Subscription.getOrAddSubscription(getContentResolver(),contents);
    	String url = contents.get("url");
    	log.debug("subscription "+sub+" found or added for URL="+url);
    }
    
    private void readZipXmlEpisode(ZipInputStream zis, Element root, String baseName) {
		//TODO - if we are supposed to have an MP3 file,
		// do we use zis.getNextEntry() here to get it and copy it to a local file
		// before we enter that data into the database?
    	Map<String,String> contents = ZipImporter.getChildrenContent(root);
    	log.debug("read XML for episode: "+contents.toString());
    	Element subscriptionElement = ZipImporter.getFirstElementByTagName(root,"subscription");
    	if (subscriptionElement==null) {
    		throw new RuntimeException("No subscription data for episode");
    	}
    	Map<String,String> subscriptionContents = ZipImporter.getChildrenContent(subscriptionElement);
    	log.debug("subscriptionContents: "+subscriptionContents.toString());
    	String subscriptionUrl = subscriptionContents.get("url");
    	Subscription sub = Subscription.getByUrl(getContentResolver(),subscriptionUrl);
    	if (sub==null) {
    		log.debug("no subscription found for URL="+subscriptionUrl);
    		return;
    	}
    	log.debug("subscription "+sub+" found or added for URL="+subscriptionUrl);
    	
    	FeedItem episode = FeedItem.getOrAddEpisode(getContentResolver(), contents, sub);
    	log.debug("found or added episode, id="+episode.id+": "+episode);
    	zipImportMap.put(baseName, episode);
   }
    
    private void readZipMp3Entry(ZipInputStream zis, String baseName) {
    	try {
        	String key = baseName;
        	log.debug("read mp3 for basename="+baseName);
        	FeedItem episode = zipImportMap.get(key);
        	if (episode==null) {
        		throw new RuntimeException("No episode found to match MP3 file for basename '"+baseName+"'");
        	}
        	log.debug("found episode, id="+episode.id+": "+episode);
			String pathname = SDCardMgr.getDownloadDir()
					+ "/import_" + episode.id + ".mp3";
    		FileOutputStream fos = new FileOutputStream(pathname);
    		log.debug("copying mp3 file to "+pathname);
    		FileUtils.copyFile(zis, fos);
    		if (episode.pathname!=null) {
    			//delete the old file before writing the pointer for the new file
    			log.debug("deleting old mp3 file from "+episode.pathname);
    			int currentStatus = episode.status;
    			episode.delFile(getContentResolver());
    			episode.status = currentStatus;
    		}
    		episode.pathname = pathname;
    		episode.update(getContentResolver());
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    }
    
	//PodcastTab interface
	public int iconResource() { return R.drawable.backup_big_pic; }
	public int tabLabelResource(boolean isLandscape) { return R.string.channel_bar_button_backup; }

	//Flingable interface
	public Intent nextIntent() { return HomeActivity.nextIntent(this); }
	public Intent prevIntent() { return HomeActivity.prevIntent(this); }
}
