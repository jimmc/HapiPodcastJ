package info.xuluan.podcast.fetcher;

import java.util.List;

/**
 * Listener for fetching feed.
 * 
 * @author Michael Liao (askxuefeng@gmail.com)
 */
public interface FeedFetcherListener {

    /**
     * Fetching on progress.
     * 
     * @param percent Integer number between 0 to 100, or (-1) if cannot determin 
     *                the progress.
     * @return True if continue to fetch, false to cancel fetch.
     */
    boolean onProgress(int percent);

    /**
     * When feeds discovered.
     * 
     * @param url URL that was scanned.
     * @param feeds Feeds discovered.
     */
    void onDiscovered(String url, List<String> feeds);

    /**
     * When feed was fetched.
     * 
     * @param feedUrl URL that was fetched.
     * @param charset Character encoding, or null if cannot determin.
     * @param content Feed content as byte array, or null if response '302 not modified'.
     */
    void onFetched(String feedUrl, String charset, byte[] content);

    /**
     * When exception occurred.
     */
    void onException(Exception e);
}
