package info.xuluan.podcast.fetcher;

import java.io.UnsupportedEncodingException;

public class Response {

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
		return new String(content, enc);
	}

}
