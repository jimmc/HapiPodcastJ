package info.xuluan.podcast;

import info.xuluan.podcastj.R;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class TabsHelper {

	static class ButtonAndActivity {
		ButtonAndActivity(int b, Class a) { buttonId=b; activityClass=a; }
		int buttonId;
		Class activityClass;
	}
	
	static ButtonAndActivity[] channelButtonActivities = {
		new ButtonAndActivity(R.id.channel_bar_add_button,AddChannelActivity.class),
		new ButtonAndActivity(R.id.channel_bar_backup_button,BackupChannelsActivity.class),
		new ButtonAndActivity(R.id.channel_bar_manage_button,ChannelsActivity.class),
		new ButtonAndActivity(R.id.channel_bar_search_button,SearchActivity.class),
	};

	static ButtonAndActivity[] episodeButtonActivities = {
		new ButtonAndActivity(R.id.episode_bar_all_button,AllItemActivity.class),
		new ButtonAndActivity(R.id.episode_bar_download_button,DownloadingActivity.class),
		new ButtonAndActivity(R.id.episode_bar_manage_button,PlayListActivity.class),
		new ButtonAndActivity(R.id.episode_bar_play_button,PlayerActivity.class),
	};

	public static void setChannelTabClickListeners(final Activity activity, int myActivityButton) {
		setTabClickListeners(activity, myActivityButton, channelButtonActivities);
	}
	
	public static void setEpisodeTabClickListeners(final Activity activity, int myActivityButton) {
		setTabClickListeners(activity, myActivityButton, episodeButtonActivities);
	}
	
	public static void setTabClickListeners(final Activity activity, int myActivityButton,
			ButtonAndActivity[] buttonActivities) {

		for (ButtonAndActivity ba : buttonActivities) {
			if (myActivityButton!=ba.buttonId) {
				Button button = (Button) activity.findViewById(ba.buttonId);
				final Class ac = ba.activityClass;
				button.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						activity.startActivity(new Intent(activity, ac));
					}
				});
			}
		}	

	}
}
