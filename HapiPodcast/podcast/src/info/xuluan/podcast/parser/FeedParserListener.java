package info.xuluan.podcast.parser;

import info.xuluan.podcast.provider.FeedItem;

import org.xml.sax.SAXException;

public interface FeedParserListener {

	void onFeedTitleLoad(String feedTitle);

	void onFeedDescriptionLoad(String feedDescription);

	void onFeedLinkLoad(String feedLink);

	void onItemLoad(FeedItem item) throws SAXException;
}
