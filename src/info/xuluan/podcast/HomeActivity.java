package info.xuluan.podcast;

import info.xuluan.podcast.provider.PodcastOpenHelper;
import info.xuluan.podcast.utils.Log;
import info.xuluan.podcastj.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Toast;

public class HomeActivity extends HapiActivity {
	
	private static boolean showDebugMenu = false;
	private static final int MENU_SETTINGS = Menu.FIRST + 1;
	private static final int MENU_DEBUG = Menu.FIRST + 2;

	private static Class[] channelActivities = {
			SearchActivity.class, AddChannelActivity.class,
			ChannelsActivity.class, BackupChannelsActivity.class
	};
	private static Class[] episodeActivities = {
			EpisodesActivity.class, DownloadActivity.class,
			ChannelActivity.class, PlayerActivity.class
	};
	
	public static boolean isShowDebugMenu() {
		return showDebugMenu;
	}
	public static void setShowDebugMenu(boolean show) {
		showDebugMenu = show;
	}
	public static void toggleShowDebugMenu() {
		setShowDebugMenu(!showDebugMenu);
	}

	public static Intent nextIntent(Activity sender) {
		return adjacentIntent(sender,1); }
	public static Intent prevIntent(Activity sender) {
		return adjacentIntent(sender,-1); }
	public static Intent adjacentIntent(Activity sender, int delta) {
		Intent intent = adjacentIntent(sender, delta, channelActivities);
		if (intent==null)
			intent = adjacentIntent(sender, delta, episodeActivities);
		return intent;
	}
	public static Intent adjacentIntent(Activity sender, int delta, Class[] activities) {
		for (int i=0; i<activities.length; i++) {
			if (sender.getClass()==activities[i]) {
				i += delta;
				if (i<0)
					i += activities.length;
				else if (i>=activities.length)
					i -= activities.length;
				return new Intent(sender,activities[i]);
			}
		}
		return null;
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
		for (Class clz : channelActivities) {
			try {
				PodcastTab tab = (PodcastTab)(clz.newInstance());
				cfh.addIntentButton(tab.iconResource(), tab.tabLabelResource(isLandscape), clz);
			} catch (Exception ex) {}
		}
		if (isLandscape)
			cfh.frame().setLayoutParams(layoutParams);

		LabeledFrameHelper efh = new LabeledFrameHelper(this,"Episodes",orientation);
		ll.addView(efh.frame());
		for (Class clz : episodeActivities) {
			try {
				PodcastTab tab = (PodcastTab)(clz.newInstance());
				efh.addIntentButton(tab.iconResource(), tab.tabLabelResource(isLandscape), clz);
			} catch (Exception ex) {}
		}
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
			case 1:	//Toggle Debug Logging
				toggleDebugLogging();
				Toast.makeText(HomeActivity.this,
						"Initial Log Level is now "+Log.initialLevel(), Toast.LENGTH_SHORT).show();
				break;
			case 2:	//Toggle Import/Export Zip
				toggleImportExportZip();
				Toast.makeText(HomeActivity.this,
						"Import/Export Zip is now " +
						(BackupChannelsActivity.importExportZipEabled? "enabled" : "disabled"),
						Toast.LENGTH_SHORT).show();
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
    
    private void toggleDebugLogging() {
    	Log.setInitialLevel(Log.initialLevel()==Log.DEFAULT_LEVEL?Log.DEBUG:Log.DEFAULT_LEVEL);
    }
    
    private void toggleImportExportZip() {
    	BackupChannelsActivity.importExportZipEabled = !BackupChannelsActivity.importExportZipEabled;
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
