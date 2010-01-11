package info.xuluan.podcast.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.util.Log;

public class FeedItem {

    public String url;
    public String title;
    public String author;
    public String date;
    public String content;
    public String resource;
    public String duration;
    

    static String[] DATE_FORMATS = {
        "EEE, dd MMM yyyy HH:mm:ss Z",
    	"EEE, d MMM yy HH:mm z",
    	"EEE, d MMM yyyy HH:mm:ss z",
    	"EEE, d MMM yyyy HH:mm z",
    	"d MMM yy HH:mm z",
    	"d MMM yy HH:mm:ss z",
    	"d MMM yyyy HH:mm z",
    	"d MMM yyyy HH:mm:ss z",	
    };

    public long getDate() {
        return parse();
    }

    private long parse() {

        for (String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format, Locale.US).parse(date).getTime();
            }
            catch (ParseException e) {
            }
        }

        return 0L;
    }


    @Override
    public String toString() {
        return title;
    }
}
