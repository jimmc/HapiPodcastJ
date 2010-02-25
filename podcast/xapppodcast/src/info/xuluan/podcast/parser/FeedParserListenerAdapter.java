package info.xuluan.podcast.parser;

import info.xuluan.podcast.provider.FeedItem;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

public class FeedParserListenerAdapter implements FeedParserListener {

	public static final int MAX_SIZE = 6;

	private String feedTitle;
	private String feedDescription;
	private String feedLink;
	private List<FeedItem> items = new ArrayList<FeedItem>();

	public String getFeedTitle() {
		return feedTitle;
	}

	public String getFeedLink() {
		return feedLink;
	}

	public String getFeedDescription() {
		return feedDescription;
	}

	public FeedItem[] getFeedItems() {
		return items.toArray(new FeedItem[items.size()]);
	}

	public int getFeedItemsSize() {
		return items.size();
	}

	public void onFeedDescriptionLoad(String feedDescription) {
		this.feedDescription = feedDescription;
	}

	public void onFeedTitleLoad(String feedTitle) {
		this.feedTitle = feedTitle;
	}

	public void onFeedLinkLoad(String feedLink) {
		this.feedLink = feedLink;
	}

	public void onItemLoad(FeedItem item) throws SAXException {
		items.add(item);
		if (items.size() >= MAX_SIZE) {
			throw new SAXException("OverSize!");
		}
	}
}
