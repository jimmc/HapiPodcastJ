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




import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import android.net.Uri;


/**
 * A gallery of basic controls: Button, EditText, RadioButton, Checkbox,
 * Spinner. This example uses the default theme.
 */
public class StockViewer extends Activity {
	private static final String TAG = "StockViewer";
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;	
    
    private static final String[] PROJECTION = new String[] {
    	StockMetaData.Stocks._ID, // 0
    	StockMetaData.Stocks.SYMBOL, // 1
    	StockMetaData.Stocks.NAME, // 2    	
    	StockMetaData.Stocks.LAST_PRICE, // 3   
    	StockMetaData.Stocks.OPEN, // 4
    	StockMetaData.Stocks.HIGH, // 5
    	StockMetaData.Stocks.LOW, // 6    	
    	StockMetaData.Stocks.CHANGE, // 7   
    	StockMetaData.Stocks.PE, // 8
    	StockMetaData.Stocks.EPS, // 9
    	StockMetaData.Stocks.TIME, // 10    	
    	StockMetaData.Stocks.MKT_VALUE, // 11       	
    	
};
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stockviewer);
                 
        Intent intent = getIntent();
             
        Uri uri = intent.getData();
      
       Cursor cursor = managedQuery(uri, PROJECTION, null, null, null);
        TextView namevalue = (TextView) findViewById(R.id.name_value);    
        TextView lastprice = (TextView) findViewById(R.id.last_price);  
 
        TextView open = (TextView) findViewById(R.id.open);    
        TextView high = (TextView) findViewById(R.id.high);  

        TextView low = (TextView) findViewById(R.id.low);    
        TextView change = (TextView) findViewById(R.id.change);  

        TextView pe = (TextView) findViewById(R.id.pe);    
        TextView eps = (TextView) findViewById(R.id.eps);  
        
        TextView mv = (TextView) findViewById(R.id.mv);    
        TextView time = (TextView) findViewById(R.id.time);  
        
        cursor.moveToFirst();
        this.setTitle(cursor.getString(1));
        
        namevalue.setText(cursor.getString(2));
        lastprice.setText(cursor.getString(3) );
        
        open.setText(cursor.getString(4) );
        high.setText(cursor.getString(5) );
        low.setText(cursor.getString(6) );
        change.setText(cursor.getString(7)+"%" );
        pe.setText(cursor.getString(8) );
        eps.setText(cursor.getString(9) );
        time.setText(cursor.getString(10) );
        mv.setText(cursor.getString(11) );


        
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_ITEM_DELETE, 0, getText(R.string.Del))
                .setShortcut('6', 'd')
                .setIcon(android.R.drawable.ic_menu_delete);
       

        return true;
    } 
  
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_DELETE:
            Log.w(TAG, "press menu delete ");          	
            Intent intent = getIntent();
            
            Uri uri = intent.getData();
            this.getContentResolver().delete(uri, null,null);          
        	finish();
        	break;
        }
        return super.onOptionsItemSelected(item);

    }    
}
