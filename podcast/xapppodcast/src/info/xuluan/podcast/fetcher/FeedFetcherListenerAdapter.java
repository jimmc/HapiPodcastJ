package info.xuluan.podcast.fetcher;

import java.util.List;

public class FeedFetcherListenerAdapter implements FeedFetcherListener {

    public void onDiscovered(String url, List<String> feeds) {
    }

    public void onException(Exception e) {
    }

    public void onFetched(String feedUrl, String charset, byte[] content) {
    }

    public boolean onProgress(int percent) {
        return true;
    }

}
