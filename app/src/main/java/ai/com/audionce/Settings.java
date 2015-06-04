package ai.com.audionce;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.newline.sjyn.audionce.Utilities;

public class Settings extends Activity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(android.R.id.content, new SettingsFragment());
        transaction.commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        public SettingsFragment() {
        }

        @Override
        public void onCreate(Bundle b) {
            super.onCreate(b);
            addPreferencesFromResource(R.xml.prefs);
            CheckBoxPreference cbp = (CheckBoxPreference) findPreference("should_run_autoplay_service");
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            cbp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    CheckBoxPreference tcb = (CheckBoxPreference) preference;
                    if (tcb.isChecked()) {
                        Utilities.stopSoundPickupService(getActivity());
                        sp.edit().putBoolean("should_start_service_from_hub", false).commit();
                    } else {
                        Utilities.startSoundPickupService(getActivity());
                        sp.edit().putBoolean("should_start_service_from_hub", true).commit();
                    }
                    return true;
                }
            });
        }
    }
}
