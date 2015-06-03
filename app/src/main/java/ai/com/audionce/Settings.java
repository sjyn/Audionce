package ai.com.audionce;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.preference.PreferenceFragment;

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

        }
    }
}
