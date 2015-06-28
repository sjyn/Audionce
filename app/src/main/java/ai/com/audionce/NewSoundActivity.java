package ai.com.audionce;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.newline.sjyn.audionce.ActivityTracker;
import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Friend;
import com.newline.sjyn.audionce.Utilities;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class NewSoundActivity extends AppCompatActivity {
    private Button play, save, record;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private MediaRecorder mr;
    private boolean recording, playing, saved, pub;
    private EditText et;
    private String filePath;
    private CountDownTimer cdt;
    private CircularProgressView cpv;
    private MediaPlayer mp;
    private ListView friendsToShareWith;
    private static TextView publicOrPrivate;
    private Adapters.ShareWithFriendsAdapter adapter;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sound);
        ActivityTracker tracker = ActivityTracker.getActivityTracker();
        tracker.update(this, ActivityTracker.ActiveActivity.ACTIVITY_NEW_SOUND);
        play = (Button) findViewById(R.id.play_button);
        save = (Button) findViewById(R.id.save_sound);
        record = (Button) findViewById(R.id.record_button);
        et = (EditText) findViewById(R.id.sound_title_et);
        publicOrPrivate = (TextView)findViewById(R.id.pub_or_priv_text_view);
        if (Utilities.getFriends().isEmpty())
            publicOrPrivate.setVisibility(View.GONE);
        cpv = (CircularProgressView) findViewById(R.id.progress_view);
        friendsToShareWith = (ListView)findViewById(R.id.friends_to_share_with);
        friendsToShareWith.setAdapter(adapter = new Adapters.ShareWithFriendsAdapter
                (this, Utilities.getFriends()));
        filePath = getCacheDir().getPath() + "/tmp_mus.aac";
        saved = pub = false;
        play.setEnabled(false);
        save.setEnabled(false);
    }

    public static void notifyTextView(boolean empty){
        Log.e("AUD", "notifyTextView() called");
        if(empty){
            Log.e("AUD", "Setting text to \"public\"");
            publicOrPrivate.setText("public");
        } else {
            Log.e("AUD", "Setting text to \"private\"");
            publicOrPrivate.setText("private");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_sound, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Utilities.stopSoundPickupService(this);
                Intent in = new Intent(this,LoginActivity.class);
                in.putExtra("should_auto_login_from_intent","no");
                startActivity(in);
                break;
            case R.id.goto_friends:
                startActivity(new Intent(this, FriendsActivity.class));
                break;
            case R.id.goto_map:
                startActivity(new Intent(this, HubActivity.class));
                break;
            case R.id.goto_profile:
                startActivity(new Intent(this, ProfileMain.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    public void onRecordClick(View v){
        saved = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        cpv.setProgress(0);
        if (!recording) {
            play.setEnabled(false);
            save.setEnabled(false);
            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            Log.e("AUD", filePath);
            mr.setOutputFile(filePath);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mr.setMaxDuration(Utilities.SOUND_DURATION);
            try {
                mr.prepare();
                mr.start();
                recording = true;
                startCountDownTimer();
                ((Button) v).setText("stop");
            } catch (Exception ex) {
                recording = false;
                Utilities.makeLogFromThrowable(ex);
                Utilities.makeToast(this, "Error Recording Audio.");
            }
        } else {
            mr.stop();
            cdt.cancel();
            cdt = null;
            recording = false;
            record.setText("record");
            play.setEnabled(true);
            save.setEnabled(true);
        }
    }

    @SuppressWarnings({"ConstantConditions"})
    public void onPlayClick(View v){
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        if (!playing) {
            mp = new MediaPlayer();
            record.setEnabled(false);
            save.setEnabled(false);
            play.setText("stop");
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    playing = false;
                    play.setText("play");
                    record.setEnabled(true);
                    save.setEnabled(true);
                }
            });
            Log.e("AUD", getCacheDir().getPath());
            File f = new File(filePath);
            try {
                mp.setDataSource(this, Uri.fromFile(f));
                mp.prepare();
                mp.start();
                playing = true;
            } catch (Exception ex) {
                Utilities.makeLogFromThrowable(ex);
                Utilities.makeToast(this, "Error Playing Audio.");
            }
        } else {
            Log.e("AUD", "Sound currently playing");
            mp.stop();
            mp.release();
            mp = null;
            record.setEnabled(true);
            save.setEnabled(true);
            play.setText("play");
            playing = false;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void onSaveClick(View v){
        if (!saved) {
            final String title = et.getText().toString().trim();
            if (title.equals("")) {
                Utilities.makeToast(this, "Please Name Your Sound.");
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    new AsyncTask<Void, Void, Boolean>() {
                        private String titleCopy = title;
                        private String soundFileLoc = filePath;
                        private LocationManager manager;
                        private ParseUser tUser;
                        private boolean pubT;
                        private List<Friend> shareToTheseFriends;

                        @Override
                        public void onPreExecute() {
                            play.setEnabled(false);
                            record.setEnabled(false);
                            save.setText("Saving...");
                            save.setEnabled(false);
                            friendsToShareWith.setEnabled(false);
                            shareToTheseFriends = adapter.getSelectedFriends();
                            tUser = currUser;
                            manager = (LocationManager) getSystemService(LOCATION_SERVICE);
                            pubT = publicOrPrivate.getText().equals("public");
                        }

                        @SuppressWarnings("ResultOfMethodCallIgnored")
                        @Override
                        public Boolean doInBackground(Void... v) {
                            try {
                                Location l = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (l == null) {
                                    l = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                }
                                ParseGeoPoint pgp = new ParseGeoPoint(l.getLatitude(), l.getLongitude());
                                File f = new File(soundFileLoc);
                                Log.e("AUD", soundFileLoc);
                                FileInputStream fis = new FileInputStream(f);
                                byte[] fArray = new byte[(int) f.length()];
                                fis.read(fArray);
                                fis.close();
                                ParseFile pf = new ParseFile("sound.aac", fArray);
                                pf.save();
                                ParseObject myObj = new ParseObject("Sounds");
                                myObj.put("location", pgp);
                                myObj.put("user", tUser);
                                myObj.put("title", titleCopy);
                                myObj.put("file", pf);
                                if(pubT){
                                    myObj.put("is_private",false);
                                    myObj.save();
                                } else {
                                    myObj.put("is_private", true);
                                    ArrayList<ParseUser> to = new ArrayList<>();
                                    for (Friend friToAdd : shareToTheseFriends) {
                                        to.add(friToAdd.getParseUser());
                                    }
                                    myObj.put("to", to);
                                    myObj.save();
                                    for(Friend friend : shareToTheseFriends){
                                        ParseUser pu = friend.getParseUser().fetchIfNeeded();
                                        ParseQuery<ParseObject> pussq = ParseQuery.getQuery("SharedSounds")
                                                .whereEqualTo("user",pu);
                                        List<ParseObject> results = pussq.find();
                                        results.get(0).add("sounds",myObj);
                                        results.get(0).save();
                                    }
                                }
                                tUser.add("sounds", myObj);
                                tUser.save();
                            } catch (Exception ex) {
                                Utilities.makeLogFromThrowable(ex);
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public void onPostExecute(Boolean res) {
                            if (res) {
                                showSnackbar();
                                saved = true;
                            }
                            play.setEnabled(true);
                            friendsToShareWith.setEnabled(true);
                            record.setEnabled(true);
                            save.setText("save");
                            save.setEnabled(true);
                        }

                        private void deleteSoundsNearMe(ParseGeoPoint cp) throws ParseException {
                            List<ParseObject> pobjs = ParseQuery.getQuery("Sounds")
                                    .whereWithinKilometers("location", cp, Utilities.SOUNDS_DISTANCE_APART_KM)
                                    .find();
                            for (ParseObject po : pobjs) {
                                po.delete();
                            }
                        }
                    }.execute();

            }
        } else {
            Utilities.makeToast(this, "You have already saved this sound!");
        }
    }

    private void startCountDownTimer() {
        cdt = new CountDownTimer(Utilities.SOUND_DURATION * 1000, 1000) {

            @Override
            public void onTick(long progLeft) {
                cpv.setProgress(cpv.getProgress() + 1);
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    private void showSnackbar() {
        SnackbarManager.show(
                Snackbar.with(this)
                        .color(getResources().getColor(R.color.ab_pink))
                        .text("Sound Saved!")
        );
    }
}
