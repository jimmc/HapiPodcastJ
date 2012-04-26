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
import info.xuluan.podcast.utils.SDCardMgr;


public class SDCardTest extends TestCase {

    public void testAppDir() throws Exception {
        assertTrue(SDCardMgr.getAppDir().equals("/mnt/sdcard/xuluan.podcast"));
    }

    public void testDownloadDir() throws Exception {
        assertTrue(SDCardMgr.getDownloadDir().equals("/mnt/sdcard/xuluan.podcast/download"));
    }
    
    public void testSDCardStatus() throws Exception {
        assertTrue(SDCardMgr.getSDCardStatus());
    }    
}
