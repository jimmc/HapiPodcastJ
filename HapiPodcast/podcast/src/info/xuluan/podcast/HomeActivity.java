package info.xuluan.podcast;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class HomeActivity extends HapiActivity {
	
	private static final int MENU_SETTINGS = Menu.FIRST + 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LinearLayout ll = new LinearLayout(this);
		int orientation = getResources().getConfiguration().orientation;
		boolean isLandscape = (orientation==Configuration.ORIENTATION_LANDSCAPE);
		ll.setOrientation(isLandscape?
				LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
		setContentView(ll);

		LinearLayout.LayoutParams layoutParams = //isLandscape?
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		layoutParams.weight = 1;	//make frames the same width

		LabeledFrameHelper cfh = new LabeledFrameHelper(this,"Channels",orientation);
		ll.addView(cfh.frame());
		cfh.addIntentButton(R.drawable.search_big_pic,
				R.string.channel_bar_button_search, SearchActivity.class);
		cfh.addIntentButton(R.drawable.channel_add_big_pic,
				R.string.channel_bar_button_add, AddChannelActivity.class);
		cfh.addIntentButton(R.drawable.channel_big_pic,
				R.string.channel_bar_button_manage, ChannelsActivity.class);
		cfh.addIntentButton(R.drawable.backup_big_pic,
				R.string.channel_bar_button_backup, BackupChannelsActivity.class);
		if (isLandscape)
			cfh.frame().setLayoutParams(layoutParams);

		LabeledFrameHelper efh = new LabeledFrameHelper(this,"Episodes",orientation);
		ll.addView(efh.frame());
		efh.addIntentButton(R.drawable.episode_big_pic,
				R.string.episode_bar_button_all, AllItemActivity.class);
		efh.addIntentButton(R.drawable.download_big_pic,
				R.string.episode_bar_button_download, DownloadingActivity.class);
		efh.addIntentButton(R.drawable.playlist_big_pic,
				R.string.episode_bar_button_manage, PlayListActivity.class);
		efh.addIntentButton(R.drawable.player3_big_pic,
				R.string.episode_bar_button_play, PlayerActivity.class);
		if (isLandscape)
			efh.frame().setLayoutParams(layoutParams);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_SETTINGS, 0,
				getResources().getString(R.string.title_pref)).setIcon(
				android.R.drawable.ic_menu_preferences);	
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			startActivity(new Intent(this, Pref.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    protected void tapHome() {
    	//Toast.makeText(this, "Already Home", Toast.LENGTH_SHORT).show();
		SharedPreferences prefsPrivate = getSharedPreferences("info.xuluan.podcast_preferences", Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefsPrivate.edit();
		ed.putInt("homeActivity",0);
        ed.commit();
    	startActivity(new Intent(this,MainActivity.class));
    }
}

class LabeledFrameHelper {
	Activity activity;
	String frameLabel;
	int orientation;
	FrameLayout frameLayout;
	TableLayout buttonTable;
	LinearLayout linLay1;
	LinearLayout linBut2 = null;
	int buttonCount = 0;
	boolean isLandscape = false;
	
	public LabeledFrameHelper(Activity a, String label, int or) {
		activity = a;
		frameLabel = label;
		orientation = or;
		isLandscape = (orientation==Configuration.ORIENTATION_LANDSCAPE);
	}
	
	FrameLayout frame() {
		if (frameLayout==null) {
			frameLayout = (FrameLayout)activity.getLayoutInflater().inflate(R.layout.framed_grid, null);
			linLay1 = (LinearLayout) frameLayout.findViewById(R.id.linearLayout1);
			TextView tv = (TextView) frameLayout.findViewById(R.id.borderText);
			tv.setText(frameLabel);
		}
		return frameLayout;
	}
	
	void addIntentButton(int iconId, int labelId, Class<? extends Activity> intentClass) {
		addIntentButton(iconId, activity.getResources().getString(labelId), intentClass);
	}
	void addIntentButton(int iconId, String label, final Class<? extends Activity> intentClass) {
		if ((buttonCount % 2)==0) {
			//Two buttons in each row, add a new row after every second button
			linBut2 = new LinearLayout(activity);
			linBut2.setOrientation(LinearLayout.HORIZONTAL);
			linLay1.addView(linBut2);
		}
		LinearLayout.LayoutParams layoutParams = isLandscape?
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT) :
				new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		layoutParams.weight = 1;	//make all buttons the same width
		Button b = new Button(activity);
		if (isLandscape)
			b.setCompoundDrawablesWithIntrinsicBounds(0, iconId, 0, 0);
		else
			b.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
		b.setBackgroundResource(R.drawable.home_button);
		b.setTextColor(Color.WHITE);
		b.setText(label);
		b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {            
                activity.startActivity(new Intent(activity, intentClass));
            }
        });
		b.setLayoutParams(layoutParams);
		linBut2.addView(b);
		buttonCount++;			
	}
}
