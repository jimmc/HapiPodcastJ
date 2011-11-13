package info.xuluan.podcast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.DialogMenu;
import info.xuluan.podcast.utils.IconCursorAdapter;
import info.xuluan.podcast.utils.SDCardMgr;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class PlayListActivity extends PodcastBaseActivity {

	private static HashMap<Integer, Integer> mIconMap;
	static {

		mIconMap = new HashMap<Integer, Integer>();
		mIconMap.put(ItemColumns.ITEM_STATUS_NO_PLAY, R.drawable.no_play);
		mIconMap.put(ItemColumns.ITEM_STATUS_PLAYED, R.drawable.played);
		mIconMap.put(ItemColumns.ITEM_STATUS_KEEP, R.drawable.keep);
		
	}

	private static final String[] PROJECTION = new String[] { ItemColumns._ID, // 0
			ItemColumns.TITLE, // 1
			ItemColumns.DURATION, ItemColumns.SUB_TITLE, ItemColumns.STATUS, // 1
	};

	// static final int MENU_REFRESH = Menu.FIRST + 1;
	static final int MENU_BACK = Menu.FIRST + 2;
	static final int MENU_PREF = Menu.FIRST + 3;

	public static final int MENU_ITEM_PLAY = Menu.FIRST + 10;
	public static final int MENU_ITEM_KEEP = Menu.FIRST + 11;
	public static final int MENU_ITEM_VIEW = Menu.FIRST + 12;
	public static final int MENU_ITEM_DELETE = Menu.FIRST + 13;
	public static final int MENU_ITEM_SHARE = Menu.FIRST + 14;
	public static final int MENU_ITEM_PLAYED_BY = Menu.FIRST + 15;
	public static final int MENU_ITEM_EXPORT = Menu.FIRST + 16;
	public static final int MENU_ITEM_ADD_TO_PLAYLIST = Menu.FIRST + 17;
	
	
	
	


	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		DialogMenu dialog_menu = createDialogMenus(id);
		if( dialog_menu==null)
			return;
		
		
		 new AlertDialog.Builder(this)
         .setTitle(dialog_menu.getHeader())
         .setItems(dialog_menu.getItems(), new EpisodeClickListener(dialog_menu,id)).show();		


	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setTitle(getResources().getString(R.string.title_play_list));

		getListView().setOnCreateContextMenuListener(this);
		Intent intent = getIntent();
		intent.setData(ItemColumns.URI);
		
		mPrevIntent = new Intent(this, DownloadingActivity.class);
		mNextIntent = new Intent(this, SearchActivity.class);		
		startInit();
	}

	public DialogMenu createDialogMenus(long id) {

		FeedItem feed_item = FeedItem.getById(getContentResolver(), id);
		if (feed_item == null) {
			return null;
		}
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(feed_item.title);
		dialog_menu.addMenu(MENU_ITEM_PLAY,
				getResources().getString(R.string.menu_play));
		dialog_menu.addMenu(MENU_ITEM_PLAYED_BY,
				getResources().getString(R.string.menu_played_by));
		dialog_menu.addMenu(MENU_ITEM_ADD_TO_PLAYLIST,
				getResources().getString(R.string.menu_add_to_playlist));	
		dialog_menu.addMenu(MENU_ITEM_EXPORT,
				getResources().getString(R.string.menu_export_audio_file));			
		if(feed_item.status!=ItemColumns.ITEM_STATUS_KEEP){
			dialog_menu.addMenu(MENU_ITEM_KEEP, 
					getResources().getString(R.string.menu_keep));			
		}		

		dialog_menu.addMenu(MENU_ITEM_SHARE,
				getResources().getString(R.string.menu_share));	
		
		dialog_menu.addMenu(MENU_ITEM_VIEW,
				getResources().getString(R.string.menu_view));
		
		dialog_menu.addMenu(MENU_ITEM_DELETE,
				getResources().getString(R.string.menu_delete));	

		return dialog_menu;
	}


	class EpisodeClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long item_id;
		public EpisodeClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			item_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		FeedItem select_item = FeedItem.getById(getContentResolver(), item_id);
    		if(select_item==null)
    			return;
    		
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_DELETE: {

    			// TODO are you sure?
    			
    			select_item.delFile(PlayListActivity.this.getContentResolver());
    			return;
    		}
    		case MENU_ITEM_PLAY: {
    			select_item.play(PlayListActivity.this);
    			return;
    		}
    		case MENU_ITEM_PLAYED_BY: {
    			Intent intent = new Intent(android.content.Intent.ACTION_VIEW); 
    	        Uri data = Uri.parse("file://"+select_item.pathname); 
				log.error(select_item.pathname);
 	        
    	        intent.setDataAndType(data,"audio/mp3"); 
    	        try { 
    	             startActivity(intent); 
    	        } catch (Exception e) { 
    	                  e.printStackTrace(); 
    	        }     			
    			return;
    		}
    		case MENU_ITEM_ADD_TO_PLAYLIST: {
    			select_item.addtoPlaylist(getContentResolver());
    			return;
    		}
    		case MENU_ITEM_EXPORT: {
    			String filename = get_export_file_name(select_item.title, select_item.id);
    			filename = SDCardMgr.getExportDir()+"/"+filename;
				log.error(filename);   			
      			 Toast.makeText(PlayListActivity.this, "Please waiting... ", 
    					 Toast.LENGTH_LONG).show();  
      			 
    			boolean b  = copy_file(select_item.pathname,filename);
    			if(b)
    			 Toast.makeText(PlayListActivity.this, "Export audio file to : "+ filename, 
    					 Toast.LENGTH_LONG).show();
    			else
       			 Toast.makeText(PlayListActivity.this, "Export failed ", 
    					 Toast.LENGTH_LONG).show();    				
    			return;
    		}    		
    		case MENU_ITEM_KEEP: {
    			if (select_item.status != ItemColumns.ITEM_STATUS_KEEP) {
    				select_item.status = ItemColumns.ITEM_STATUS_KEEP;
    				select_item.update(PlayListActivity.this.getContentResolver());
    			}
    			return;
    		}
    		case MENU_ITEM_VIEW: {
    			Uri uri = ContentUris
    					.withAppendedId(getIntent().getData(), select_item.id);
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));
    			return;
    		}
    		case MENU_ITEM_SHARE: {
    			select_item.sendMail(PlayListActivity.this);
    			return;
    		}		
    		}
		}        	
       }

	public boolean copy_file(String src, String dst)
	{
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        boolean b=true;
        try {
            File readFile = new File(src);

            File writeFile = new File(dst);

            fileInputStream = new FileInputStream(readFile);

            fileOutputStream = new FileOutputStream(writeFile);

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = fileInputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            b = false;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception ex) {}
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception ex) {}
        }	
        
        return b;
	}
	
	public String get_export_file_name(String title, long id)
	{
		title = title.replaceAll("[\\s\\\\:\\<\\>\\[\\]\\*\\|\\/\\?\\{\\}]+", "_");		

		return title+"_"+id+".mp3";
	}
	
	public void startInit() {
		String where = ItemColumns.STATUS + ">"
				+ ItemColumns.ITEM_STATUS_MAX_DOWNLOADING_VIEW + " AND " 
		+ ItemColumns.STATUS + "<" + ItemColumns.ITEM_STATUS_MAX_PLAYLIST_VIEW;
		
		String order = ItemColumns.STATUS + " ASC, " + ItemColumns.LAST_UPDATE
				+ " DESC";

		mCursor = managedQuery(ItemColumns.URI, PROJECTION, where, null, order);

		// Used to map notes entries from the database to views
		mAdapter = new IconCursorAdapter(this, R.layout.list_item, mCursor,
				new String[] { ItemColumns.TITLE, ItemColumns.SUB_TITLE,
						ItemColumns.DURATION, ItemColumns.STATUS }, new int[] {
						R.id.text1, R.id.text2, R.id.text3 }, mIconMap);

		setListAdapter(mAdapter);

		super.startInit();
	}

}
