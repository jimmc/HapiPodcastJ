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

import android.content.Intent;
import android.test.ServiceTestCase;
import info.xuluan.podcast.R;
import info.xuluan.podcast.provider.Subscription;
import info.xuluan.podcast.service.PodcastService;

/**
 * An example of a true unit test that tests the utility class {@link MorseCodeConverter}.
 * Since this test doesn't need a {@link android.content.Context}, or any other
 * dependencies injected, it simply extends the standard {@link TestCase}.
 *
 * See {@link com.example.android.apis.AllTests} for documentation on running
 * all tests and individual tests in this application.
 */
public class FeedFetchTest extends android.test.ServiceTestCase<PodcastService> {

	PodcastService PodcastServiceInstance;
    public FeedFetchTest() {
		super(PodcastService.class);
		// TODO Auto-generated constructor stub
	}
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), PodcastService.class);
        startService(startIntent); 
        while(PodcastServiceInstance==null){
        	PodcastServiceInstance = getService();
        }

    }
    
    
	public void testFetchNetworkErr() throws Exception {
    	String url = "http://www.xxx.com/aaa/bbb";
    	PodcastServiceInstance.fetchFeed(url);
    	int code = PodcastServiceInstance.getErrCode();
        assertTrue(code==R.string.network_fail);

    }

	public void testFetchFormatErr() throws Exception {
    	String url = "http://www.baidu.com";
    	PodcastServiceInstance.fetchFeed(url);
    	int code = PodcastServiceInstance.getErrCode();
        assertTrue(code==R.string.feed_format_error);

    }	
	
	public void testFetchOK() throws Exception {
    	String url = "http://blog.sina.com.cn/rss/twocold.xml";
    	PodcastServiceInstance.fetchFeed(url);
    	int code = PodcastServiceInstance.getErrCode();
    	assertTrue(code==0);

    }	
}
