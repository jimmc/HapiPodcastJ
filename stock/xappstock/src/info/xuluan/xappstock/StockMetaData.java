

package info.xuluan.xappstock;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Convenience definitions for StockProvider
 */
public final class StockMetaData {

    public static final String AUTHORITY = "info.xuluan.xappstock.stockprovider";
	
    private StockMetaData() {}	
    /**
     * Stocks table
     */
    public static final class Stocks implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI
                = Uri.parse("content://info.xuluan.xappstock.stockprovider/stocks");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.xuluan.stock";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.xuluan.stock";		
        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "priori DESC";
        public static final String SYMBOL = "symbol";
        public static final String NAME = "name";
        public static final String LAST_PRICE = "price";
        public static final String OPEN = "open";
        public static final String HIGH = "high";
        public static final String LOW = "low";
        public static final String CHANGE = "change";
        public static final String PE = "pe";
        public static final String EPS = "eps";
        public static final String TIME = "time";
        public static final String MKT_VALUE = "mv";

        public static final String SHARE = "share";
        public static final String COST_BASIS = "cost";

        public static final String MARKET = "market";        
        public static final String CURRENCY_TYPE = "ctype";
        
        public static final String ORDER = "priori";
        public static final String CREATED_DATE = "created";
        public static final String MODIFIED_DATE = "modified";
        
        
        
        


    }
}
