/*
 * Copyright (C) 2007 The Android Open Source Project
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

package info.xuluan.podcast;

import info.xuluan.podcast.service.PodcastService;
import info.xuluan.podcastj.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;

public class Pref extends HapiPreferenceActivity {

	public static final String HAPI_PREFS_FILE_NAME = "info.xuluan.podcastj_preferences";
		//Default filename is our package name (see manifest) with _preferences appended
	private static final int DAYS_PER_WEEK = 7;
	private static final int DAYS_PER_MONTH = 30;
	private static final int DAYS_PER_YEAR = 365;
	private static final int HOURS_PER_DAY = 24;
	private static final int HOURS_PER_WEEK = HOURS_PER_DAY * DAYS_PER_WEEK;
	private static final int HOURS_PER_MONTH = HOURS_PER_DAY * DAYS_PER_MONTH;
	private static final int HOURS_PER_YEAR = HOURS_PER_DAY * DAYS_PER_YEAR;
	private static final int MINUTES_PER_HOUR = 60;
	private static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
	private static final int MINUTES_PER_WEEK = MINUTES_PER_HOUR * HOURS_PER_WEEK;
	
	private PodcastService serviceBinder = null;
	ComponentName service = null;
	private SummaryUpdater summaryUpdater = new SummaryUpdater();

	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			serviceBinder = ((PodcastService.PodcastBinder) service)
					.getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			serviceBinder = null;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);

		service = startService(new Intent(this, PodcastService.class));

		Intent bindIntent = new Intent(this, PodcastService.class);
		bindService(bindIntent, serviceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
		prefs.registerOnSharedPreferenceChangeListener(summaryUpdater);
		summaryUpdater.updateAllSummaries(prefs);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(serviceBinder!=null)
			serviceBinder.updateSetting();
	    getPreferenceScreen().getSharedPreferences()
            .unregisterOnSharedPreferenceChangeListener(summaryUpdater);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(serviceConnection);
		// stopService(new Intent(this, service.getClass()));
	}

	class SummaryUpdater implements OnSharedPreferenceChangeListener {
		public void updateAllSummaries(SharedPreferences prefs) {
			String[] keys = {
					"pref_update_wifi", "pref_update_mobile", "pref_download_only_wifi",
					"pref_max_new_items",
					"pref_item_expire", "pref_download_file_expire", "pref_played_file_expire",
					"pref_rewind_interval", "pref_fast_forward_interval"
			};
			for (String key : keys) {
				onSharedPreferenceChanged(prefs, key);
			}
		}
		
	    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
	    	if (key.equals("pref_download_only_wifi"))
	    		updateDisableMobile(prefs,key);
	        if (key.equals("pref_update_wifi"))
	        	updateMinuteIntervalSummary(prefs, key);
	        else if (key.equals("pref_update_mobile"))
	        	updateMinuteIntervalSummary(prefs, key);
	        else if (key.equals("pref_max_new_items"))
	        	updateMaxItems(prefs, key);
	        else if (key.equals("pref_item_expire"))
	        	updateExpireDaysSummary(prefs, key);
	        else if (key.equals("pref_download_file_expire"))
	        	updateExpireDaysSummary(prefs, key);
	        else if (key.equals("pref_played_file_expire"))
	        	updateExpireHoursSummary(prefs, key);
	        else if (key.equals("pref_rewind_interval"))
	        	updatePlayerIntervalSummary(prefs, key, "Rewind");
	        else if (key.equals("pref_fast_forward_interval"))
	        	updatePlayerIntervalSummary(prefs, key, "Fast-forward");
	    }

	    private void updateDisableMobile(SharedPreferences prefs, String key) {
	    	Boolean disabled = prefs.getBoolean(key, false);
	    	Preference mobileIntervalPref = findPreference("pref_update_mobile");
	    	mobileIntervalPref.setEnabled(!disabled);
	    	if (disabled)
	    		mobileIntervalPref.setSummary("To set, enable Mobile update");
	    	else
	    		onSharedPreferenceChanged(prefs, "pref_update_mobile");
	    }
	    
	    private void updateMaxItems(SharedPreferences prefs, String key) {
	    	Preference pref = findPreference(key);
	    	String value = prefs.getString(key, "");
	    	pref.setSummary("Download up to "+value+" new items");
	    }
	    
	    private void updateExpireDaysSummary(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            String value = prefs.getString(key,"");
            int days = Integer.parseInt(value);
            String s;
            if ((days%DAYS_PER_YEAR)==0)
            	s = maybePlural(days/DAYS_PER_YEAR,"year","years");
            else if ((days%DAYS_PER_MONTH)==0)
            	s = maybePlural(days/DAYS_PER_MONTH,"month","months");
            else if ((days%DAYS_PER_WEEK)==0)
            	s = maybePlural(days/DAYS_PER_WEEK,"week","weeks");
            else
            	s = maybePlural(days,"day","days");
            pref.setSummary("Expire after "+s);	    	
	    }
	    
	    private void updateExpireHoursSummary(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            String value = prefs.getString(key,"");
            int hours = Integer.parseInt(value);
            String s;
            if ((hours%HOURS_PER_YEAR)==0)
            	s = maybePlural(hours/HOURS_PER_YEAR,"year","years");
            else if ((hours%HOURS_PER_MONTH)==0)
            	s = maybePlural(hours/HOURS_PER_MONTH,"month","months");
            else if ((hours%HOURS_PER_WEEK)==0)
            	s = maybePlural(hours/HOURS_PER_WEEK,"week","weeks");
            else if ((hours%HOURS_PER_DAY)==0)
            	s = maybePlural(hours/HOURS_PER_DAY,"day","days");
            else
            	s = maybePlural(hours,"hour","hours");
            pref.setSummary("Expire after "+s);	    	
	    }
	    
	    private void updateMinuteIntervalSummary(SharedPreferences prefs, String key) {
            Preference pref = findPreference(key);
            String value = prefs.getString(key,"");
            int minutes = Integer.parseInt(value);
            String s;
            if ((minutes%MINUTES_PER_WEEK)==0)
            	s = maybePlural(minutes/MINUTES_PER_WEEK,"week","weeks");
            else if ((minutes%MINUTES_PER_DAY)==0)
            	s = maybePlural(minutes/MINUTES_PER_DAY,"day","days");
            else if ((minutes%MINUTES_PER_HOUR)==0)
            	s = maybePlural(minutes/MINUTES_PER_HOUR,"hour","hours");
            else
            	s = maybePlural(minutes,"minute","minutes");
            pref.setSummary("Every "+s);	    	
	    }
	    
	    private String maybePlural(int n, String singular, String plural) {
	    	return ""+n+" "+((n==1)?singular:plural);
	    }

	    private void updatePlayerIntervalSummary(SharedPreferences prefs, String key, String direction) {
            Preference pref = findPreference(key);
            String value = prefs.getString(key,"");
            pref.setSummary(direction+" "+value+" seconds");	    	
	    }

	}

}
