package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.utils.Log;

import java.io.UnsupportedEncodingException;

public class Response {
	private final Log log = Log.getLog(getClass());
	public final String contentType;
	public final String charset;
	public final byte[] content;

	public Response(String contentType, String charset, byte[] content) {
		this.contentType = contentType;
		this.charset = charset;
		this.content = content;
	}

	public String getContentAsString() throws UnsupportedEncodingException {
		
		String enc = charset == null ? "ISO-8859-1" : charset;
		
		//String enc = charset == null ? "US-ASCII" : charset;
		log.warn("charset: " + enc);
		log.warn("contentType: " + contentType);
		return new String(content, enc);
	}

}
