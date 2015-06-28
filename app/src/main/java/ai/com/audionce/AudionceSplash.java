package ai.com.audionce;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.newline.sjyn.audionce.Utilities;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.List;


public class AudionceSplash extends AppCompatActivity {

    @Override
    @SuppressWarnings("null")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
        setContentView(R.layout.activity_audionce_splash);
        if (!Utilities.doesHaveNetworkConnection(this)) {
            Utilities.makeToast(this, "No Network Connection.");
            startActivity(new Intent(this, LoginActivity.class));
        } else if (!checkLocationIsOff()) {
            attemptToAutoLoginUser();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Location Services Are Not Enabled")
                    .setMessage(getResources().getString(R.string.enable_location_text))
                    .setPositiveButton("Enable Location", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent in = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(in);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        }
    }

    private void attemptToAutoLoginUser() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("should_auto_login", false)) {
            final String un = sp.getString("saved_username", "");
            final String pw = sp.getString("saved_password", "");
            new AsyncTask<Void, Void, Boolean>() {
                String unc = un;
                String pwc = pw;
                SharedPreferences spc = sp;

                @Override
                @SuppressWarnings("unchecked")
                public Boolean doInBackground(Void... v) {
                    try {
                        ParseUser.logIn(unc, pwc);
                        ParseQuery<ParseObject> gFriend = ParseQuery.getQuery("FriendTable")
                                .whereEqualTo("user", ParseUser.getCurrentUser());
                        List<ParseObject> resA = gFriend.find();
                        List<ParseUser> mFriends = (List<ParseUser>) resA.get(0).get("all_friends");
                        spc.edit().putInt("num_friends", mFriends.size()).commit();
                    } catch (Exception ex) {
                        return false;
                    }
                    return true;
                }

                @Override
                public void onPostExecute(Boolean res) {
                    if (res) {
                        Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_SHORT).show();
                        Intent in = new Intent(getApplicationContext(), HubActivity.class);
                        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(in);
                    } else {
                        Intent in = new Intent(getApplicationContext(), LoginActivity.class);
                        in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(in);
                    }
                }

            }.execute();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private boolean checkLocationIsOff() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gps, network;
        gps = network = false;
        try {
            gps = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }
        try {
            network = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        return !gps && !network;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_audionce_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }
}
