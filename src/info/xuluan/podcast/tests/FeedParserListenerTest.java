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

import junit.framework.TestCase;
import info.xuluan.podcast.parser.FeedParserListener;
import info.xuluan.podcast.provider.FeedItem;
import info.xuluan.podcast.provider.Subscription;

/**
 * An example of a true unit test that tests the utility class {@link MorseCodeConverter}.
 * Since this test doesn't need a {@link android.content.Context}, or any other
 * dependencies injected, it simply extends the standard {@link TestCase}.
 *
 * See {@link com.example.android.apis.AllTests} for documentation on running
 * all tests and individual tests in this application.
 */
public class FeedParserListenerTest extends TestCase {

    public void testSortASC() throws Exception {
    	FeedParserListener listener = new FeedParserListener(10);
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.date = "Fri, 30 Jul 2000 00:00:00 PST";
    	listener.onItemLoad(item);
    	item = new FeedItem();
    	item.id = 2;
    	item.date = "Fri, 30 Jun 2010 00:00:00 PST";
    	listener.onItemLoad(item);    	
    	item = new FeedItem();
    	item.id = 3;
    	item.date = "Fri, 10 Jul 2010 00:00:00 PST";
    	listener.onItemLoad(item);    	    	
    	item = new FeedItem();
    	item.id = 4; 
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";
    	listener.onItemLoad(item);   
    	
        assertTrue(listener.getFeedItemsSize()==4);
        FeedItem[] items = listener.getSortItems();
        assertTrue(items.length==4);
       
        assertTrue(items[0].id == 1 );
        assertTrue(items[1].id == 2 );
        assertTrue(items[2].id == 3 );
        assertTrue(items[3].id == 4 );        
   	
    }

    public void testSortDESC() throws Exception {
    	FeedParserListener listener = new FeedParserListener(10);
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	

    	listener.onItemLoad(item);
    	item = new FeedItem();
    	item.id = 2;
    	item.date = "Fri, 10 Jul 2010 00:00:00 PST";    	
    	listener.onItemLoad(item);    	
    	item = new FeedItem();
    	item.id = 3;
    	item.date = "Fri, 30 Jun 2010 00:00:00 PST";
    	listener.onItemLoad(item);    	    	
    	item = new FeedItem();
    	item.id = 4; 
    	item.date = "Fri, 30 Jul 2000 00:00:00 PST";
    	listener.onItemLoad(item);   
    	
        assertTrue(listener.getFeedItemsSize()==4);
        FeedItem[] items = listener.getSortItems();
        assertTrue(items.length==4);
       
        assertTrue(items[0].id == 4 );
        assertTrue(items[1].id == 3 );
        assertTrue(items[2].id == 2 );
        assertTrue(items[3].id == 1 );        
   	
    }    
    
    public void testSort() throws Exception {
    	FeedParserListener listener = new FeedParserListener(10);
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.date = "Fri, 10 Jul 2010 00:00:00 PST";        	

    	listener.onItemLoad(item);
    	item = new FeedItem();
    	item.id = 2;
    	item.date = "Fri, 30 Jul 2000 00:00:00 PST";    	
	
    	listener.onItemLoad(item);    	
    	item = new FeedItem();
    	item.id = 3;
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	
    	listener.onItemLoad(item);    	    	
    	item = new FeedItem();
    	item.id = 4; 
    	item.date = "Fri, 30 Jun 2010 00:00:00 PST";
    
    	listener.onItemLoad(item);   
    	
        assertTrue(listener.getFeedItemsSize()==4);
        FeedItem[] items = listener.getSortItems();
        assertTrue(items.length==4);
       
        assertTrue(items[0].id == 2 );
        assertTrue(items[1].id == 4 );
        assertTrue(items[2].id == 1 );
        assertTrue(items[3].id == 3 );        
   	
    }        
    
    public void testSortEqual() throws Exception {
    	FeedParserListener listener = new FeedParserListener(10);
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.title = "1";
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	

    	listener.onItemLoad(item);
    	item = new FeedItem();
    	item.id = 2;
    	item.title = "2";    	
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	
    	listener.onItemLoad(item);
    	
        assertTrue(listener.getFeedItemsSize()==2);
        FeedItem[] items = listener.getSortItems();
        assertTrue(items.length==2);
       
        assertTrue(items[0].id == 1 );
        assertTrue(items[1].id == 2 );
   
   	
    }  
    
    public void testSortOnly3() throws Exception {
    	FeedParserListener listener = new FeedParserListener(3);
    	FeedItem item = new FeedItem();
    	item.id = 1;
    	item.date = "Fri, 30 Jul 2010 00:00:00 PST";    	

    	listener.onItemLoad(item);
    	item = new FeedItem();
    	item.id = 2;
    	item.date = "Fri, 10 Jul 2010 00:00:00 PST";    	
    	listener.onItemLoad(item);    	
    	item = new FeedItem();
    	item.id = 3;
    	item.date = "Fri, 30 Jun 2010 00:00:00 PST";
    	listener.onItemLoad(item);    	    	
    	item = new FeedItem();
    	item.id = 4; 
    	item.date = "Fri, 30 Jul 2000 00:00:00 PST";
    	listener.onItemLoad(item);   
    	
        assertTrue(listener.getFeedItemsSize()==4);
        FeedItem[] items = listener.getSortItems();
        assertTrue(items.length==3);
       
        assertTrue(items[0].id == 3 );
        assertTrue(items[1].id == 2 );
        assertTrue(items[2].id == 1 );        
   	
    }     
}
