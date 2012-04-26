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

import info.xuluan.podcast.fetcher.Response;
import junit.framework.TestCase;



public class ResponseTest extends TestCase {

    public void testGetCharset_ISO_8859_1() throws Exception {
    	String s = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("ISO-8859-1"));
    
    }

    public void testGetCharset_UTF_8() throws Exception {
    	String s = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("UTF-8"));
    
    }  
    
    public void testGetCharset_UTF_8_2() throws Exception {
    	String s = "               <?xml version=\"1.0\" encoding=\"UTF-8\"?>           ";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("UTF-8"));
    
    }  
    
    public void testGetCharset_ISO_8859_1_2() throws Exception {
    	String s = "    \n \n            <?xml version = \"1.0\" encoding=\"ISO-8859-2\"  ?>           ";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("ISO-8859-2"));
    
    }     
    
    public void testGetCharset_none() throws Exception {
    	String s = "    \n \n            <?xml version = \"1.0\" ?>           ";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("ISO-8859-1"));
    
    }   
    
    public void testGetCharset_null() throws Exception {

    	Response res = new Response(null);
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("ISO-8859-1"));
    
    }   
    
    public void testGetCharset_unknown() throws Exception {
    	String s = "    \n \n            <?xml version = \"1.0\" encoding=\"abcdefghi1234567\"  ?>           ";
    	Response res = new Response(s.getBytes());
    	String charset = res.getCharset();
        assertTrue(charset.equalsIgnoreCase("abcdefghi1234567"));
    
    }      
}
