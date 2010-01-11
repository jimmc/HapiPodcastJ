package info.xuluan.podcast.parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

class FeedParserHandler extends DefaultHandler {

    static final int TYPE_UNKNOWN = 0;
    static final int TYPE_RSS = 1;
    static final int TYPE_FEED = 2;

    static final String NODE_RSS_TITLE = "title";
    static final String NODE_RSS_DESCRIPTION = "description";
    static final String NODE_RSS_LINK = "link";
    static final String NODE_RSS_PUBDATE = "pubDate";
    static final String NODE_RSS_AUTHOR = "author";
    static final String NODE_RSS_CREATOR = "creator";
    static final String NODE_RSS_GUID = "guid";    


    static final String NODE_RSS_SUBTITLE = "subtitle";
    static final String NODE_RSS_SUMMARY = "summary";    
    static final String NODE_RSS_ENCLOSURE = "enclosure";    
    static final String NODE_RSS_DURATION = "duration";  
    
    static final String NODE_FEED_TITLE = "title";
    static final String NODE_FEED_SUBTITLE = "subtitle";
    static final String NODE_FEED_CONTENT = "content";
    static final String NODE_FEED_PUBLISHED = "published";
    static final String NODE_FEED_AUTHOR_NAME = "name";

    static final Set<String> fetchChars = new HashSet<String>();

    static {
        fetchChars.add(NODE_RSS_TITLE);
        fetchChars.add(NODE_RSS_DESCRIPTION);
        fetchChars.add(NODE_RSS_LINK);
        fetchChars.add(NODE_RSS_AUTHOR);
        fetchChars.add(NODE_RSS_CREATOR);
        fetchChars.add(NODE_RSS_PUBDATE);
        
        fetchChars.add(NODE_RSS_SUBTITLE);
        fetchChars.add(NODE_RSS_SUMMARY);
        fetchChars.add(NODE_RSS_DURATION);


        fetchChars.add(NODE_FEED_TITLE);
        fetchChars.add(NODE_FEED_SUBTITLE);
        fetchChars.add(NODE_FEED_CONTENT);
        fetchChars.add(NODE_FEED_PUBLISHED);
        fetchChars.add(NODE_FEED_AUTHOR_NAME);
    }

    private final FeedParserListener listener;
    private boolean feedTitleLoaded = false;
    private boolean feedDescriptionLoaded = false;

    private int type = TYPE_UNKNOWN;
    private FeedItem currentItem = null;

    private boolean firstElement = true;
    private StringBuilder cache = new StringBuilder(4096);

