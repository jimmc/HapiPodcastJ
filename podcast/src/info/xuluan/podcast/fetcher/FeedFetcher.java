package info.xuluan.podcast.fetcher;

import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.ItemColumns;
import info.xuluan.podcast.utils.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

public class FeedFetcher {

	private static final int maxSize = 100 * 1024;
	private static final int TIMEOUT = 10 * 1000;
	private boolean canceled = false;
	private String mAgent;
	protected final Log log = Log.getLog(getClass());

    private static final int[] mp3Sig = { 0x49, 0x44, 0x33};
    
    private boolean isAudioFile(byte[] buffer, int size, String type) {  
        //if not mp3 , skip     
        if(!(type.equals("audio/mpeg") || type.equals(""))) return true;
    
        if(ismp3File(buffer, size)){
            return true;
        }
        
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
		setCanceled(false);
		Response response = null;
		try {
			response = get(feedUrl, ifModifiedSince);
		} catch (InterruptedException e) {
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!isCanceled()) {

		}

		return response;

	}

	
	
	Response get(String url, long ifModifiedSince) throws IOException,
			InterruptedException {
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
			hc
					.addRequestProperty("User-Agent",
							mAgent);
			hc.addRequestProperty("Accept-Encoding", "gzip");
			hc.setReadTimeout(TIMEOUT);
			hc.setConnectTimeout(TIMEOUT);

			hc.connect();
			int code = hc.getResponseCode();
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
	
	

	public int download(FeedItem item) {
		String pathname = item.pathname;

		int nStartPos = item.offset;

		RandomAccessFile oSavedFile = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			URL url = new URL(item.resource);
			log.debug("url = " + url);
			oSavedFile = new RandomAccessFile(pathname, "rw");

			httpConnection = (HttpURLConnection) url.openConnection();
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

			int responseCode = httpConnection.getResponseCode();
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

			while ((nRead = input.read(b, 0, buff_size)) > 0
					&& nStartPos < nEndPos) {
                                if(item.offset == 0) {
                                    if(!isAudioFile(b, nRead, item.type)){
                                        log.debug(" using public wifi!!! ");
                                        break;
                                    }
                                }
				oSavedFile.write(b, 0, nRead);
				nStartPos += nRead;
				item.offset = nStartPos;
			}
			if (nStartPos >= nEndPos)
				item.downloadSuccess();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

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

}
