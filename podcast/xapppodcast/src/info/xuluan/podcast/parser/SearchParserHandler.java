package info.xuluan.podcast.parser;

import info.xuluan.podcast.parser.SearchItem;
import info.xuluan.podcast.utils.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SearchParserHandler extends DefaultHandler {



	private static final String NODE_SEARCH_RESULTS = "searchresults";

	private static final String NODE_SITE_ITEM = "siteitem";

	private static final String NODE_TITLE = "title";
	private static final String NODE_FEED_ADDRESS = "feedaddress";
	private static final String NODE_TAG_LIST = "taglisting";
	private static final String NODE_TAG = "tag";
	private static final String NODE_LANGUAGE = "language";
	private static final String NODE_CONTENT = "content";
	
	

	private final Log log = Log.getLog(getClass());

	private SearchItem mCurrentItem = null;

	private boolean mFirstElement = true;
	private StringBuilder mCache = new StringBuilder(1024);

	public int startindex = 0 ;
	public int foundcount = 0;
	public int pagecount = 0;
	public int pagesize = 0;

	public List<SearchItem> items = new ArrayList<SearchItem>();
		

	static final Set<String> fetchChars = new HashSet<String>();

	static {
		fetchChars.add(NODE_TITLE);
		fetchChars.add(NODE_FEED_ADDRESS);
		fetchChars.add(NODE_TAG);
		fetchChars.add(NODE_LANGUAGE);
		fetchChars.add(NODE_CONTENT);
	}

	public SearchParserHandler() {
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (mCache != null){
			//log.warn("characters: " + ch.toString());
			mCache.append(ch, start, length);
			
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		// item end
		if (NODE_SEARCH_RESULTS.equalsIgnoreCase(localName)){
			throw new SAXException("END OK");
		}
		
		if( mCurrentItem == null)
			return;
		
		
		if (NODE_SITE_ITEM.equalsIgnoreCase(localName)){
			
			mCurrentItem = checkItem(mCurrentItem);
			if(mCurrentItem!=null){
				items.add(mCurrentItem);				
				mCurrentItem = null;
				return;
			}
			
			
		}

		if (mCache != null) {

					if (NODE_TITLE.equalsIgnoreCase(localName)){
						mCurrentItem.title = mCache.toString();
						log.warn("TITLE: " + mCurrentItem.title);
					}else if (NODE_FEED_ADDRESS.equalsIgnoreCase(localName)){
						mCurrentItem.url = mCache.toString();
						log.warn("url: " + mCurrentItem.url);
					}else if (NODE_TAG.equalsIgnoreCase(localName)){
						mCurrentItem.tags += mCache.toString() + "\n";
						
					} else if (NODE_TAG_LIST.equalsIgnoreCase(localName)){
						//log.warn("TAG_LIST: " + mCurrentItem.tags);
						
					} else if (NODE_LANGUAGE.equalsIgnoreCase(localName)){
						mCurrentItem.language = mCache.toString();
						
					} else if (NODE_CONTENT.equalsIgnoreCase(localName)){
						mCurrentItem.content = mCache.toString();
					} 
		}
		
		//log.warn("<" + localName + ">  end!");
	}

	void stopParse() throws SAXException {
		throw new SAXException("Stop parse!");
	}

	SearchItem checkItem(SearchItem item) throws SAXException {
		if (item.title == null)
			item.title = "(Untitled)";
			
		if (item.language == null)
			item.language = "English";			

		if (item.url == null) {
			log.warn("item have not a resource link: " + item.title);
			return null;
		}
		
		Pattern p = Pattern.compile("^(http|https)://.*",
				Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(item.url);
		if (!m.find()) {
			return null;
		}
		
		if (item.content == null)
			item.content = "(No content)";

		return item;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		//log.warn("<" + localName + ">  start");

		if (mFirstElement) {
			if (NODE_SEARCH_RESULTS.equalsIgnoreCase(localName)){
				mFirstElement = false;		
				startindex = Integer.parseInt( attributes.getValue("startindex") );
				foundcount = Integer.parseInt( attributes.getValue("foundcount") );
				pagecount = Integer.parseInt( attributes.getValue("pagecount") );
				pagesize = Integer.parseInt( attributes.getValue("pagesize") );

				if(foundcount<=0){
					throw new SAXException("foundcount == 0 ");
				}
			
				
			}else
				throw new SAXException("Unknown type '<" + localName + ">'.");

			return;
		}

		if (NODE_SITE_ITEM.equalsIgnoreCase(localName)) {
			mCurrentItem = new SearchItem();
			return;
		} 
		
		if (NODE_TAG_LIST.equalsIgnoreCase(localName)) {
			mCurrentItem.tags = "";;
			return;
		} 
		
		

		if (fetchChars.contains(localName))
			mCache = new StringBuilder(1024);
		else
			mCache = null;
	}



}
