/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.xuluan.podcast.tests;

import android.test.mock.MockContentResolver;
import info.xuluan.podcastj.R;
import info.xuluan.podcast.parser.FeedHandler;
import info.xuluan.podcast.parser.FeedParserHandler;
import info.xuluan.podcast.parser.FeedParserListener;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.PodcastProvider;
import info.xuluan.podcast.provider.Subscription;


public class FeedHandlerTest extends android.test.ProviderTestCase<PodcastProvider> {

	MockContentResolver context;
	
    public FeedHandlerTest() {
		super(PodcastProvider.class, PodcastProvider.AUTHORITY);
		// TODO Auto-generated constructor stub
	}
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context  = getMockContentResolver();

    }   

 /*
	public void testFetchNetworkErr() throws Exception {
    	String url = "http://www.xxx.com/aaa/bbb";
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener result = handler.fetchFeed(url);
    	int code = result.resultCode;
        assertTrue(code==R.string.network_fail);

    }

	public void testFetchFormatErr() throws Exception {
    	String url = "http://www.baidu.com";
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener result = handler.fetchFeed(url);
    	int code = result.resultCode;
        assertTrue(code==R.string.feed_format_error);

    }	
	
	public void testFetchOK() throws Exception {
    	String url = "http://podcast.rthk.org.hk/podcast/observeworld.xml";
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener result = handler.fetchFeed(url);
    	int code = result.resultCode;
    	assertTrue(code==0);

    }	
*/
    public void testUpdateFail() throws Exception {
    	FeedHandler handler = new FeedHandler(context,10);
    	
    	Subscription sub = new Subscription();
    	sub.failCount = 0;
    	handler.updateFail(sub);
    	assertTrue(sub.failCount==1);
    	
    	sub.failCount = 3;
    	handler.updateFail(sub);
    	assertTrue(sub.failCount==0);   	
    }
    
    public void testUpdateFeedFirst() throws Exception {
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener listener = new FeedParserListener(10);
    	listener.onFeedTitleLoad("title");
    	listener.onFeedDescriptionLoad("description");
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.title = "1";
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	

    	listener.onItemLoad(item);
	
    	
    	Subscription sub = new Subscription();
    	sub.lastItemUpdated = 0 ;
    	int i = handler.updateFeed(sub, listener);
    	assertTrue(i==1);
    	
    	assertTrue(sub.lastItemUpdated == item.getDate());
    	assertTrue(sub.title.equalsIgnoreCase("title"));
    	assertTrue(sub.description.equalsIgnoreCase("description"));
	
    }    
    
    public void testUpdateFeedNone() throws Exception {
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener listener = new FeedParserListener(10);
    	listener.onFeedTitleLoad("title");
    	listener.onFeedDescriptionLoad("description");
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.title = "1";
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	


    	
    	Subscription sub = new Subscription();
    	sub.lastItemUpdated = item.getDate();
    	FeedItem item2 = new FeedItem();
    	item2.id = 2;
    	item2.title = "2";  	
    	item2.date = "Fri, 30 Jul 2000 00:00:00 PST";  
    	listener.onItemLoad(item2);
    	
    	int i = handler.updateFeed(sub, listener);
    	assertTrue(i==0);
    	
    	assertTrue(sub.lastItemUpdated == item.getDate());
    	assertTrue(sub.title.equalsIgnoreCase("title"));
    	assertTrue(sub.description.equalsIgnoreCase("description"));
	
    }  
    
    public void testUpdateFeed2() throws Exception {
    	FeedHandler handler = new FeedHandler(context,10);
    	FeedParserListener listener = new FeedParserListener(10);
    	listener.onFeedTitleLoad("title");
    	listener.onFeedDescriptionLoad("description");
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.title = "1";
    	item.date = "Fri, 30 Jun 2010 00:00:00 PST";    	


    	
    	Subscription sub = new Subscription();
    	sub.lastItemUpdated = item.getDate();
    	FeedItem item2 = new FeedItem();
    	item2.id = 2;
    	item2.title = "2";  	
    	item2.date = "Fri, 30 Jul 2010 00:00:00 PST";  
    	listener.onItemLoad(item2);
    	
    	int i = handler.updateFeed(sub, listener);
    	
    	assertTrue(sub.lastItemUpdated == item2.getDate());
    	assertTrue(i==1);
    	
    	assertTrue(sub.title.equalsIgnoreCase("title"));
    	assertTrue(sub.description.equalsIgnoreCase("description"));
	
    }     


    public void testStrip() throws Exception {
    	FeedParserHandler handler = new FeedParserHandler(null,0);

    	String str= "\n                    abc\n                ";
    	
    	String str2 = handler.strip(str);
    	assertTrue(str2.equals("abc"));
    	
    }
}
