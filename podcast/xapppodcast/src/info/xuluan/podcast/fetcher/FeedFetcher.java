package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.provider.Item;
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

	public static int download(Item item) {
		String sURL = item.res;
		String pathname = item.pathname;

		int nStartPos = item.offset;
		int nRead = 0;

		Log.w("RSS", "sURL = " + sURL);

		RandomAccessFile oSavedFile = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(sURL);
			Log.w("RSS", "url = " + url);

			httpConnection = (HttpURLConnection) url.openConnection();

			long nEndPos = 0;
			if (item.offset == 0) {
				
				nEndPos = getFileSize(sURL);
				if(nEndPos<0)
					return 0;
				item.length = nEndPos;
			}
			Log.w("RSS", "nEndPos = " + nEndPos);
			oSavedFile = new RandomAccessFile(pathname, "rw");

			httpConnection.setRequestProperty("User-Agent", "Internet Explorer");
			if (nStartPos != 0) {
				String sProperty = "bytes=" + nStartPos + "-";
				httpConnection.setRequestProperty("RANGE", sProperty);
				System.out.println(sProperty);
				oSavedFile.seek(nStartPos); 
			}
			input = httpConnection.getInputStream();
			byte[] b = new byte[4096];

			while ((nRead = input.read(b, 0, 4096)) > 0 && nStartPos < nEndPos) {
				oSavedFile.write(b, 0, nRead);
				nStartPos += nRead;
			}
			
			item.status = ItemColumns.ITEM_STATUS_DOWNLOADED;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpConnection.disconnect();
			
			try {
				input.close();
			} catch (IOException e) {
			}

			try {
				oSavedFile.close();
			} catch (IOException e) {
			}
			
		}
		
		return nStartPos - item.offset;

	}

	// 获得文件长度
	public static long getFileSize(String sURL) {
		int nFileLength = -1;
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(sURL);
			httpConnection = (HttpURLConnection) url
					.openConnection();
			httpConnection
					.setRequestProperty("User-Agent", "Internet Explorer");

			int responseCode = httpConnection.getResponseCode();
			if (responseCode >= 400) {
				System.err.println("Error Code : " + responseCode);
				return -2; // -2 represent access is error
			}
			String sHeader;
			for (int i = 1;; i++) {
				sHeader = httpConnection.getHeaderFieldKey(i);
				if (sHeader != null) {
					if (sHeader.equalsIgnoreCase("Content-Length")) {
						nFileLength = Integer.parseInt(httpConnection
								.getHeaderField(sHeader));
						break;
					}
				} else
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			httpConnection.disconnect();
		}
		System.out.println(nFileLength);
		return nFileLength;
	}
}
