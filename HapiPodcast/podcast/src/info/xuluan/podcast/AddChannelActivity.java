package info.xuluan.podcast;

import info.xuluan.podcast.parser.FeedHandler;
import info.xuluan.podcast.parser.FeedParserListener;
import info.xuluan.podcast.provider.Subscription;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddChannelActivity extends HapiActivity {

	private EditText mUrlText;
	private ProgressDialog progress = null;
	private FeedHandler feed_handler = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_a_channel);
		
		mUrlText = (EditText) findViewById(R.id.urlText);		
		resetUrlField();
		
		Button addButton = (Button) findViewById(R.id.addButton);
		addButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				checkValid(mUrlText.getText().toString());
			}
		});
		
		Button clearButton = (Button) findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				resetUrlField();
			}
		});
		
		TabsHelper.setChannelTabClickListeners(this, R.id.channel_bar_add_button);		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.add_a_channel, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
/*		case R.id.backup_channels:
			startActivity(new Intent(this, BackupChannelsActivity.class));
			return true;
		case R.id.search_channels:
			startActivity(new Intent(this, SearchActivity.class));
			return true;
		case R.id.list_channels:
			startActivity(new Intent(this, ChannelsActivity.class));
			return true;
*/		}
		return super.onOptionsItemSelected(item);
	}
	
	private void resetUrlField() {
		mUrlText.setText("http://");
	}
	
	void checkValid(String value) {
		String url = null;
		int fail_res = 0;
		try {
			url = formatURL(value);

			if (Subscription.getByUrl(getContentResolver(), url) != null)
				fail_res = R.string.dialog_message_url_exist;
		} catch (MalformedURLException e) {
			fail_res = R.string.dialog_message_malformed_url;
		}

		if (fail_res != 0) {
			new AlertDialog.Builder(this).setTitle(
					getResources().getText(R.string.dialog_title_add_sub))
					.setMessage(getResources().getText(fail_res))
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).show();
			return;
		}
		this.progress = ProgressDialog.show(this, getResources().getText(
				R.string.dialog_title_loading), getResources().getText(
				R.string.dialog_message_loading), true);
		AsyncTask<String, ProgressDialog, FeedParserListener> asyncTask 
			= new AsyncTask<String, ProgressDialog, FeedParserListener>() {
			String url;

			@Override
			protected FeedParserListener doInBackground(String... params) {

				url = params[0];
				// log.debug("doInBackground URL ="+url);
				feed_handler = new FeedHandler(getContentResolver(),getPrefMaxSize());
				return feed_handler.fetchFeed(url);
			}

			@Override
			protected void onPostExecute(FeedParserListener result) {

				if (AddChannelActivity.this.progress != null) {
					AddChannelActivity.this.progress.dismiss();
					AddChannelActivity.this.progress = null;
				}
				if (result != null) {
					if(result.resultCode==0){
						Toast.makeText(AddChannelActivity.this, getResources().getString(R.string.success),
								Toast.LENGTH_SHORT).show();		
						addFeed(url, result);

					}else{
						Toast.makeText(AddChannelActivity.this, getResources().getString(result.resultCode),
								Toast.LENGTH_LONG).show();						
					}

				} else {
					Toast.makeText(AddChannelActivity.this, getResources().getString(R.string.fail),
							Toast.LENGTH_LONG).show();
				}
			}
		};
		asyncTask.execute(url);
	}

	private void addFeed(String url, FeedParserListener feed) {

		Subscription sub = new Subscription(url);
		sub.subscribe(getContentResolver());
		feed_handler.updateFeed(sub, feed);

	}

	private String formatURL(String value) throws MalformedURLException {
		Pattern p = Pattern.compile("^(http|https)://.*",
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(value);
		if (m.find()) {
			URL url = new URL(value);
			return url.toString();
		}

		return null;
	}
	
    private int getPrefMaxSize() {
		SharedPreferences pref = getSharedPreferences(
				"info.xuluan.podcast_preferences", Service.MODE_PRIVATE);
		return  Integer.parseInt(pref.getString(
				"pref_max_new_items", "10"));
	}    	

}
