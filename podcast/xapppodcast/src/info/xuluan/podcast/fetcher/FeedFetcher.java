package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.DownloadItemListener;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import android.util.Log;

public class FeedFetcher {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private int maxSize = 100 * 1024;
	private static final int TIMEOUT = 20*1000;
	private boolean canceled = false;

	/**
	 * Set proxy host and port.
	 * 
	 * @param host
	 *            Proxy host.
	 * @param port
	 *            Proxy port.
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
	 * @param url
	 *            The URL for discovery.
	 */
	public String[] discover(String url) {
		setCanceled(false);
		Response response = null;
		try {
			response = get(url, 0L, null);
		} catch (InterruptedException e) {
			return EMPTY_STRING_ARRAY;
		} catch (Exception e) {
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
		} catch (UnsupportedEncodingException e) {
			return EMPTY_STRING_ARRAY;
		}
		// search for:
		// <link href="http://xxx" title="xxx" type="application/rss+xml" ... />
		int start = 0;
		List<String> list = new ArrayList<String>(5);
		for (;;) {
			int pos = html.indexOf("<link ", start);
			if (pos == (-1))
				break;
			int end = html.indexOf(">", pos);
			if (end == (-1))
				break;
			String link = extractFeedUrl(html.substring(pos, end + 1), url);
			if (link != null)
				list.add(link);
			start = end + 1;
		}
		return list.toArray(new String[list.size()]);
	}

	String extractFeedUrl(String link, String baseUrl) {
		if (link.indexOf("rel='alternate'") == (-1)
				&& link.indexOf("rel=\"alternate\"") == (-1)) {
			return null;
		}
		if (link.indexOf("type='application/rss+xml'") == (-1)
				&& link.indexOf("type=\"application/rss+xml\"") == (-1)
				&& link.indexOf("type='application/atom+xml'") == (-1)
				&& link.indexOf("type=\"application/atom+xml\"") == (-1)) {
			return null;
		}
		// it is an XML feed:
		int pos = link.indexOf("href='");
		if (pos == (-1))
			pos = link.indexOf("href=\"");
		if (pos == (-1))
			return null;
		int end = link.indexOf('\'', pos + 6);
		if (end == (-1))
			end = link.indexOf('\"', pos + 6);
		if (end == (-1))
			return null;
		String href = link.substring(pos + 6, end);
		if (href.startsWith("http://") || href.startsWith("https://"))
			return href;
		return getAbsoluteUrl(baseUrl, href);
	}

	String getAbsoluteUrl(String baseUrl, String href) {
		if (href.startsWith("/")) {
			int n = baseUrl.indexOf('/', "https://".length());
			if (n == (-1))
				return baseUrl + href;
			return baseUrl.substring(0, n) + href;
		}
		int backs = 0;
		for (;;) {
			if (href.startsWith("../")) {
				backs++;
				href = href.substring(3);
			} else if (href.startsWith("./")) {
				href = href.substring(2);
			} else {
				break;
			}
		}
		baseUrl = trimWithSlash(baseUrl);
		for (int i = 0; i < backs; i++) {
			int last = baseUrl.lastIndexOf('/');
			if (last > 8)
				baseUrl = baseUrl.substring(0, last);
			else
				break;
		}
		baseUrl = trimWithSlash(baseUrl);
		return baseUrl + href;
	}

	String trimWithSlash(String url) {
		int n = url.lastIndexOf('/');
		if (n > 8)
			return url.substring(0, n + 1);
		return url.endsWith("/") ? url : url + "/";
	}

	/**
	 * Request to fetch the feed content.
	 * 
	 * @param feedUrl
	 *            The URL of feed.
	 */
	public void fetch(String feedUrl, FeedFetcherListener listener) {
		fetch(feedUrl, 0L, listener);
	}

	/**
	 * Request to fetch the feed content.
	 * 
	 * @param feedUrl
	 *            The URL of feed.
	 */
	public Response fetch(String feedUrl, long ifModifiedSince,
			FeedFetcherListener listener) {
		setCanceled(false);
		Response response = null;
		try {
			response = get(feedUrl, ifModifiedSince, listener);
		} catch (InterruptedException e) {
			return null;
		} catch (Exception e) {
			if (listener != null)
				listener.onException(e);
		}
		if (!isCanceled() && listener != null) {

			if (response == null)
				listener.onFetched(feedUrl, null, null);
			else
				listener.onFetched(feedUrl, response.charset, response.content);
		}

		return response;

	}

