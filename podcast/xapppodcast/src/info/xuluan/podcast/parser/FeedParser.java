package info.xuluan.podcast.parser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class FeedParser {

	static final FeedParser instance = new FeedParser();

	public static FeedParser getDefault() {
		return instance;
	}

	SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

	public FeedParser() {
		//
	}

	public void parse(InputStream input, FeedParserListener listener) {
		FeedParserHandler handler = new FeedParserHandler(listener);
		try {
			SAXParser parser = saxParserFactory.newSAXParser();
			parser.parse(input, handler);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
