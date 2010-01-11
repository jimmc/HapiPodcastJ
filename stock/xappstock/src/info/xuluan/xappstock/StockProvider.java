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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class StockProvider extends ContentProvider {

    private static final String TAG = "StockProvider";

    private static final String DATABASE_NAME = "stock.db";
    private static final int DATABASE_VERSION = 28;
    private static final String STOCKS_TABLE_NAME = "stocks";

    private static HashMap<String, String> sStocksProjectionMap;

    private static final int STOCKS = 1;
    private static final int STOCK_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.w(TAG, "CREATE TABLE  stocks aaa");   
        	String creat_table= "CREATE TABLE " + STOCKS_TABLE_NAME + " ("
                    + StockMetaData.Stocks._ID + " INTEGER PRIMARY KEY,"
                    + StockMetaData.Stocks.SYMBOL + " TEXT UNIQUE,"
                    + StockMetaData.Stocks.NAME + " TEXT,"
                    + StockMetaData.Stocks.OPEN + " TEXT,"
                    + StockMetaData.Stocks.HIGH + " TEXT,"
                    + StockMetaData.Stocks.LOW + " TEXT,"
                    + StockMetaData.Stocks.CHANGE + " TEXT,"
                    + StockMetaData.Stocks.PE + " TEXT,"
                    + StockMetaData.Stocks.EPS + " TEXT,"  
                    + StockMetaData.Stocks.TIME + " TEXT,"   
                    + StockMetaData.Stocks.MKT_VALUE + " TEXT,"                       
                    + StockMetaData.Stocks.LAST_PRICE + " TEXT,"

                    + StockMetaData.Stocks.MARKET + " TEXT,"                       
                    + StockMetaData.Stocks.CURRENCY_TYPE + " TEXT,"
                    
                    + StockMetaData.Stocks.SHARE + " INTEGER DEFAULT 0,"
                    + StockMetaData.Stocks.COST_BASIS + " TEXT DEFAULT '0',"
                    
                    + StockMetaData.Stocks.ORDER + " INTEGER,"
                    + StockMetaData.Stocks.CREATED_DATE + " INTEGER,"
                    + StockMetaData.Stocks.MODIFIED_DATE + " INTEGER "                
                    + ");";
            Log.w(TAG, "CREATE TABLE  stocks aaa "+creat_table);   

        	db.execSQL(creat_table);

            Log.w(TAG, "CREATE TABLE  stocks");   
            Long now = Long.valueOf(System.currentTimeMillis());
            
            String sql_insert_subs = "INSERT INTO "
                    +STOCKS_TABLE_NAME + " ("
                    + StockMetaData.Stocks.SYMBOL+","
                    + StockMetaData.Stocks.NAME + " ,"
                    + StockMetaData.Stocks.OPEN + " ,"
                    + StockMetaData.Stocks.HIGH + " ,"
                    + StockMetaData.Stocks.LOW + " ,"
                    + StockMetaData.Stocks.CHANGE + " ,"
                    + StockMetaData.Stocks.PE + " ,"
                    + StockMetaData.Stocks.EPS + " ,"  
                    + StockMetaData.Stocks.TIME + " ,"   
                    + StockMetaData.Stocks.MKT_VALUE + " ,"                      
                    + StockMetaData.Stocks.LAST_PRICE + " ,"
                    + StockMetaData.Stocks.CREATED_DATE + " ,"
                    + StockMetaData.Stocks.MODIFIED_DATE + " ,"                    
                    + StockMetaData.Stocks.ORDER + " "
                    
                    +") VALUES ('NASDAQ:GOOG', 'unknown', '', '', '', '', '', '', '', '', '', '"
                    +now+"','"+now+"','"+ now+"');";
            
            Log.w(TAG, "insert "+sql_insert_subs);               
            db.execSQL(sql_insert_subs);   
            Log.w(TAG, "insert item  GOOGLE");               
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS "+ STOCKS_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
        case STOCKS:
            qb.setTables(STOCKS_TABLE_NAME);
            qb.setProjectionMap(sStocksProjectionMap);
            break;

        case STOCK_ID:
            qb.setTables(STOCKS_TABLE_NAME);
            qb.setProjectionMap(sStocksProjectionMap);
            qb.appendWhere(StockMetaData.Stocks._ID + "=" + uri.getPathSegments().get(1));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = StockMetaData.Stocks.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

        // Tell the cursor what uri to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case STOCKS:
            return StockMetaData.Stocks.CONTENT_TYPE;

        case STOCK_ID:
            return StockMetaData.Stocks.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != STOCKS) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Make sure that the fields are all set
        if (values.containsKey(StockMetaData.Stocks.SYMBOL) == false) {
            throw new SQLException("Fail to insert row because SYMBOL is needed " + uri);
        }

        if (values.containsKey(StockMetaData.Stocks.NAME) == false) {
            values.put(StockMetaData.Stocks.NAME, "unknow");
        }

        if (values.containsKey(StockMetaData.Stocks.LAST_PRICE) == false) {
            values.put(StockMetaData.Stocks.LAST_PRICE, "0");
        }

        if (values.containsKey(StockMetaData.Stocks.OPEN) == false) {
            values.put(StockMetaData.Stocks.OPEN, "0");
        }

        if (values.containsKey(StockMetaData.Stocks.HIGH) == false) {
            values.put(StockMetaData.Stocks.HIGH, "0");
        }   

        if (values.containsKey(StockMetaData.Stocks.LOW) == false) {
            values.put(StockMetaData.Stocks.LOW, "0");
        }   

        if (values.containsKey(StockMetaData.Stocks.CHANGE) == false) {
            values.put(StockMetaData.Stocks.CHANGE, "0");
        }   

        if (values.containsKey(StockMetaData.Stocks.PE) == false) {
            values.put(StockMetaData.Stocks.PE, "0");
        }   

        if (values.containsKey(StockMetaData.Stocks.EPS) == false) {
            values.put(StockMetaData.Stocks.EPS, "0");
        }   

        if (values.containsKey(StockMetaData.Stocks.TIME) == false) {
            values.put(StockMetaData.Stocks.TIME, "0");
        }           

        if (values.containsKey(StockMetaData.Stocks.MKT_VALUE) == false) {
            values.put(StockMetaData.Stocks.MKT_VALUE, "0");
        }  
        
        Long now = Long.valueOf(System.currentTimeMillis());
        
        if (values.containsKey(StockMetaData.Stocks.ORDER) == false) {
            values.put(StockMetaData.Stocks.ORDER, now);
        }        

        if (values.containsKey(StockMetaData.Stocks.CREATED_DATE) == false) {
            values.put(StockMetaData.Stocks.CREATED_DATE, now);
        }

        if (values.containsKey(StockMetaData.Stocks.MODIFIED_DATE) == false) {
            values.put(StockMetaData.Stocks.MODIFIED_DATE, now);
        }

        

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        try {
            long rowId = db.insertOrThrow(STOCKS_TABLE_NAME, StockMetaData.Stocks.SYMBOL, values);
       
	        if (rowId > 0) {
	            Uri stockUri = ContentUris.withAppendedId(StockMetaData.Stocks.CONTENT_URI, rowId);
	            getContext().getContentResolver().notifyChange(stockUri, null);
	            return stockUri;
	        }
        } catch ( Exception  e) {
            Log.w(TAG, "Failed to insert row into", e);
            return null;
        }finally {
          }        

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case STOCKS:
            count = db.delete(STOCKS_TABLE_NAME, where, whereArgs);
            break;

        case STOCK_ID:
            String stockId = uri.getPathSegments().get(1);
            count = db.delete(STOCKS_TABLE_NAME, StockMetaData.Stocks._ID + "=" + stockId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
        case STOCKS:
            count = db.update(STOCKS_TABLE_NAME, values, where, whereArgs);
            break;

        case STOCK_ID:
            String stockId = uri.getPathSegments().get(1);
            count = db.update(STOCKS_TABLE_NAME, values, StockMetaData.Stocks._ID + "=" + stockId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
            break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(StockMetaData.AUTHORITY, "stocks", STOCKS);
        sUriMatcher.addURI(StockMetaData.AUTHORITY, "stocks/#", STOCK_ID);

        sStocksProjectionMap = new HashMap<String, String>();
        sStocksProjectionMap.put(StockMetaData.Stocks._ID, StockMetaData.Stocks._ID);
        sStocksProjectionMap.put(StockMetaData.Stocks.SYMBOL, StockMetaData.Stocks.SYMBOL);        
        sStocksProjectionMap.put(StockMetaData.Stocks.NAME, StockMetaData.Stocks.NAME);
        sStocksProjectionMap.put(StockMetaData.Stocks.LAST_PRICE, StockMetaData.Stocks.LAST_PRICE);
        
        sStocksProjectionMap.put(StockMetaData.Stocks.OPEN, StockMetaData.Stocks.OPEN);
        sStocksProjectionMap.put(StockMetaData.Stocks.HIGH, StockMetaData.Stocks.HIGH);        
        sStocksProjectionMap.put(StockMetaData.Stocks.LOW, StockMetaData.Stocks.LOW);
        sStocksProjectionMap.put(StockMetaData.Stocks.CHANGE, StockMetaData.Stocks.CHANGE);

        sStocksProjectionMap.put(StockMetaData.Stocks.PE, StockMetaData.Stocks.PE);
        sStocksProjectionMap.put(StockMetaData.Stocks.EPS, StockMetaData.Stocks.EPS);        
        sStocksProjectionMap.put(StockMetaData.Stocks.TIME, StockMetaData.Stocks.TIME);
        sStocksProjectionMap.put(StockMetaData.Stocks.MKT_VALUE, StockMetaData.Stocks.MKT_VALUE);   
 
        sStocksProjectionMap.put(StockMetaData.Stocks.MARKET, StockMetaData.Stocks.MARKET);
        sStocksProjectionMap.put(StockMetaData.Stocks.CURRENCY_TYPE, StockMetaData.Stocks.CURRENCY_TYPE); 
        
        sStocksProjectionMap.put(StockMetaData.Stocks.SHARE, StockMetaData.Stocks.SHARE); 
        sStocksProjectionMap.put(StockMetaData.Stocks.COST_BASIS, StockMetaData.Stocks.COST_BASIS); 
        
        
        sStocksProjectionMap.put(StockMetaData.Stocks.ORDER, StockMetaData.Stocks.ORDER);        
        sStocksProjectionMap.put(StockMetaData.Stocks.CREATED_DATE, StockMetaData.Stocks.CREATED_DATE);
        sStocksProjectionMap.put(StockMetaData.Stocks.MODIFIED_DATE, StockMetaData.Stocks.MODIFIED_DATE);        
    }
}
