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
	
	private static final String TOTAL_RESULTS = "totalResults";
	private static final String START_INDEX = "startIndex";
	private static final String ITEMS_PERPAGE = "itemsPerPage";
	
	private static final String NODE_HEAD = "head";
	private static final String NODE_OUTLINE = "outline";
	
	private static final String NODE_TITLE = "title";	
	private static final String NODE_DESCRIPTION = "description";	
	private static final String NODE_XML_URL = "xmlUrl";
	private static final String NODE_HTML_URL = "htmlUrl";	

	private final Log log = Log.getLog(getClass());

	private SearchItem mCurrentItem = null;

	private boolean mInHead = false;
	private StringBuilder mCache = new StringBuilder(1024);

	public int startindex = -1 ;
	public int foundcount = 0;
	public int pagecount = 0;
	public int pagesize = 0;

	public List<SearchItem> items = new ArrayList<SearchItem>();
		

	static final Set<String> fetchChars = new HashSet<String>();

	static {
		fetchChars.add(TOTAL_RESULTS);
		fetchChars.add(START_INDEX);
		fetchChars.add(ITEMS_PERPAGE);
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
		
		if (NODE_HEAD.equalsIgnoreCase(localName)) {
			mInHead = false;
			return;
		} 	
		
		if (mInHead && (mCache != null)) {
			if (TOTAL_RESULTS.equalsIgnoreCase(localName)){
				foundcount = Integer.parseInt(mCache.toString());
				
			}else if(START_INDEX.equalsIgnoreCase(localName)){
				startindex = Integer.parseInt(mCache.toString());
			}else if(ITEMS_PERPAGE.equalsIgnoreCase(localName)){
				pagesize = Integer.parseInt(mCache.toString());
				
			}
			return;
		}
		
		//log.warn("<" + localName + ">  end!");
	}

	void stopParse() throws SAXException {
		throw new SAXException("Stop parse!");
	}

	SearchItem checkItem(SearchItem item) throws SAXException {
		if (item.title == null)
			item.title = "(Untitled)";
	

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
		
		if (item.link == null)
			item.content = "";		

		return item;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		//log.warn("<" + localName + ">  start");
		if (NODE_HEAD.equalsIgnoreCase(localName)) {
			mInHead = true;
			return;
		} 
		
		
		if (NODE_OUTLINE.equalsIgnoreCase(localName)) {
			mCurrentItem = new SearchItem();
			mCurrentItem.title = attributes.getValue(NODE_TITLE);
			mCurrentItem.url = attributes.getValue(NODE_XML_URL);
			mCurrentItem.link = attributes.getValue(NODE_HTML_URL);
			mCurrentItem.content = attributes.getValue(NODE_DESCRIPTION);
			mCurrentItem = checkItem(mCurrentItem);
			
			if(mCurrentItem != null){
				items.add(mCurrentItem);				
				mCurrentItem = null;
				return;
			}			
		} 

		

		if (fetchChars.contains(localName))
			mCache = new StringBuilder(1024);
		else
			mCache = null;
	}



}
