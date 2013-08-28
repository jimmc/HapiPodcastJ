package info.xuluan.podcast;

import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.PodcastOpenHelper;
import info.xuluan.podcastj.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.Toast;

public class HomeActivity extends HapiActivity {
	
	private static boolean showDebugMenu = false;
	private static final int MENU_SETTINGS = Menu.FIRST + 1;
	private static final int MENU_DEBUG = Menu.FIRST + 2;

	public static boolean isShowDebugMenu() {
		return showDebugMenu;
	}
	public static void setShowDebugMenu(boolean show) {
		showDebugMenu = show;
	}
	public static void toggleShowDebugMenu() {
		setShowDebugMenu(!showDebugMenu);
	}
	
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
				R.string.channel_bar_button_manage,
				R.string.channel_bar_button_manage_l,
				ChannelsActivity.class);
		cfh.addIntentButton(R.drawable.backup_big_pic,
				R.string.channel_bar_button_backup, BackupChannelsActivity.class);
		if (isLandscape)
			cfh.frame().setLayoutParams(layoutParams);

		LabeledFrameHelper efh = new LabeledFrameHelper(this,"Episodes",orientation);
		ll.addView(efh.frame());
		efh.addIntentButton(R.drawable.playlist_big_pic,
				R.string.episode_bar_button_library, EpisodesActivity.class);
		efh.addIntentButton(R.drawable.download_big_pic,
				R.string.episode_bar_button_download, DownloadActivity.class);
		efh.addIntentButton(R.drawable.episode_big_pic,
				R.string.episode_bar_button_channel, ChannelActivity.class);
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
		if (showDebugMenu) {
			menu.add(0, MENU_DEBUG, 1, "Debug");
		}
		return true;
	}

	private DialogInterface.OnClickListener debugClickListener =
   		    new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int select) {
			dialog.dismiss();
			switch (select) {
			case 0:	//DB downgrade(13)
				dbDowngrade(13);
		    	Toast.makeText(HomeActivity.this, "DB downgraded to version 13", Toast.LENGTH_SHORT).show();
		    	break;
			default:
		    	Toast.makeText(HomeActivity.this, "Selected: "+select, Toast.LENGTH_SHORT).show();
		    	break;
			}
        }
    };
			
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			startActivity(new Intent(this, Pref.class));
			return true;
		case MENU_DEBUG:
			new AlertDialog.Builder(this)
				.setTitle("Debug Commands")
				.setItems(R.array.debug_commands,debugClickListener)
				.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    protected void tapHome() {
    	//Toast.makeText(this, "Already Home", Toast.LENGTH_SHORT).show();
		SharedPreferences prefsPrivate = getSharedPreferences(Pref.HAPI_PREFS_FILE_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefsPrivate.edit();
		ed.putInt("homeActivity",0);
        ed.commit();
    	startActivity(new Intent(this,MainActivity.class));
    }
    
    private void dbDowngrade(int version) {
		PodcastOpenHelper mHelper = new PodcastOpenHelper(this,version);
		/*SQLiteDatabase db =*/ mHelper.getWritableDatabase();
		//by this point, we should have already called the downgrade method in PodcastOpenHelper
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
	
	void addIntentButton(int iconId, int labelId, final Class<? extends Activity> intentClass) {
		addIntentButton(iconId, labelId, labelId, intentClass);
	}
	void addIntentButton(int iconId, int labelIdP, int labelIdL, final Class<? extends Activity> intentClass) {
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
		if (isLandscape) {
			b.setCompoundDrawablesWithIntrinsicBounds(0, iconId, 0, 0);
			b.setText(labelIdL);
		} else {
			b.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
			b.setText(labelIdP);
		}
		b.setBackgroundResource(R.drawable.home_button);
		b.setTextColor(Color.WHITE);
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