    public FeedParserHandler(FeedParserListener listener) {
        this.listener = listener;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (cache!=null)
            cache.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	//Log.d("END ", "<"+localName +"> ");


        if ((type==TYPE_RSS && "item".equalsIgnoreCase(localName)) || (type==TYPE_FEED && "entry".equalsIgnoreCase(localName))) {
            checkItem(currentItem);
            if(currentItem!=null)
            	listener.onItemLoad(currentItem);
            currentItem = null;
            return;
        }
        if (currentItem==null) {
            if (cache!=null) {
            	//Log.d("parser", "<"+localName +"> "+ cache.toString());
                if (type==TYPE_RSS) {
                    if (NODE_RSS_TITLE.equalsIgnoreCase(localName)) {
                        if (!feedTitleLoaded) {
                            feedTitleLoaded = true;
                            listener.onFeedTitleLoad(cache.toString());
                        }
                    }
                    else if (NODE_RSS_DESCRIPTION.equalsIgnoreCase(localName)) {
                        if (!feedDescriptionLoaded) {
                            feedDescriptionLoaded = true;
                            listener.onFeedDescriptionLoad(cache.toString());
                        }
                    }
                }
                else if (type==TYPE_FEED) {
                    if (NODE_FEED_TITLE.equalsIgnoreCase(localName)) {
                        if (!feedTitleLoaded) {
                            feedTitleLoaded = true;
                            listener.onFeedTitleLoad(cache.toString());
                        }
                    }
                    else if (NODE_FEED_SUBTITLE.equalsIgnoreCase(localName)) {
                        if (!feedDescriptionLoaded) {
                            feedDescriptionLoaded = true;
                            listener.onFeedDescriptionLoad(cache.toString());
                        }
                    }
                }
            }
        }
        else {
            if (cache!=null) {
            	//Log.d("parser item", "<"+localName +"> "+ cache.toString());
            	
                if (type==TYPE_RSS) {
                    if (NODE_RSS_TITLE.equalsIgnoreCase(localName))
                        currentItem.title = cache.toString();
                    else if (NODE_RSS_LINK.equalsIgnoreCase(localName))
                        currentItem.url = cache.toString();
                    else if (NODE_RSS_AUTHOR.equalsIgnoreCase(localName) || NODE_RSS_CREATOR.equals(localName))
                        currentItem.author = cache.toString();
                    else if (NODE_RSS_DESCRIPTION.equalsIgnoreCase(localName))
                        currentItem.content = cache.toString();
                    else if (NODE_RSS_PUBDATE.equalsIgnoreCase(localName))
                        currentItem.date = cache.toString();
                    else if (NODE_RSS_SUMMARY.equalsIgnoreCase(localName))
                        currentItem.content = cache.toString();
                    else if (NODE_RSS_DURATION.equalsIgnoreCase(localName))
                        currentItem.duration = cache.toString();
                    
                }
                else if (type==TYPE_FEED) {
                    if (NODE_FEED_TITLE.equals(localName))
                        currentItem.title = cache.toString();
                    else if (NODE_FEED_AUTHOR_NAME.equals(localName))
                        currentItem.author = cache.toString();
                    else if (NODE_FEED_CONTENT.equals(localName))
                        currentItem.content = cache.toString();
                    else if (NODE_FEED_PUBLISHED.equals(localName))
                        currentItem.date = cache.toString();
                }
            }
        }
    }

    void stopParse() throws SAXException {
        throw new SAXException("Stop parse!");
    }

    void checkItem(FeedItem item) throws SAXException {
        if (item.title==null)
            item.title = "(Untitled)";
        
        if (item.resource==null){
        	Log.w("NO RESOURCE", "item have not a resource link: "+item.title);
        	item = null;
        	return;
        }
        if (item.author==null)
            item.author = "(Unknown)";
        if (item.content==null)
            item.content = "(No content)";
        
        if (item.getDate()==0){
        	 SimpleDateFormat formatter = new SimpleDateFormat(
        			 "EEE, dd MMM yyyy HH:mm:ss Z");
		     Date currentTime = new Date();
		     item.date = formatter.format(currentTime);
	        Log.w("RSS", "item.date: "+item.date);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	//Log.d("start", "<"+localName +"> ");

    	if (firstElement) {
            if ("rss".equals(localName))
                type = TYPE_RSS;
            else if ("feed".equals(localName))
                type = TYPE_FEED;
            else
                throw new SAXException("Unknown type '<" + localName + ">'.");
            firstElement = false;
            return;
        }
        if (type==TYPE_RSS && "item".equals(localName)) {
            currentItem = new FeedItem();
            return;
        }
        else if (type==TYPE_FEED && "entry".equals(localName)) {
            currentItem = new FeedItem();
            return;
        }
        if (type==TYPE_FEED && "link".equals(localName) && currentItem!=null) {
            if ("alternate".equals(attributes.getValue("rel"))) {
                String url = attributes.getValue("href");
                if (url!=null)
                    currentItem.url = url;
            }
            return;
        }
        if (type==TYPE_RSS && "enclosure".equals(localName) && currentItem!=null) {
            	currentItem.resource = attributes.getValue("url");
            	//Log.d("res", currentItem.resource);

            return;
        }        
        if (fetchChars.contains(localName))
            cache = new StringBuilder(1024);
        else
            cache = null;
    }

}
