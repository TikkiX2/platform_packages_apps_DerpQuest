/*
 * Copyright (C) 2017-2019 The PixelDust Project
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
import android.content.res.Resources;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.hwkeys.ActionConstants;
import com.android.internal.util.hwkeys.ActionUtils;
import com.android.settings.R;

import com.derpquest.settings.preferences.ActionFragment;
import com.derpquest.settings.preferences.CustomSeekBarPreference;

public class ButtonSettings extends ActionFragment implements OnPreferenceChangeListener {

    //Keys
    private static final String KEY_BUTTON_BRIGHTNESS = "button_brightness";
    private static final String KEY_BUTTON_BRIGHTNESS_SW = "button_brightness_sw";
    private static final String KEY_BACKLIGHT_TIMEOUT = "backlight_timeout";
    private static final String HWKEY_DISABLE = "hardware_keys_disable";
    private static final String DISABLE_NAV_KEYS = "disable_nav_keys";

    // category keys
    private static final String CATEGORY_HWKEY = "hardware_keys";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private ListPreference mBacklightTimeout;
    private CustomSeekBarPreference mButtonBrightness;
    private SwitchPreference mButtonBrightness_sw;
    private SwitchPreference mHwKeyDisable;
    private SwitchPreference mDisableNavigationKeys;
    private boolean mIsNavSwitchingMode = false;
    private Handler mHandler;

    private static final String KEY_TORCH_LONG_PRESS_POWER_TIMEOUT =
            "torch_long_press_power_timeout";

    private ListPreference mTorchLongPressPowerTimeout;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.derpquest_settings_button);
        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        // Force Navigation bar related options
        mDisableNavigationKeys = (SwitchPreference) findPreference(DISABLE_NAV_KEYS);

        // Only visible on devices that does not have a navigation bar already
        if (ActionUtils.isHWKeysSupported(getActivity())) {
            mDisableNavigationKeys.setOnPreferenceChangeListener(this);
            mHandler = new Handler();
            // Remove keys that can be provided by the navbar
            updateDisableNavkeysOption();
            setActionPreferencesEnabled(mDisableNavigationKeys.isChecked());
        } else {
            prefScreen.removePreference(mDisableNavigationKeys);
        }

        final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        final PreferenceCategory hwkeyCat = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HWKEY);
        int keysDisabled = 0;
        if (!needsNavbar) {
            mHwKeyDisable = (SwitchPreference) findPreference(HWKEY_DISABLE);
            keysDisabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT);
            mHwKeyDisable.setChecked(keysDisabled != 0);
            mHwKeyDisable.setOnPreferenceChangeListener(this);

            final boolean variableBrightness = getResources().getBoolean(
                    com.android.internal.R.bool.config_deviceHasVariableButtonBrightness);

            mBacklightTimeout =
                    (ListPreference) findPreference(KEY_BACKLIGHT_TIMEOUT);

            mButtonBrightness =
                    (CustomSeekBarPreference) findPreference(KEY_BUTTON_BRIGHTNESS);

            mButtonBrightness_sw =
                    (SwitchPreference) findPreference(KEY_BUTTON_BRIGHTNESS_SW);

                if (mBacklightTimeout != null) {
                    mBacklightTimeout.setOnPreferenceChangeListener(this);
                    int BacklightTimeout = Settings.System.getIntForUser(getContentResolver(),
                            Settings.System.BUTTON_BACKLIGHT_TIMEOUT, 5000, UserHandle.USER_CURRENT);
                    mBacklightTimeout.setValue(Integer.toString(BacklightTimeout));
                    mBacklightTimeout.setSummary(mBacklightTimeout.getEntry());
                }

                if (variableBrightness) {
                    hwkeyCat.removePreference(mButtonBrightness_sw);
                    if (mButtonBrightness != null) {
                        int ButtonBrightness = Settings.System.getIntForUser(getContentResolver(),
                                Settings.System.BUTTON_BRIGHTNESS, 255, UserHandle.USER_CURRENT);
                        mButtonBrightness.setValue(ButtonBrightness / 1);
                        mButtonBrightness.setOnPreferenceChangeListener(this);
                    }
                } else {
                    hwkeyCat.removePreference(mButtonBrightness);
                    if (mButtonBrightness_sw != null) {
                        mButtonBrightness_sw.setChecked((Settings.System.getIntForUser(getContentResolver(),
                                Settings.System.BUTTON_BRIGHTNESS, 1, UserHandle.USER_CURRENT) == 1));
                        mButtonBrightness_sw.setOnPreferenceChangeListener(this);
                    }
                }
        } else {
            prefScreen.removePreference(hwkeyCat);
        }

        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        // read bits for present hardware keys
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        // load categories and init/remove preferences based on device
        // configuration
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);

        // back key
        if (!hasBackKey) {
            prefScreen.removePreference(backCategory);
        }

        // home key
        if (!hasHomeKey) {
            prefScreen.removePreference(homeCategory);
        }

        // App switch key (recents)
        if (!hasAppSwitchKey) {
            prefScreen.removePreference(appSwitchCategory);
        }

        // menu key
        if (!hasMenuKey) {
            prefScreen.removePreference(menuCategory);
        }

        // search/assist key
        if (!hasAssistKey) {
            prefScreen.removePreference(assistCategory);
        }

        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.HWKEYS));

        // load preferences first
        setActionPreferencesEnabled(keysDisabled == 0);

        // Torch Power button
        mTorchLongPressPowerTimeout =
                    (ListPreference) findPreference(KEY_TORCH_LONG_PRESS_POWER_TIMEOUT);

        mTorchLongPressPowerTimeout.setOnPreferenceChangeListener(this);
        int TorchTimeout = Settings.System.getIntForUser(getContentResolver(),
                        Settings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, 0, UserHandle.USER_CURRENT);
        mTorchLongPressPowerTimeout.setValue(Integer.toString(TorchTimeout));
        mTorchLongPressPowerTimeout.setSummary(mTorchLongPressPowerTimeout.getEntry());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBacklightTimeout) {
            String BacklightTimeout = (String) newValue;
            int BacklightTimeoutValue = Integer.parseInt(BacklightTimeout);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BACKLIGHT_TIMEOUT, BacklightTimeoutValue, UserHandle.USER_CURRENT);
            int BacklightTimeoutIndex = mBacklightTimeout
                    .findIndexOfValue(BacklightTimeout);
            mBacklightTimeout
                    .setSummary(mBacklightTimeout.getEntries()[BacklightTimeoutIndex]);
            return true;
        } else if (preference == mButtonBrightness) {
            int value = (Integer) newValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, value * 1, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mButtonBrightness_sw) {
            boolean value = (Boolean) newValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, value ? 1 : 0, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mHwKeyDisable) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putIntForUser(getContentResolver(), Settings.Secure.HARDWARE_KEYS_DISABLE,
                    value ? 1 : 0, UserHandle.USER_CURRENT);
            setActionPreferencesEnabled(!value);
            return true;
        } else if (preference == mTorchLongPressPowerTimeout) {
            String TorchTimeout = (String) newValue;
            int TorchTimeoutValue = Integer.parseInt(TorchTimeout);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.TORCH_LONG_PRESS_POWER_TIMEOUT, TorchTimeoutValue, UserHandle.USER_CURRENT);
            int TorchTimeoutIndex = mTorchLongPressPowerTimeout
                    .findIndexOfValue(TorchTimeout);
            mTorchLongPressPowerTimeout
                    .setSummary(mTorchLongPressPowerTimeout.getEntries()[TorchTimeoutIndex]);
            return true;
        } else if (preference == mDisableNavigationKeys) {
            if (mIsNavSwitchingMode) {
                return false;
            }
            mIsNavSwitchingMode = true;
            boolean isNavKeysChecked = ((Boolean) newValue);
            mDisableNavigationKeys.setEnabled(false);
            mHwKeyDisable.setEnabled(false);
            writeDisableNavkeysOption(isNavKeysChecked);
            updateDisableNavkeysOption();
            int keysDisabled = Settings.Secure.getIntForUser(getActivity().getContentResolver(),
                    Settings.Secure.HARDWARE_KEYS_DISABLE, 0, UserHandle.USER_CURRENT);
            setActionPreferencesEnabled(keysDisabled == 0);
            mDisableNavigationKeys.setEnabled(true);
            mHwKeyDisable.setEnabled(true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsNavSwitchingMode = false;
                }
            }, 1000);
            return true;
        }
        return false;
    }

    private void writeDisableNavkeysOption(boolean enabled) {
        Settings.System.putIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void updateDisableNavkeysOption() {
        boolean enabled = Settings.System.getIntForUser(getActivity().getContentResolver(),
                Settings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
        mDisableNavigationKeys.setChecked(enabled);
    }

    @Override
    protected boolean usesExtendedActionsList() {
        return true;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.OWLSNEST;
    }
}
