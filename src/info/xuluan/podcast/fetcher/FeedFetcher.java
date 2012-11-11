package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class FeedFetcher {

	private static final int maxSize = 1000 * 1024;
	private static final int TIMEOUT = 10 * 1000;
	private volatile boolean canceled = false;
	private volatile Thread downloadThread = null;
	private volatile HttpURLConnection m_httpConnection = null;
	private volatile String downloadingStatus = "";
	private String mAgent;
	protected final Log log = Log.getLog(getClass());

    private static final int[] mp3Sig = { 0x49, 0x44, 0x33};
    
    private boolean isAudioFile(byte[] buffer, int size, String type) {  
        //if not mp3 , skip     
        if(!(type.equals("audio/mpeg") || type.equals(""))) return true;
    
        if(ismp3File(buffer, size)){
            return true;
        }
        //Not the standard signature, do a simpler check for frame sync
        if (size>2 && buffer[0]==(byte)0xFF && (buffer[1]&0x00E0)==0x00E0)
        	return true;
        
        log.debug("audio file type '"+type+"' is not recognized");
        return false;
    }
    
    private static boolean ismp3File(byte[] buffer, int size) {
        return matchesSignature(mp3Sig, buffer, size);
    }    
    
    private static boolean matchesSignature(int[] signature, byte[] buffer, int size) {
        if (size < signature.length) {
            return false;
        }

        boolean b = true;
        for (int i = 0; i < signature.length; i++) {
            if (signature[i] != (0x00ff & buffer[i])) {
                b = false;
                break;
            }
        }
        
        return b;
    }   

	public FeedFetcher() {
		mAgent = "Mozilla/4.0 (compatible; Windows XP 5.1; MSIE 6.0.2900.2180)";
	}
	
	public FeedFetcher(String agent) {
		mAgent = agent;
	}
	
	
	public void setProxy(String host, int port) {
		Properties props = System.getProperties();
		props.put("proxySet", "true");
		props.put("proxyHost", host);
		props.put("proxyPort", String.valueOf(port));
	}

	public Response fetch(String feedUrl) {
		return fetch(feedUrl, 0L);
	}

	public Response fetch(String feedUrl, long ifModifiedSince) {
		canceled = false;
		Response response = null;
		try {
			response = get(feedUrl, ifModifiedSince);
		} catch (InterruptedException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!canceled) {

		}

		return response;

	}


	Response get(String url, long ifModifiedSince) throws IOException,
			InterruptedException {
		log.debug("Retrieving file "+url);
		URL u = new URL(url);
		InputStream input = null;
		ByteArrayOutputStream output = null;
		HttpURLConnection hc = null;
		HttpURLConnection.setFollowRedirects(true);

		int total = 0;
		
		try {
			hc = (HttpURLConnection) u.openConnection();
			if (ifModifiedSince > 0)
				hc.setIfModifiedSince(ifModifiedSince);
			hc.setRequestMethod("GET");
			hc.setUseCaches(false);
			hc.addRequestProperty("Accept", "*/*");
			hc.addRequestProperty("User-Agent", mAgent);
			hc.addRequestProperty("Accept-Encoding", "gzip");
			hc.setReadTimeout(TIMEOUT);
			hc.setConnectTimeout(TIMEOUT);

			hc.connect();
			int code = hc.getResponseCode();
			log.debug("Connection response code is "+code);
			if (code == 304)
				return null;
			if (code != 200)
				throw new IOException("Connection failed: " + code);

			boolean gzip = "gzip".equals(hc.getContentEncoding());
			input = gzip ? new GZIPInputStream(hc.getInputStream()) : hc
					.getInputStream();
			output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			for (;;) {
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
			log.debug("download length = " + total);

			return new Response(output.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
			log.debug("download length = " + total);

			return null;

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

	/**
	 * Stop downloading.
	 */
	public synchronized void cancel() {
		log.debug("FeedFetcher.cancel");
		canceled = true;
		//give the thread a bit of time to finish, as long as it's not blocked
		try { Thread.sleep(250); } catch (InterruptedException ex) {}
		Thread th = downloadThread;
		if (th != null) {
			log.debug("sending interrupt to download thread");
			th.interrupt();	//in case it is hung on blocked I/O
			m_httpConnection.disconnect();
		}
		log.debug("FeedFetcher.cancel done");
	}
	
	public String getDownloadingStatus() {
		return downloadingStatus;
	}
	
	public int download(FeedItem item) {
		canceled = false;
		String pathname = item.pathname;

		int nStartPos = item.offset;

		RandomAccessFile oSavedFile = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			downloadThread = Thread.currentThread();
			URL url = new URL(item.resource);
			log.debug("url = " + url);

			//If we have an offset already, make sure the file actually exists
			//(we could also check the file size if we want to take the next step).
			if (item.offset>0 && !((new File(pathname).exists())))
				item.offset = 0;
			oSavedFile = new RandomAccessFile(pathname, "rw");

			downloadingStatus = "Connecting";
			//httpConnection = (HttpURLConnection) url.openConnection();
			httpConnection = openUrl(url);
			m_httpConnection = httpConnection;
			httpConnection.setReadTimeout(TIMEOUT);
			httpConnection.setConnectTimeout(TIMEOUT);
			httpConnection
					.setRequestProperty("User-Agent", "Internet Explorer");
			if (item.offset != 0) {
				String sProperty = "bytes=" + item.offset + "-";
				httpConnection.setRequestProperty("RANGE", sProperty);
				System.out.println(sProperty);
				oSavedFile.seek(item.offset);
			}

			downloadingStatus = "Requesting";
			log.debug("FeedFetcher.download A");
			//int responseCode = httpConnection.getResponseCode();
			int responseCode = getResponseCode(httpConnection);
			downloadingStatus = "";
			log.debug("FeedFetcher.download B");
			log.debug("Error Code : " + responseCode);
			if (responseCode >= 500) {
				item.offset = 0;
				throw new IOException("Error Code : " + responseCode);
			} else if (responseCode >= 400) {
				throw new IOException("Error Code : " + responseCode);
			}

			long nEndPos = item.length;
			if (item.offset == 0) {

				nEndPos = httpConnection.getContentLength();
				if (nEndPos < 0) {
					log.warn("Cannot get content length: " + nEndPos);

					throw new IOException("Cannot get content length: "
							+ nEndPos);
				}
				item.length = nEndPos;
			}
			log.debug("nEndPos = " + nEndPos);

			input = httpConnection.getInputStream();
			int buff_size = 1024 * 4;
			byte[] b = new byte[buff_size];
			int nRead = 0;

			downloadingStatus = "Downloading";
			while (!canceled &&
					(nRead = input.read(b, 0, buff_size)) > 0
					&& nStartPos < nEndPos) {
                                if(item.offset == 0) {
                                    if(!isAudioFile(b, nRead, item.type)){
                                        log.debug("giving up on non-audio file");
                                        break;
                                    }
                                }
				oSavedFile.write(b, 0, nRead);
				nStartPos += nRead;
				item.offset = nStartPos;
			}
			if (nStartPos >= nEndPos)
				item.downloadSuccess();
			else if (canceled)
				item.status = ItemColumns.ITEM_STATUS_DOWNLOAD_PAUSE;
		} catch (InterruptedException ex) {
			log.debug("Download thread interrupted");
		} catch (Exception e) {
			log.debug("Caught exception in FeedFetcher.download:");
			e.printStackTrace();
		} finally {
			downloadingStatus = "";
			log.debug("FeedFetcher.download C");
			downloadThread = null;

			try {
				if (httpConnection != null)
					httpConnection.disconnect();
			} catch (Exception e) {
			}

			try {
				if (input != null)
					input.close();
			} catch (Exception e) {
			}

			try {
				if (oSavedFile != null)
					oSavedFile.close();
			} catch (Exception e) {
			}
		}

		return 0;

	}

	//Make the connection call in another thread so our thread can be interrupted
	private HttpURLConnection openUrl(URL url) throws InterruptedException {
		UrlConnectionOpener opener = new UrlConnectionOpener(url);
		opener.start();
		opener.join();
		return opener.getConn();
	}

	//new UrlConnectionOpener(url).open();
	private class UrlConnectionOpener extends Thread {		
		URL url;
		URLConnection conn;
		UrlConnectionOpener(URL url) {
			this.url = url;
		}
		@Override
		public void run() {
			try {
				conn = url.openConnection();
			} catch (Exception ex) {
				log.debug("Exception in UrlConnectionOpener thread:");
				ex.printStackTrace();
			}
		}
		public HttpURLConnection getConn() {
			return (HttpURLConnection)conn;
		}
	}

	//Make the connection call in another thread so our thread can be interrupted
	private int getResponseCode(HttpURLConnection conn) throws InterruptedException {
		ResponseCodeGetter rcg = new ResponseCodeGetter(conn);
		log.debug("getResponseCode A");
		log.debug("Current thread is "+Thread.currentThread());
		rcg.start();
		log.debug("getResponseCode B");
		rcg.join();
		log.debug("getResponseCode C");
		return rcg.getResponseCode();
	}

	//new UrlConnectionOpener(url).open();
	private class ResponseCodeGetter extends Thread {		
		HttpURLConnection conn;
		int responseCode;
		ResponseCodeGetter(HttpURLConnection conn) {
			this.conn = conn;
		}
		@Override
		public void run() {
			try {
				responseCode = conn.getResponseCode();
			} catch (Exception ex) {
				log.debug("Exception in ResponseCodeGetter thread:");
				ex.printStackTrace();
			}
		}
		public int getResponseCode() {
			return responseCode;
		}
	}
}
