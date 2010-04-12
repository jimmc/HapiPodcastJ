package info.xuluan.podcast;


import info.xuluan.podcast.parser.FeedParserListenerAdapter;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.provider.SubscriptionColumns;
import info.xuluan.podcast.utils.DialogMenu;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class SubsActivity extends PodcastBaseActivity {
	private final int MENU_ADD = Menu.FIRST + 2;

	
	private final int MENU_ITEM_VIEW = Menu.FIRST + 9;
	private final int MENU_ITEM_DELETE = Menu.FIRST + 10;
	private final int MENU_ITEM_AUTO = Menu.FIRST + 11;
	private final int MENU_ITEM_REFRESH = Menu.FIRST + 12;
	
	

	private ProgressDialog progress = null;

	private static final String[] PROJECTION = new String[] {
			SubscriptionColumns._ID, // 0
			SubscriptionColumns.TITLE, // 1
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.subs);
		setTitle(getResources().getString(R.string.title_subs));

		getListView().setOnCreateContextMenuListener(this);

		Intent intent = getIntent();
		intent.setData(SubscriptionColumns.URI);

		mPrevIntent = new Intent(this, SearchChannel.class);
		mNextIntent = new Intent(this, MainActivity.class);		
		startInit();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_ADD, 0, getResources().getString(R.string.menu_add))
				.setIcon(android.R.drawable.ic_menu_add);
	
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			addSubscription();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public DialogMenu createDialogMenus(long id) {

		Subscription subs = Subscription.getSubbyId(getContentResolver(), 
				id);
		if (subs == null)
			return null;		
		
		DialogMenu dialog_menu = new DialogMenu();
		
		dialog_menu.setHeader(subs.title);
		
		dialog_menu.addMenu(MENU_ITEM_VIEW, 
				getResources().getString(R.string.menu_view));

		String auto;
		if(subs.auto_download==0){
			auto = getResources().getString(R.string.menu_auto_download);
		}else{
			auto = getResources().getString(R.string.menu_manual_download);
		}       
		dialog_menu.addMenu(MENU_ITEM_AUTO, auto);

		dialog_menu.addMenu(MENU_ITEM_REFRESH, 
				getResources().getString(R.string.menu_manual_update));
		
		dialog_menu.addMenu(MENU_ITEM_DELETE, 
				getResources().getString(R.string.unsubscribe));
		
		return dialog_menu;
	}	


	class SubsClickListener implements DialogInterface.OnClickListener {
		public DialogMenu mMenu;
		public long subs_id;
		public SubsClickListener(DialogMenu menu, long id)
		{
			mMenu = menu;
			subs_id = id;
		}
		
        public void onClick(DialogInterface dialog, int select) 
        {
    		switch (mMenu.getSelect(select)) {
    		case MENU_ITEM_REFRESH: {
        		Subscription subs = Subscription.getSubbyId(getContentResolver(),
        				subs_id);
        		if (subs != null) {
        			subs.lastUpdated = 0;
        			subs.update(getContentResolver());
        			ContentValues cv = new ContentValues();
        			cv.put(SubscriptionColumns.LAST_UPDATED, 0);
        			getContentResolver().update(SubscriptionColumns.URI, cv,
        					SubscriptionColumns._ID + "=" + subs.id, null);        			
        			
        			if(mServiceBinder!=null)
        				mServiceBinder.start_update();        	
					Toast.makeText(SubsActivity.this, "Start to update channel, please waiting a little",
							Toast.LENGTH_LONG).show();					
        		}
    		}     				
    		case MENU_ITEM_VIEW: {
    			Uri uri = ContentUris.withAppendedId(SubscriptionColumns.URI, subs_id);
    			startActivity(new Intent(Intent.ACTION_EDIT, uri));
    			return;
    		}  
	
    		case MENU_ITEM_DELETE: {

    			new AlertDialog.Builder(SubsActivity.this)
                .setTitle(R.string.unsubscribe_channel)
                .setPositiveButton(R.string.menu_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                		Subscription subs = Subscription.getSubbyId(getContentResolver(),
                				subs_id);
                		if (subs != null)
                			subs.delete(getContentResolver());
    						
            			dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.menu_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    			return;
    		}
    		case MENU_ITEM_AUTO: {
    			Subscription subs = Subscription.getSubbyId(getContentResolver(),
    					subs_id);
    			if (subs == null)
    				return;			
    			subs.auto_download = 1 - subs.auto_download;
    			subs.update(getContentResolver());	
    			return ;
    		}
    		}    		

		}        	
       }	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		DialogMenu dialog_menu = createDialogMenus(id);
		if( dialog_menu==null)
			return;
		
		 new AlertDialog.Builder(this)
         .setTitle(dialog_menu.getHeader())
         .setItems(dialog_menu.getItems(), new SubsClickListener(dialog_menu,id)).show();		
	}	

	private void addSubscription() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getResources().getText(R.string.dialog_title_add_sub));
		alert.setMessage(getResources()
				.getText(R.string.dialog_message_add_sub));

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setText("http://");
		alert.setView(input);
		alert.setPositiveButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						checkValid(input.getText().toString());
					}
				});
		alert.setNegativeButton(android.R.string.cancel,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
					}
				});
		alert.show();
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
		AsyncTask<String, ProgressDialog, FeedParserListenerAdapter> asyncTask = new AsyncTask<String, ProgressDialog, FeedParserListenerAdapter>() {
			String url;

			@Override
			protected FeedParserListenerAdapter doInBackground(String... params) {

				url = params[0];
				// log.debug("doInBackground URL ="+url);
				return mServiceBinder.fetchFeed(url);
			}

			@Override
			protected void onPostExecute(FeedParserListenerAdapter result) {

				if (SubsActivity.this.progress != null) {
					SubsActivity.this.progress.dismiss();
					SubsActivity.this.progress = null;
				}
				if (result != null) {
					addFeed(url, result);
					Toast.makeText(SubsActivity.this, getResources().getString(R.string.success),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(SubsActivity.this, getResources().getString(R.string.fail),
							Toast.LENGTH_SHORT).show();
				}
			}
		};
		asyncTask.execute(url);
	}

	private void addFeed(String url, FeedParserListenerAdapter feed) {
		if (mServiceBinder == null)
			return;
		mServiceBinder.addSubscription(url);
		mServiceBinder.updateFeed(url, feed);

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

	@Override
	public void startInit() {

		mCursor = managedQuery(SubscriptionColumns.URI, PROJECTION, null, null,
				null);

		// Used to map notes entries from the database to views
		mAdapter = new SimpleCursorAdapter(this,
				android.R.layout.simple_list_item_1, mCursor,
				new String[] { SubscriptionColumns.TITLE },
				new int[] { android.R.id.text1 });
		setListAdapter(mAdapter);

		super.startInit();
	}
}
