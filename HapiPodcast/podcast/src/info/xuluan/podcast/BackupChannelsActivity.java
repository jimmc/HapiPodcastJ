package info.xuluan.podcast;

import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.OPMLParserHandler;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.SDCardMgr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

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
import android.widget.Button;
import android.widget.Toast;

public class BackupChannelsActivity extends HapiActivity {

	public static String OPML_FILE = "hapi_podcast.opml"; 
	
	private final Log log = Log.getLog(getClass());
	
	class MyFileFilter implements FileFilter{

		//@Override
		public boolean accept(File pathname) {
			return pathname.isFile();
		}
	
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.backup_channels);
		
		Button importButton = (Button) findViewById(R.id.importButton);
		importButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				import_opml();
			}
		});
		
		Button exportButton = (Button) findViewById(R.id.exportButton);
		exportButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				export_opml();
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.backup_channels, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add_channel:
			startActivity(new Intent(this, AddChannelActivity.class));
			return true;
		case R.id.search_channels:
			startActivity(new Intent(this, SearchActivity.class));
			return true;
		case R.id.list_channels:
			startActivity(new Intent(this, ChannelsActivity.class));
			return true;
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

    private void import_opml()
    {

    	if(pre_dir_handle()==false){
		Toast.makeText(this, getResources().getString(R.string.sdcard_unmout), Toast.LENGTH_LONG).show();
    		return;   		
    	}
    	
        File directory = new File(SDCardMgr.getAppDir());
        final File[] filesArray = directory.listFiles(new MyFileFilter());
        
        if(filesArray == null){
	    		Toast.makeText(BackupChannelsActivity.this, " No OPML file found!\n  " 
 	    				, Toast.LENGTH_LONG).show();	
	    		return;
        }
  
        
        if ( filesArray.length>0) {
        	String[] arr = new String[filesArray.length];

            for (int i = 0; i < filesArray.length; i++) {
            	arr[i] = new String(filesArray[i].getName());
            }
			 new AlertDialog.Builder(this)
             .setTitle("Select Import File")
             .setSingleChoiceItems(arr, 0, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int select) {
                	 FileInputStream fileInputStream = null;
                	 try {
						fileInputStream = new FileInputStream(filesArray[select]);
						OPMLParserHandler handler = new OPMLParserHandler();
						handler.context = getContentResolver();
						try {
							FeedParser.getDefault().parse(
									fileInputStream, handler);							
						} catch (Exception e) {
			            	log.debug("OPMLParserHandler Exception.");							
						}

						
						if(handler.success_num>0){
		     	    		Toast.makeText(BackupChannelsActivity.this, " Success!\n  " 
		     	    				+ handler.success_num + " added"
		     	    				, Toast.LENGTH_LONG).show();							
						} else {
		     	    		Toast.makeText(BackupChannelsActivity.this, " No New Channel Added.\n  " 
		     	    				, Toast.LENGTH_LONG).show();								
						}
						
					} catch (Exception e) {
						e.printStackTrace();						
					}finally{
		                try {
		                    if (fileInputStream != null) {
		                    	fileInputStream.close();
		                        
		                    }
		                } catch (IOException ex) {}
					}
         			dialog.dismiss();

                 }
             })
            .show();
			
        }else{
		 Toast.makeText(BackupChannelsActivity.this, " No OPML file found. \n  Please copy OPML file to the directory:\n"
				 + SDCardMgr.getAppDir()+"\n", Toast.LENGTH_LONG).show();	
		}
    	
    }
    
	private String writeXml(){
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
    private void export_opml()
    {
    	if(pre_dir_handle()==false){
    		return;
    	}
    	String xml = writeXml();
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

}
