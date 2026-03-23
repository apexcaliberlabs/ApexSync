package com.apexcaliberlabs.apexsync.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.apexcaliberlabs.apexsync.R;

/**
 * Settings screen backed by {@link PreferenceFragmentCompat}.
 *
 * <p>The preferences XML is defined in {@code res/xml/preferences.xml}.
 */
public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState,
                                    @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Additional preference interaction can be wired here.
    }
}
