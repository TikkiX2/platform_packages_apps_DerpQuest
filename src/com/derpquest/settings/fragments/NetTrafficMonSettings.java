/*
 * Copyright (C) 2020 DerpFest ROM
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
package com.derpquest.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settingslib.search.SearchIndexable;

import com.derpquest.settings.preferences.CustomSeekBarPreference;
import com.derpquest.settings.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SearchIndexable
public class NetTrafficMonSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {

    private ListPreference mNetTrafficLocation;
    private CustomSeekBarPreference mThreshold;
    private SystemSettingSwitchPreference mShowArrows;
    private ListPreference mNetTrafficType;
    private CustomSeekBarPreference mNetTrafficSize;

    private static final String NETWORK_TRAFFIC_FONT_SIZE  = "network_traffic_font_size";
    private static final String NETWORK_TRAFFIC_LOCATION = "network_traffic_location";
    private static final String NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD = "network_traffic_autohide_threshold";
    private static final String NETWORK_TRAFFIC_ARROW = "network_traffic_arrow";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.derpquest_settings_net_traffic);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        int type = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_TYPE, 0, UserHandle.USER_CURRENT);
        mNetTrafficType = (ListPreference) findPreference("network_traffic_type");
        mNetTrafficType.setValue(String.valueOf(type));
        mNetTrafficType.setSummary(mNetTrafficType.getEntry());
        mNetTrafficType.setOnPreferenceChangeListener(this);

        int NetTrafficSize = Settings.System.getInt(resolver,
                Settings.System.NETWORK_TRAFFIC_FONT_SIZE, 36);
        mNetTrafficSize = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_FONT_SIZE);
        mNetTrafficSize.setValue(NetTrafficSize / 1);
        mNetTrafficSize.setOnPreferenceChangeListener(this);

        mNetTrafficLocation = (ListPreference) findPreference(NETWORK_TRAFFIC_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0, UserHandle.USER_CURRENT);
        mNetTrafficLocation.setOnPreferenceChangeListener(this);
        mNetTrafficLocation.setValue(String.valueOf(location));
        mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntry());

        int value = Settings.System.getIntForUser(resolver,
                Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, 1, UserHandle.USER_CURRENT);
        mThreshold = (CustomSeekBarPreference) findPreference(NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD);
        mThreshold.setValue(value);
        mThreshold.setOnPreferenceChangeListener(this);
        mShowArrows = (SystemSettingSwitchPreference) findPreference(NETWORK_TRAFFIC_ARROW);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNetTrafficLocation) {
            int location = Integer.valueOf((String) objValue);
            // 0=sb; 1=expanded sb
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, location, UserHandle.USER_CURRENT);
            int index = mNetTrafficLocation.findIndexOfValue((String) objValue);
            mNetTrafficLocation.setSummary(mNetTrafficLocation.getEntries()[index]);
            mNetTrafficLocation.setValue(String.valueOf(location));
            return true;
        } else if (preference == mThreshold) {
            int val = (Integer) objValue;
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_AUTOHIDE_THRESHOLD, val,
                    UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mNetTrafficType) {
            int val = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_TYPE, val,
                    UserHandle.USER_CURRENT);
            int index = mNetTrafficType.findIndexOfValue((String) objValue);
            mNetTrafficType.setSummary(mNetTrafficType.getEntries()[index]);
            return true;
        }  else if (preference == mNetTrafficSize) {
            int width = ((Integer)objValue).intValue();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_FONT_SIZE, width);
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.OWLSNEST;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.derpquest_settings_net_traffic;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
