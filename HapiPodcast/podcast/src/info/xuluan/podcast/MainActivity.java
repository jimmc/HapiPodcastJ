package info.xuluan.podcast;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;

import android.os.Bundle;
import android.util.Xml;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import info.xuluan.podcast.parser.FeedParser;
import info.xuluan.podcast.parser.OPMLParserHandler;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.service.PlayerService;
import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcast.utils.SDCardMgr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlSerializer;



public class MainActivity extends HapiListActivity{
	
	public static String OPML_FILE = "hapi_podcast.opml"; 
	
	private final Log log = Log.getLog(getClass());
	
	class MyFileFilter implements FileFilter{

		//@Override
		public boolean accept(File pathname) {
			return pathname.isFile();
		}
	
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.select);
		startService(new Intent(this, PodcastService.class));
		startService(new Intent(this, PlayerService.class));

        setListAdapter(new SimpleAdapter(this, getData(),
        		R.layout.select_item, new String[] { "title" ,"icon"},
                new int[] { R.id.text1, R.id.icon, }));
    }
    
    protected List getData() {
        List<Map> myData = new ArrayList<Map>();

	    addItem(myData, getResources().getString(R.string.title_search_channel), R.drawable.search_big_pic , 
	    		new Intent(this, SearchActivity.class),"");
	    
	    addItem(myData, getResources().getString(R.string.title_subs), R.drawable.channel_big_pic , 
	    		new Intent(this, ChannelsActivity.class),"");	    
 
	    addItem(myData, getResources().getString(R.string.title_read_list), R.drawable.episode_big_pic , 
	    		new Intent(this, AllItemActivity.class),"");		   
	    
	    addItem(myData, getResources().getString(R.string.title_download_list), R.drawable.download_big_pic , 
	    		new Intent(this, DownloadingActivity.class),"");
	    
	    addItem(myData, getResources().getString(R.string.title_play_list), R.drawable.playlist_big_pic , 
	    		new Intent(this, PlayListActivity.class),"");	    
	    addItem(myData, getResources().getString(R.string.title_player), R.drawable.player3_big_pic , 
	    		new Intent(this, PlayerActivity.class),"");		    

	    addItem(myData, getResources().getString(R.string.title_backup), R.drawable.backup_big_pic , 
	    		null,"backup");	
	    
	    addItem(myData, getResources().getString(R.string.title_pref), R.drawable.settings_big_pic , 
	    		new Intent(this, Pref.class),"");	
	    
	    
        
        return myData;
    }

    protected void addItem(List<Map> data, String name,Integer icon, Intent intent, String cmd) {
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.put("title", name);
        temp.put("icon", icon);
        temp.put("intent", intent);
        temp.put("cmd",cmd);
        
        data.add(temp);
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
	    		Toast.makeText(MainActivity.this, " No OPML file found!\n  " 
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
		     	    		Toast.makeText(MainActivity.this, " Success!\n  " 
		     	    				+ handler.success_num + " added"
		     	    				, Toast.LENGTH_LONG).show();							
						} else {
		     	    		Toast.makeText(MainActivity.this, " No New Channel Added.\n  " 
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
		 Toast.makeText(MainActivity.this, " No OPML file found. \n  Please copy OPML file to the directory:\n"
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
    
    private void backup()
    {
         new AlertDialog.Builder(this)
        .setTitle(R.string.title_backup)
        .setItems(R.array.backup_select, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	
            	if(which == 0){
            		import_opml();
            		
            	}else if(which ==1){
            		export_opml();
            	}

            }
        })
        .show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map map = (Map) l.getItemAtPosition(position);

        Intent intent = (Intent) map.get("intent");
        if(intent!=null){
        	startActivity(intent);
        } else {
        	String cmd = (String) map.get("cmd");
        	if(cmd.equals("backup")){
        		backup();
        	}
        }
    }
    
    @Override
    protected void tapHome() {
    	Toast.makeText(this, "Already Home", Toast.LENGTH_SHORT).show();
    }
}
