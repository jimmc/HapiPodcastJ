package info.xuluan.xappstock.fetch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Discover and fetch XML feeds from URL.
 * 
 * @author Michael Liao (askxuefeng@gmail.com)
 */
public class FeedFetcher {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private int maxSize = 4 * 1024 * 1024;
    private boolean canceled = false;

    /**
     * Set proxy host and port.
     * 
     * @param host Proxy host.
     * @param port Proxy port.
     */
    public void setProxy(String host, int port) {
        Properties props = System.getProperties();
        props.put("proxySet", "true");
        props.put("proxyHost", host);
        props.put("proxyPort", String.valueOf(port));
    }

    /**
     * Discover feeds from URL.
     * 
     * @param url The URL for discovery.
     */
    public String[] discover(String url) {
        setCanceled(false);
        Response response = null;
        try {
            response = get(url, 0L, null);
        }
        catch (InterruptedException e) {
            return EMPTY_STRING_ARRAY;
        }
        catch (Exception e) {
            return EMPTY_STRING_ARRAY;
        }
        if (isCanceled())
            return EMPTY_STRING_ARRAY;
        if (response.isXml() || response.isPlain())
            return new String[] { url };
        if (!response.isHtml())
            return EMPTY_STRING_ARRAY;
        String html = null;
        try {
            html = response.getContentAsString();
        }
        catch (UnsupportedEncodingException e) {
            return EMPTY_STRING_ARRAY;
        }
        // search for:
        // <link href="http://xxx" title="xxx" type="application/rss+xml" ... />
        int start = 0;
        List<String> list = new ArrayList<String>(5);
        for (;;) {
            int pos = html.indexOf("<link ", start);
            if (pos==(-1))
                break;
            int end = html.indexOf(">", pos);
            if (end==(-1))
                break;
            String link = extractFeedUrl(html.substring(pos, end+1), url);
            if (link!=null)
                list.add(link);
            start = end + 1;
        }
        return list.toArray(new String[list.size()]);
    }

    String extractFeedUrl(String link, String baseUrl) {
        if (link.indexOf("rel='alternate'")==(-1)
                && link.indexOf("rel=\"alternate\"")==(-1))
        {
            return null;
        }
        if (link.indexOf("type='application/rss+xml'")==(-1)
                && link.indexOf("type=\"application/rss+xml\"")==(-1)
                && link.indexOf("type='application/atom+xml'")==(-1)
                && link.indexOf("type=\"application/atom+xml\"")==(-1))
        {
            return null;
        }
        // it is an XML feed:
        int pos = link.indexOf("href='");
        if (pos==(-1))
            pos = link.indexOf("href=\"");
        if (pos==(-1))
            return null;
        int end = link.indexOf('\'', pos+6);
        if (end==(-1))
            end = link.indexOf('\"', pos+6);
        if (end==(-1))
            return null;
        String href = link.substring(pos+6, end);
        if (href.startsWith("http://") || href.startsWith("https://"))
            return href;
        return getAbsoluteUrl(baseUrl, href);
    }

    String getAbsoluteUrl(String baseUrl, String href) {
        if (href.startsWith("/")) {
            int n = baseUrl.indexOf('/', "https://".length());
            if (n==(-1))
                return baseUrl + href;
            return baseUrl.substring(0, n) + href;
        }
        int backs = 0;
        for (;;) {
            if (href.startsWith("../")) {
                backs++;
                href = href.substring(3);
            }
            else if (href.startsWith("./")) {
                href = href.substring(2);
            }
            else {
                break;
            }
        }
        baseUrl = trimWithSlash(baseUrl);
        for (int i=0; i<backs; i++) {
            int last = baseUrl.lastIndexOf('/');
            if (last>8)
                baseUrl = baseUrl.substring(0, last);
            else
                break;
        }
        baseUrl = trimWithSlash(baseUrl);
        return baseUrl + href;
    }

    String trimWithSlash(String url) {
        int n = url.lastIndexOf('/');
        if (n>8)
            return url.substring(0, n+1);
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Request to fetch the feed content.
     * 
     * @param feedUrl The URL of feed.
     */
    public String fetch(String feedUrl, FeedFetcherListener listener) {
        return fetch(feedUrl, 0L, listener);
    }

    /**
     * Request to fetch the feed content.
     * 
     * @param feedUrl The URL of feed.
     */
    public String fetch(String feedUrl, long ifModifiedSince, FeedFetcherListener listener) {
        setCanceled(false);
        Response response = null;
        try {
            response = get(feedUrl, ifModifiedSince, listener);
        }
        catch (InterruptedException e) {
            return null;
        }
        catch (Exception e) {
            listener.onException(e);
        }
        if (!isCanceled()) {
            if (response==null)
                listener.onFetched(feedUrl, null, null);
            else
                listener.onFetched(feedUrl, response.charset, response.content);
        }
		if(response==null){
			return null;
		}
		
		try {
			return response.getContentAsString();
		} catch (UnsupportedEncodingException e) {

			return null;
		}
		
		
    }

    Response get(String url, long ifModifiedSince, FeedFetcherListener listener) throws IOException, InterruptedException {
        URL u = new URL(url);
        InputStream input = null;
        ByteArrayOutputStream output = null;
        HttpURLConnection hc = null;
        HttpURLConnection.setFollowRedirects(true);
        try {
            hc = (HttpURLConnection) u.openConnection();
            if (ifModifiedSince>0)
                hc.setIfModifiedSince(ifModifiedSince);
            hc.setRequestMethod("GET");
            hc.setUseCaches(false);
            hc.addRequestProperty("Accept", "*/*");
            hc.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; Windows XP 5.1; MSIE 6.0.2900.2180)");
            hc.addRequestProperty("Accept-Encoding", "gzip");
            hc.connect();
            setProgress(-1, listener);
            int code = hc.getResponseCode();
            if (code==304)
                return null;
            if (code!=200)
                throw new IOException("Connection failed: " + code);
            // detect content type and charset:
            String contentType = hc.getContentType();
            String charset = null;
            if (contentType!=null) {
                int n = contentType.indexOf("charset=");
                if (n!=(-1)) {
                    charset = contentType.substring(n + 8).trim();
                    contentType = contentType.substring(0, n).trim();
                    if (contentType.endsWith(";")) {
                        contentType = contentType.substring(0, contentType.length()-1).trim();
                    }
                }
            }
            boolean gzip = "gzip".equals(hc.getContentEncoding());
            input = gzip ? new GZIPInputStream(hc.getInputStream()) : hc.getInputStream();
            output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int total = 0;
            for (;;) {
                setProgress(-1, listener);
                int n = input.read(buffer);
                if (n==(-1))
                    break;
                total += n;
                if (total>maxSize)
                    throw new IOException("Feed size is too large. More than " + maxSize + " bytes.");
                output.write(buffer, 0, n);
            }
            output.close();
            return new Response(contentType, charset, output.toByteArray());
        }
        finally {
            if (input!=null) {
                try {
                    input.close();
                }
                catch (IOException e) {}
            }
            if (hc!=null) {
                hc.disconnect();
            }
        }
    }

    void setProgress(int progress, FeedFetcherListener listener) throws InterruptedException {
        if (listener!=null)
            if (!listener.onProgress(progress))
                throw new InterruptedException();
    }

    /**
     * Get if operation is marked as canceled.
     */
    public synchronized boolean isCanceled() {
        return this.canceled;
    }

    /**
     * Set operation canceled.
     */
    public synchronized void cancel() {
        setCanceled(true);
    }

    synchronized void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
