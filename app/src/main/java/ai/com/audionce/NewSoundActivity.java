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

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.newline.sjyn.audionce.Utilities;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;


public class NewSoundActivity extends AppCompatActivity {
    private Button play, save, record;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private MediaRecorder mr;
    private boolean recording, playing, saved;
    private EditText et;
    private String filePath;
    private CountDownTimer cdt;
    private CircularProgressView cpv;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sound);
        play = (Button) findViewById(R.id.play_button);
        save = (Button) findViewById(R.id.save_sound);
        record = (Button) findViewById(R.id.record_button);
        et = (EditText) findViewById(R.id.sound_title_et);
        cpv = (CircularProgressView) findViewById(R.id.progress_view);
        filePath = getCacheDir().getPath() + "/tmp_mus.aac";
        saved = false;
        play.setEnabled(false);
        save.setEnabled(false);
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
        }
        return super.onOptionsItemSelected(item);
    }

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
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
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

    public void onPlayClick(View v){
        MediaPlayer mp = new MediaPlayer();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        if (!playing) {
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
                    mp = null;
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
            mp.stop();
            mp.release();
            mp = null;
            record.setEnabled(true);
            save.setEnabled(true);
            play.setText("play");
        }
    }

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

                    @Override
                    public void onPreExecute() {
                        play.setEnabled(false);
                        record.setEnabled(false);
                        save.setText("Saving...");
                        save.setEnabled(false);
                        tUser = currUser;
                        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    }

                    @Override
                    public Boolean doInBackground(Void... v) {
                        try {
                            Location l = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (l == null) {
                                l = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            }
                            ParseGeoPoint pgp = new ParseGeoPoint(l.getLatitude(), l.getLongitude());
                            ParseQuery<ParseObject> soundsThatNeedToBeDeleted = ParseQuery.getQuery("Sounds")
                                    .whereWithinKilometers("location", pgp, Utilities.SOUNDS_DISTANCE_APART_KM);
                            List<ParseObject> results = soundsThatNeedToBeDeleted.find();
                            for (ParseObject po : results) {
                                po.delete();
                            }
                            File f = new File(soundFileLoc);
                            FileInputStream fis = new FileInputStream(f);
                            byte[] fArray = new byte[(int) f.length()];
                            fis.read(fArray);
                            fis.close();
                            ParseFile pf = new ParseFile((long) (Math.random() * Long.MAX_VALUE) + ".acc", fArray);
                            pf.save();
                            ParseObject myObj = new ParseObject("Sounds");
                            myObj.put("location", pgp);
                            myObj.put("title", titleCopy);
                            myObj.put("file", pf);
                            myObj.save();
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
                        record.setEnabled(true);
                        save.setText("save");
                        save.setEnabled(true);
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
