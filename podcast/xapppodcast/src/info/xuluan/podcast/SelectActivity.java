package info.xuluan.podcast;

import android.app.ListActivity;
import android.content.Intent;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import info.xuluan.podcast.service.PodcastService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SelectActivity extends ListActivity{
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.select);
		startService(new Intent(this, PodcastService.class));

        setListAdapter(new SimpleAdapter(this, getData(),
        		R.layout.select_item, new String[] { "title" ,"icon"},
                new int[] { R.id.text1, R.id.icon, }));
    }

    protected List getData() {
        List<Map> myData = new ArrayList<Map>();

	    addItem(myData, getResources().getString(R.string.title_search_channel), R.drawable.search_big_pic , 
	    		new Intent(this, SearchChannel.class));
	    
	    addItem(myData, getResources().getString(R.string.title_subs), R.drawable.channel_big_pic , 
	    		new Intent(this, SubsActivity.class));	    
 
	    addItem(myData, getResources().getString(R.string.title_read_list), R.drawable.episode_big_pic , 
	    		new Intent(this, MainActivity.class));		   
	    
	    addItem(myData, getResources().getString(R.string.title_download_list), R.drawable.download_big_pic , 
	    		new Intent(this, DownloadingActivity.class));
	    
	    addItem(myData, getResources().getString(R.string.title_play_list), R.drawable.playlist_big_pic , 
	    		new Intent(this, PlayListActivity.class));	    
	    
	    addItem(myData, getResources().getString(R.string.title_pref), R.drawable.settings_big_pic , 
	    		new Intent(this, Pref.class));		    
        
        return myData;
    }




    


    protected void addItem(List<Map> data, String name,Integer icon, Intent intent) {
        Map<String, Object> temp = new HashMap<String, Object>();
        temp.put("title", name);
        temp.put("icon", icon);
        temp.put("intent", intent);
        
        data.add(temp);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map map = (Map) l.getItemAtPosition(position);

        Intent intent = (Intent) map.get("intent");
        if(intent!=null)
        	startActivity(intent);
    }
}