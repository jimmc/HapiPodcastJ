package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.utils.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.UnsupportedEncodingException;

public class Response {
	private final Log log = Log.getLog(getClass());

	public final byte[] content;

	public Response(byte[] content) {
		this.content = content;
	}
	
	public String getCharset(){
		if(content==null)
			return "ISO-8859-1";
		
		Pattern p = Pattern.compile("\\<\\?xml.*?encoding\\=\"(.*?)\"\\s*\\?\\>"
				,Pattern.CASE_INSENSITIVE + Pattern.MULTILINE);
		int length = content.length>100?100:content.length;
		String line = new String(content, 0, length);		
		Matcher m = p.matcher(line);
		
		if (m.find()){
			log.debug("Charset = " + m.group(1));
			return m.group(1);
		}
		return "ISO-8859-1";
	}
	
	public String getContentAsString() throws UnsupportedEncodingException {
		
		//String enc = charset == null ? "ISO-8859-1" : charset;
		String charset = getCharset();
		String text;

		try {
			text = new String(content, charset);
		}catch (Exception e) {
			text = new String(content, "UTF-8");
		} 

		return text;
	}

}
