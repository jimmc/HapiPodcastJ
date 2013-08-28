package info.xuluan.podcast;

import info.xuluan.podcastj.R;
import info.xuluan.podcast.service.PlayerService;
import info.xuluan.podcast.service.PodcastService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class MainActivity extends HapiListActivity{
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.main_activity);
		startService(new Intent(this, PodcastService.class));
		startService(new Intent(this, PlayerService.class));

        setListAdapter(new SimpleAdapter(this, getData(),
        		R.layout.main_item, new String[] { "title" ,"icon"},
                new int[] { R.id.text1, R.id.icon, }));
    }
    
    protected List getData() {
        List<Map> myData = new ArrayList<Map>();

	    addItem(myData, getResources().getString(R.string.title_search_channel), R.drawable.search_big_pic , 
	    		new Intent(this, SearchActivity.class),"");
	    
	    addItem(myData, getResources().getString(R.string.title_subs), R.drawable.channel_big_pic , 
	    		new Intent(this, ChannelsActivity.class),"");	    
 
	    addItem(myData, getResources().getString(R.string.title_episodes), R.drawable.playlist_big_pic , 
	    		new Intent(this, EpisodesActivity.class),"");		   
	    
	    addItem(myData, getResources().getString(R.string.title_download_list), R.drawable.download_big_pic , 
	    		new Intent(this, DownloadActivity.class),"");
	    
	    addItem(myData, getResources().getString(R.string.title_player), R.drawable.player3_big_pic , 
	    		new Intent(this, PlayerActivity.class),"");		    

	    addItem(myData, getResources().getString(R.string.title_backup), R.drawable.backup_big_pic , 
	    		new Intent(this, BackupChannelsActivity.class),"");	
	    
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
  
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map map = (Map) l.getItemAtPosition(position);

        Intent intent = (Intent) map.get("intent");
        if(intent!=null){
        	startActivity(intent);
        }
    }
    
    @Override
    protected void tapHome() {
    	//Toast.makeText(this, "Already Home", Toast.LENGTH_SHORT).show();
		SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefsPrivate.edit();
		ed.putInt("homeActivity",1);
        ed.commit();
    	startActivity(new Intent(this,HomeActivity.class));
    }
}
