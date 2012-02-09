package info.xuluan.podcast;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

public class ChannelTabsHelper {

	public static void setTabClickListeners(final Activity activity, int myActivityButton) {
		class ButtonAndActivity {
			ButtonAndActivity(int b, Class a) { buttonId=b; activityClass=a; }
			int buttonId;
			Class activityClass;
		}
		ButtonAndActivity[] buttonActivities = {
			new ButtonAndActivity(R.id.channel_bar_add_button,AddChannelActivity.class),
			new ButtonAndActivity(R.id.channel_bar_backup_button,BackupChannelsActivity.class),
			new ButtonAndActivity(R.id.channel_bar_manage_button,ChannelsActivity.class),
			new ButtonAndActivity(R.id.channel_bar_search_button,SearchActivity.class),
		};

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
