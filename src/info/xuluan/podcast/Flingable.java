package info.xuluan.podcast;

import android.content.Intent;

public interface Flingable {
	void startActivity(Intent intent);
	void finish();
	Intent nextIntent();
	Intent prevIntent();
}
