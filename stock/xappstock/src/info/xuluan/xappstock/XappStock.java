/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.xuluan.xappstock;

import info.xuluan.xappstock.StockMetaData;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;


public class XappStock extends ListActivity{
	private static final String TAG = "XappStock";
    IRemoteService mService = null;

    public static final int MENU_ITEM_ADD = Menu.FIRST + 1;
    public static final int MENU_ITEM_UPDATE = Menu.FIRST + 2;	
    public static final int MENU_ITEM_PREF = Menu.FIRST + 3;
    
    private static final String[] PROJECTION = new String[] {
    	StockMetaData.Stocks._ID, // 0
    	StockMetaData.Stocks.SYMBOL, // 1
    	StockMetaData.Stocks.LAST_PRICE, // 2
    	StockMetaData.Stocks.CHANGE, // 3
    	
};
    
    private MyListCursorAdapter mAdapter;
    

    
    static class MyListCursorAdapter extends SimpleCursorAdapter {
    	
        protected int[] mFrom2;
        protected int[] mTo2;    	
    
    	MyListCursorAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            mTo2 = to;
            if (cursor != null) {
                int i;
                int count = from.length;
                if (mFrom2 == null || mFrom2.length != count) {
                	mFrom2 = new int[count];
                }
                for (i = 0; i < count; i++) {
                	mFrom2[i] = cursor.getColumnIndexOrThrow(from[i]);
                }
            }            

        }    
   	
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View v = super.newView(context, cursor, parent);
			final int[] to = mTo2;
			final int count = to.length;
			final View[] holder = new View[count+1];
			Log.d(TAG, "count = "+count);

			for (int i = 0; i < count; i++) {
				holder[i] = v.findViewById(to[i]);
			}
			holder[count] = v.findViewById(R.id.icon);
			v.setTag(holder);

			return v;

		}

		public void setViewImage2(ImageView v, int value) {
			Log.d(TAG, "setViewImage2 = "+value);
			
			v.setImageResource(value);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			final View[] holder = (View[]) view.getTag();
			final int count = mTo2.length;
			final int[] from = mFrom2;
			double change=0.0;
			

			for (int i = 0; i < count+1; i++) {
				final View v = holder[i];
				if(i == 3){
					View v_icon = view.findViewById(R.id.icon);                	
					
					if (change < 0) {
						//View v_icon = view.findViewById(R.id.icon);
						setViewImage2((ImageView) v_icon,R.drawable.redstar);
					}else{
                		setViewImage2((ImageView) v_icon,R.drawable.greenstar);
						
					}
					break;
				}
				if (v != null) {
					String text = cursor.getString(from[i]);
					
					if (text == null) {
						text = "";
					}
					
					if (i == 2) { // change

						Log.d(TAG, "text = "+text);
						try{
							change = Double.parseDouble(text);
						}catch(Exception e){
							text = "0.0";
						}
						
						if (change < 0) {
							//View v_icon = view.findViewById(R.id.icon);
							//setViewImage2((ImageView) v_icon,R.drawable.redstar);
						} else {
							text = "+" + text;
						}

						text += "%";
					}

					
					if (v instanceof TextView) {
						setViewText((TextView) v, text);
					} else if (v instanceof ImageView) {
						setViewImage((ImageView) v, text);
					}
				}
			}
			
		}

	}

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.xappstock);
        getListView().setOnCreateContextMenuListener(this);

        Intent intent = getIntent();
        intent.setData(StockMetaData.Stocks.CONTENT_URI);
        Cursor cursor = managedQuery(getIntent().getData(), PROJECTION, null, null,null);

        mAdapter = new MyListCursorAdapter(this, R.layout.xappstock_item, cursor,
                new String[] { StockMetaData.Stocks.SYMBOL, StockMetaData.Stocks.LAST_PRICE,StockMetaData.Stocks.CHANGE },
                new int[] { R.id.text1, R.id.text2, R.id.text3});
        setListAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();  
        bindService(new Intent(IRemoteService.class.getName()),
                mConnection, Context.BIND_AUTO_CREATE);
       
        Log.d(TAG, "onResume"); 
    }

    
    @Override
    protected void onPause() {
    	
        super.onPause();
        Log.d(TAG, "onPause"); 
    	
    	if(mService!=null){
        	try {
				mService.setUpdateFlag(false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    	}
    	
        unbindService(mConnection);
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mService = IRemoteService.Stub.asInterface(service);

            Log.d(TAG, "onServiceConnected"); 
    		try {
    			if(mService!=null){
    				mService.setUpdateFlag(true);
    			}
    		} catch (RemoteException e) {
    			e.printStackTrace();
    		}               
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_ADD, 0, getText(R.string.Add))
                .setShortcut('3', 'a')
                .setIcon(android.R.drawable.ic_menu_add);
        
        menu.add(0, MENU_ITEM_UPDATE, 0, getText(R.string.Update))
        .setShortcut('4', 'u')
        .setIcon(android.R.drawable.ic_menu_rotate);
        
        menu.add(0, MENU_ITEM_PREF, 0, getText(R.string.Setting))
        .setShortcut('5', 's')
        .setIcon(android.R.drawable.ic_menu_preferences);        
        

        return true;
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_ADD:
            Log.d(TAG, "press menu add ");          	
        	Intent intent = new Intent(); 
        	intent.setComponent(new ComponentName( 
        	     "info.xuluan.xappstock" 
        	    ,"info.xuluan.xappstock.AddStock")); 
        	startActivity(intent);
        	break;
        case MENU_ITEM_UPDATE:
        	if(mService!=null){
            	try {
    				mService.updateStockInfo();
    			} catch (RemoteException e) {
    				e.printStackTrace();
    			}
            }
        	break;
    case MENU_ITEM_PREF:
      	
    	Intent intent1 = new Intent(); 
    	intent1.setComponent(new ComponentName( 
    	     "info.xuluan.xappstock" 
    	    ,"info.xuluan.xappstock.Pref")); 
    	startActivity(intent1);
    	break;
    }

        return super.onOptionsItemSelected(item);
}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        Log.d(TAG, "URI: "+uri);     
        String action = getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }    
}