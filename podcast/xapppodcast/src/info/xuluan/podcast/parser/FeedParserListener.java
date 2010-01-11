package info.xuluan.podcast.parser;

import org.xml.sax.SAXException;


public interface FeedParserListener {

    void onFeedTitleLoad(String feedTitle);

    void onFeedDescriptionLoad(String feedDescription);
    
    void onFeedLinkLoad(String feedLink);

    void onItemLoad(FeedItem item) throws SAXException;
}