	Response get(String url, long ifModifiedSince, FeedFetcherListener listener)
			throws IOException, InterruptedException {
		URL u = new URL(url);
		InputStream input = null;
		ByteArrayOutputStream output = null;
		HttpURLConnection hc = null;
		HttpURLConnection.setFollowRedirects(true);
		try {
			hc = (HttpURLConnection) u.openConnection();
			if (ifModifiedSince > 0)
				hc.setIfModifiedSince(ifModifiedSince);
			hc.setRequestMethod("GET");
			hc.setUseCaches(false);
			hc.addRequestProperty("Accept", "*/*");
			hc
					.addRequestProperty("User-Agent",
							"Mozilla/4.0 (compatible; Windows XP 5.1; MSIE 6.0.2900.2180)");
			hc.addRequestProperty("Accept-Encoding", "gzip");
			hc.setReadTimeout(TIMEOUT);

			hc.connect();
			setProgress(-1, listener);
			int code = hc.getResponseCode();
			if (code == 304)
				return null;
			if (code != 200)
				throw new IOException("Connection failed: " + code);
			// detect content type and charset:
			String contentType = hc.getContentType();
			String charset = null;
			if (contentType != null) {
				int n = contentType.indexOf("charset=");
				if (n != (-1)) {
					charset = contentType.substring(n + 8).trim();
					contentType = contentType.substring(0, n).trim();
					if (contentType.endsWith(";")) {
						contentType = contentType.substring(0,
								contentType.length() - 1).trim();
					}
				}
			}
			boolean gzip = "gzip".equals(hc.getContentEncoding());
			input = gzip ? new GZIPInputStream(hc.getInputStream()) : hc
					.getInputStream();
			output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int total = 0;
			for (;;) {
				setProgress(-1, listener);
				int n = input.read(buffer);
				if (n == (-1))
					break;
				total += n;
				if (total > maxSize)
					break;
				// throw new IOException("Feed size is too large. More than " +
				// maxSize + " bytes.");

				output.write(buffer, 0, n);
			}
			output.close();
			Log.w("RSS", "download length = " + total);

			return new Response(contentType, charset, output.toByteArray());
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (hc != null) {
				hc.disconnect();
			}

		}
	}

	void setProgress(int progress, FeedFetcherListener listener)
			throws InterruptedException {
		if (listener != null)
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

	public static int download(FeedItem item,DownloadItemListener listener) {
		String pathname = item.pathname;

		int nStartPos = item.offset;

		RandomAccessFile oSavedFile = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(item.resource);
			Log.w("RSS", "url = " + url);
			oSavedFile = new RandomAccessFile(pathname, "rw");

			httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection.setReadTimeout(TIMEOUT);
			
			httpConnection.setRequestProperty("User-Agent", "Internet Explorer");
			if (item.offset != 0) {
				String sProperty = "bytes=" + item.offset + "-";
				httpConnection.setRequestProperty("RANGE", sProperty);
				System.out.println(sProperty);
				oSavedFile.seek(item.offset); 
			}
			
			int responseCode = httpConnection.getResponseCode();
				Log.w("RSS", "Error Code : " + responseCode);			
			if (responseCode >= 500) {
				item.offset = 0;
				throw new IOException("Error Code : " + responseCode);
			}else if (responseCode >= 400){
				throw new IOException("Error Code : " + responseCode);
			}
			
			long nEndPos = item.length;
			if (item.offset == 0) {
				
				nEndPos = httpConnection.getContentLength();
				if(nEndPos<0){
				Log.w("RSS", "Cannot get content length: " + nEndPos);			
				
					throw new IOException("Cannot get content length: " + nEndPos);
				}
				item.length = nEndPos;
			}
			Log.w("RSS", "nEndPos = " + nEndPos);

			input = httpConnection.getInputStream();
			int buff_size = 1024*4;
			byte[] b = new byte[buff_size];
			int nRead = 0;

			while ((nRead = input.read(b, 0, buff_size)) > 0 && nStartPos < nEndPos) {
				if(listener!=null)
					listener.onUpdate(item);
				oSavedFile.write(b, 0, nRead);
				nStartPos += nRead;
				item.offset = nStartPos;
			}
			if(nStartPos >= nEndPos)
				item.status = ItemColumns.ITEM_STATUS_NO_PLAY;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
			if(httpConnection!=null)
				httpConnection.disconnect();
			} catch (Exception e) {
			}
			
			try {
				if(input!=null)
					input.close();
			} catch (Exception e) {
			}

			try {
				if(oSavedFile!=null)
					oSavedFile.close();
			} catch (Exception e) {
			}
			
		}
		
		return 0;

	}


}
