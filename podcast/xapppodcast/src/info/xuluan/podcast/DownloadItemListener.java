package info.xuluan.podcast;

import info.xuluan.podcast.provider.FeedItem;



public interface DownloadItemListener {
	
    public void onBegin(FeedItem item);	

    public void onUpdate(FeedItem item);

    public void onFinish();
    

}
