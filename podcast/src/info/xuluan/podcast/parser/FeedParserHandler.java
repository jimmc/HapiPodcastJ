package info.xuluan.podcast.parser;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.utils.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FeedParserHandler extends DefaultHandler {

	private static final int TYPE_UNKNOWN = 0;
	private static final int TYPE_RSS = 1;
	private static final int TYPE_FEED = 2;

	private static final String NODE_RSS_TYPE = "rss";

	private static final String NODE_RSS_ITEM = "item";

	private static final String NODE_RSS_TITLE = "title";
	private static final String NODE_RSS_DESCRIPTION = "description";
	private static final String NODE_RSS_LINK = "link";
	private static final String NODE_RSS_PUBDATE = "pubDate";
	private static final String NODE_RSS_AUTHOR = "author";
	private static final String NODE_RSS_CREATOR = "creator";
	private static final String NODE_RSS_GUID = "guid";

	private static final String NODE_RSS_SUBTITLE = "subtitle";
	private static final String NODE_RSS_SUMMARY = "summary";
	private static final String NODE_RSS_ENCLOSURE = "enclosure";
	private static final String NODE_RSS_DURATION = "duration";

	private static final String NODE_FEED_TYPE = "feed";

	private static final String NODE_FEED_ENTRY = "entry";

	private static final String NODE_FEED_TITLE = "title";
	private static final String NODE_FEED_SUBTITLE = "subtitle";
	private static final String NODE_FEED_CONTENT = "content";
	private static final String NODE_FEED_PUBLISHED = "published";
	private static final String NODE_FEED_AUTHOR_NAME = "name";

	private final FeedParserListenerInterface listener;
	private final long sub_id;
	private final Log log = Log.getLog(getClass());

	private boolean mFeedTitleLoaded = false;
	private String mFeedTitle = "";
	private boolean mFeedDescriptionLoaded = false;

	private int mType = TYPE_UNKNOWN;
	private FeedItem mCurrentItem = null;

	private boolean mFirstElement = true;
	private StringBuilder mCache = new StringBuilder(1024);

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
		fetchChars.add(NODE_RSS_ENCLOSURE);

		fetchChars.add(NODE_FEED_TITLE);
		fetchChars.add(NODE_FEED_SUBTITLE);
		fetchChars.add(NODE_FEED_CONTENT);
		fetchChars.add(NODE_FEED_PUBLISHED);
		fetchChars.add(NODE_FEED_AUTHOR_NAME);
	}

	public FeedParserHandler(FeedParserListenerInterface listener, long sub_id) {
		this.listener = listener;
		this.sub_id = sub_id;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (mCache != null)
			mCache.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

		// item end
		if ((mType == TYPE_RSS && NODE_RSS_ITEM.equalsIgnoreCase(localName))
				|| (mType == TYPE_FEED && NODE_FEED_ENTRY
						.equalsIgnoreCase(localName))) {
			mCurrentItem = checkItem(mCurrentItem);

			if (mCurrentItem != null) {
				mCurrentItem.sub_title = mFeedTitle;
				mCurrentItem.sub_id = sub_id;
				listener.onItemLoad(mCurrentItem);
			} else
				log.debug("item=null ");

			mCurrentItem = null;
			return;
		}

		// Feed tag
		if (mCurrentItem == null) {
			if (mCache != null) {
				// Log.d("parser", "<"+localName +"> "+ cache.toString());
				if (mType == TYPE_RSS) {
					if (NODE_RSS_TITLE.equalsIgnoreCase(localName)) {
						if (!mFeedTitleLoaded) {
							mFeedTitleLoaded = true;
							mFeedTitle = mCache.toString();
							listener.onFeedTitleLoad(strip(mFeedTitle));
						}
					} else if (NODE_RSS_DESCRIPTION.equalsIgnoreCase(localName)) {
						if (!mFeedDescriptionLoaded) {
							mFeedDescriptionLoaded = true;
							listener.onFeedDescriptionLoad(mCache.toString());
						}
					}
				} else if (mType == TYPE_FEED) {
					if (NODE_FEED_TITLE.equalsIgnoreCase(localName)) {
						if (!mFeedTitleLoaded) {
							mFeedTitleLoaded = true;
							listener.onFeedTitleLoad(mCache.toString());
						}
					} else if (NODE_FEED_SUBTITLE.equalsIgnoreCase(localName)) {
						if (!mFeedDescriptionLoaded) {
							mFeedDescriptionLoaded = true;
							listener.onFeedDescriptionLoad(mCache.toString());
						}
					}
				}
			}
		} else {
			if (mCache != null) {
				// Log.d("parser item", "<"+localName +"> "+ cache.toString());

				if (mType == TYPE_RSS) {
					if (NODE_RSS_TITLE.equalsIgnoreCase(localName))
						mCurrentItem.title = mCache.toString();
					else if (NODE_RSS_LINK.equalsIgnoreCase(localName))
						mCurrentItem.url = mCache.toString();
					else if (NODE_RSS_AUTHOR.equalsIgnoreCase(localName)
							|| NODE_RSS_CREATOR.equals(localName))
						mCurrentItem.author = mCache.toString();
					else if (NODE_RSS_DESCRIPTION.equalsIgnoreCase(localName)) {
						mCurrentItem.content = mCache.toString();
						// log.warn("item content " + mCurrentItem.content);
					} else if (NODE_RSS_PUBDATE.equalsIgnoreCase(localName))
						mCurrentItem.date = mCache.toString();
					else if (NODE_RSS_SUMMARY.equalsIgnoreCase(localName)) {
						if (mCurrentItem.content == null)
							mCurrentItem.content = txt2html(mCache.toString());
					} else if (NODE_RSS_DURATION.equalsIgnoreCase(localName)) {
						mCurrentItem.duration = mCache.toString();
					} else if (NODE_RSS_ENCLOSURE.equalsIgnoreCase(localName)) {
						// log.warn("item title " + mCurrentItem.title);
						if (mCurrentItem.resource != null)
							return;

						String url = mCache.toString();
						// log.warn("item resource " + url);

						Pattern p = Pattern.compile("^(http|https)://.*",
								Pattern.CASE_INSENSITIVE);
						Matcher m = p.matcher(url);
						if (m.find()) {
							// log.warn("item match");
							mCurrentItem.resource = url;
							mCurrentItem.type = "audio/mpeg";
						}
					}

				} else if (mType == TYPE_FEED) {
					if (NODE_FEED_TITLE.equals(localName))
						mCurrentItem.title = mCache.toString();
					else if (NODE_FEED_AUTHOR_NAME.equals(localName))
						mCurrentItem.author = mCache.toString();
					else if (NODE_FEED_CONTENT.equals(localName))
						mCurrentItem.content = mCache.toString();
					else if (NODE_FEED_PUBLISHED.equals(localName))
						mCurrentItem.date = mCache.toString();
				}
			}
		}
	}

	void stopParse() throws SAXException {
		throw new SAXException("Stop parse!");
	}

	FeedItem checkItem(FeedItem item) throws SAXException {
		if (item.title == null)
			item.title = "(Untitled)";
		item.title = strip(item.title);
		
		if (item.resource == null) {
			log.warn("item have not a resource link: " + item.title);
			return null;
		}
		if (item.author == null)
			item.author = "(Unknown)";
		
		if (item.content == null)
			item.content = "(No content)";

		if (item.getDate() == 0) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"EEE, dd MMM yyyy HH:mm:ss Z");
			Date currentTime = new Date();
			item.date = formatter.format(currentTime);
			// log.warn("item.date: " + item.date);
		}

		return item;
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {

		if (mFirstElement) {
			if (NODE_RSS_TYPE.equalsIgnoreCase(localName))
				mType = TYPE_RSS;
			else if (NODE_FEED_TYPE.equalsIgnoreCase(localName))
				mType = TYPE_FEED;
			else
				throw new SAXException("Unknown type '<" + localName + ">'.");
			mFirstElement = false;
			return;
		}

		if (mType == TYPE_RSS && NODE_RSS_ITEM.equalsIgnoreCase(localName)) {
			mCurrentItem = new FeedItem();
			return;
		} else if (mType == TYPE_FEED
				&& NODE_FEED_ENTRY.equalsIgnoreCase(localName)) {
			mCurrentItem = new FeedItem();
			return;
		}

		if (mType == TYPE_FEED && NODE_RSS_LINK.equalsIgnoreCase(localName)
				&& mCurrentItem != null) {
			if ("alternate".equals(attributes.getValue("rel"))) {
				String url = attributes.getValue("href");
				if (url != null)
					mCurrentItem.url = url;
			}
			return;
		}
		if (NODE_RSS_ENCLOSURE.equalsIgnoreCase(localName)
				&& mCurrentItem != null) {
			String type = attributes.getValue("type");
			if (type == null) {
				mCurrentItem.resource = attributes.getValue("url");
				mCurrentItem.type = "audio/mpeg";
			} else {
				Pattern p = Pattern.compile("audio");
				Matcher m = p.matcher(type);
				if (m.find()) {
					mCurrentItem.type = type;
					mCurrentItem.resource = attributes.getValue("url");
				}
			}
			// Log.d("res", currentItem.resource);

		}
		if (fetchChars.contains(localName))
			mCache = new StringBuilder(1024);
		else
			mCache = null;
	}

	String txt2html(String in) {
		Pattern p = Pattern.compile("\\n");
		Matcher m = p.matcher(in);
		return m.replaceAll("<br/>");
	}

	public String strip(String str) 
	{
		
		Pattern pattern = Pattern.compile("\\n");
		Matcher matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll(" ");
		
		pattern = Pattern.compile("^\\s+");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");

		pattern = Pattern.compile("\\s+$");
		matcher = pattern.matcher(str);
		str = matcher.replaceAll("");		
	
		return str;
	}
}
