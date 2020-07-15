/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2018 The LineageOS Project
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

package org.lineageos.settings.doze;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.Preference.OnPreferenceChangeListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class DozeSettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener {

    private static final String PULSE_AMBIENT_LIGHT_COLOR_MODE = "pulse_ambient_light_color_mode";
    private static final String PULSE_AMBIENT_LIGHT_COLOR = "pulse_ambient_light_color";

    private TextView mTextView;
    private View mSwitchBar;

    private ColorPickerPreference mEdgeLightColorPref;
    private ListPreference mEdgeLightColorModePref;

    private SwitchPreference mPickUpPreference;
    private SwitchPreference mHandwavePreference;
    private SwitchPreference mPocketPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.doze_settings);
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        SharedPreferences prefs = getActivity().getSharedPreferences("doze_settings",
                Activity.MODE_PRIVATE);
        if (savedInstanceState == null && !prefs.getBoolean("first_help_shown", false)) {
            showHelp();
        }

        boolean dozeEnabled = Utils.isDozeEnabled(getActivity());

        PreferenceCategory proximitySensorCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference(Utils.CATEG_PROX_SENSOR);

        mPickUpPreference = (SwitchPreference) findPreference(Utils.GESTURE_PICK_UP_KEY);
        mPickUpPreference.setEnabled(dozeEnabled);
        mPickUpPreference.setOnPreferenceChangeListener(this);

        mHandwavePreference = (SwitchPreference) findPreference(Utils.GESTURE_HAND_WAVE_KEY);
        mHandwavePreference.setEnabled(dozeEnabled);
        mHandwavePreference.setOnPreferenceChangeListener(this);

        mPocketPreference = (SwitchPreference) findPreference(Utils.GESTURE_POCKET_KEY);
        mPocketPreference.setEnabled(dozeEnabled);
        mPocketPreference.setOnPreferenceChangeListener(this);

        mEdgeLightColorModePref = (ListPreference) findPreference(PULSE_AMBIENT_LIGHT_COLOR_MODE);
        mEdgeLightColorModePref.setEnabled(dozeEnabled);
        mEdgeLightColorModePref.setOnPreferenceChangeListener(this);
        mEdgeLightColorPref = (ColorPickerPreference) findPreference(PULSE_AMBIENT_LIGHT_COLOR);
        mEdgeLightColorPref.setEnabled(dozeEnabled);
        mEdgeLightColorPref.setOnPreferenceChangeListener(this);

        updateCategoryDependencies(dozeEnabled);
        updateColorPrefs();

        proximitySensorCategory.setEnabled(dozeEnabled);
        // Hide proximity sensor related features if the device doesn't support them
        if (!Utils.getProxCheckBeforePulse(getActivity())) {
            getPreferenceScreen().removePreference(proximitySensorCategory);
        }
    }
    private void updateCategoryDependencies(boolean state) {
        PreferenceCategory ambientLightCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(Utils.CATEG_AMBIENT_LIGHT);
        PreferenceCategory doubleTapCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(Utils.CATEG_DOUBLE_TAP);
        PreferenceCategory tiltSensorCategory = (PreferenceCategory) getPreferenceScreen().
                findPreference(Utils.CATEG_TILT_SENSOR);
        SwitchPreference ambientMusicTicker = (SwitchPreference) getPreferenceScreen().
                findPreference(Utils.AMBIENT_MUSIC_TICKER);
        ambientLightCategory.setEnabled(state);
        doubleTapCategory.setEnabled(state);
        tiltSensorCategory.setEnabled(state);
        if (!state) ambientMusicTicker.setChecked(false);
        ambientMusicTicker.setEnabled(state);
     }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.doze, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean dozeEnabled = Utils.isDozeEnabled(getActivity());

        mTextView = view.findViewById(R.id.switch_text);
        mTextView.setText(getString(dozeEnabled ?
                R.string.switch_bar_on : R.string.switch_bar_off));

        mSwitchBar = view.findViewById(R.id.switch_bar);
        Switch switchWidget = mSwitchBar.findViewById(android.R.id.switch_widget);
        switchWidget.setChecked(dozeEnabled);
        switchWidget.setOnCheckedChangeListener(this);
        mSwitchBar.setActivated(dozeEnabled);
        mSwitchBar.setOnClickListener(v -> {
            switchWidget.setChecked(!switchWidget.isChecked());
            mSwitchBar.setActivated(switchWidget.isChecked());
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEdgeLightColorModePref.equals(preference)) {
            updateColorPrefs();
            return true;
        }
        Utils.checkDozeService(getActivity());
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        Utils.enableDoze(getActivity(), isChecked);
        Utils.checkDozeService(getActivity());

        mTextView.setText(getString(isChecked ? R.string.switch_bar_on : R.string.switch_bar_off));
        mSwitchBar.setActivated(isChecked);

        mPickUpPreference.setEnabled(isChecked);
        mHandwavePreference.setEnabled(isChecked);
        mPocketPreference.setEnabled(isChecked);
        updateCategoryDependencies(isChecked);
        if (isChecked) updateColorPrefs();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private void updateColorPrefs() {
        int edgeLightColorMode = Settings.System.getIntForUser(getActivity().getContentResolver(),
              Settings.System.PULSE_AMBIENT_LIGHT_COLOR_MODE, 1, UserHandle.USER_CURRENT);
        mEdgeLightColorPref.setEnabled(edgeLightColorMode == 2);
    }

    public static class HelpDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.doze_settings_help_title)
                    .setMessage(R.string.doze_settings_help_text)
                    .setNegativeButton(R.string.dialog_ok, (dialog, which) -> dialog.cancel())
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            getActivity().getSharedPreferences("doze_settings", Activity.MODE_PRIVATE)
                    .edit()
                    .putBoolean("first_help_shown", true)
                    .commit();
        }
    }

    private void showHelp() {
        HelpDialogFragment fragment = new HelpDialogFragment();
        fragment.show(getFragmentManager(), "help_dialog");
    }
}
