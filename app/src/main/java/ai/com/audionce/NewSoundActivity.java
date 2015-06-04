package ai.com.audionce;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Friend;
import com.newline.sjyn.audionce.Utilities;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class NewSoundActivity extends AppCompatActivity {
//    private ListView miniFriendsList;
//    private Switch pubOrPriv;
    private CountDownTimer cdt;
    private int SCREEN_WIDTH;
    private ProgressBar prog;
    private ImageButton play,save,record;
    private Adapters.FriendAdapter adapter;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private List<Friend> friends;
    private ParseFile pf;
    private MediaRecorder mr;
    private List<String> sndsUrls;
    private boolean recording;
    private String tFileURL;
    private LocationManager manager;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sound);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        SCREEN_WIDTH = dm.widthPixels;
        recording = false;
        tFileURL = null;
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        prog = (ProgressBar)findViewById(R.id.progress);
        prog.setMax(SCREEN_WIDTH - 20);
//        miniFriendsList = (ListView) findViewById(R.id.new_sound_friend_share_view);
//        miniFriendsList.setVisibility(View.GONE);
//        pubOrPriv = (Switch)findViewById(R.id.pub_or_priv_switch);
//        pubOrPriv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked){
//                    miniFriendsList.setVisibility(View.VISIBLE);
//                } else {
//                    miniFriendsList.setVisibility(View.GONE);
//                }
//            }
//        });
        play = (ImageButton)findViewById(R.id.playback_button);
        save = (ImageButton)findViewById(R.id.save_button);
        record = (ImageButton)findViewById(R.id.record_button);
        friends = new ArrayList<>();
        sndsUrls = new ArrayList<>();
        mr = new MediaRecorder();
//        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendTable");
//        query.whereEqualTo("user", currUser);
//        query.findInBackground(new FindCallback<ParseObject>() {
//            @Override
//            public void done(List<ParseObject> list, ParseException e) {
//                if (e == null && !list.isEmpty()) {
//                    ParseObject obj = list.get(0);
//                    List<ParseUser> uf = (List<ParseUser>) obj.get("all_friends");
//                    for (ParseUser pu : uf) {
//                        try {
//                            Friend f = Friend.parseFriend(pu.fetchIfNeeded());
//                            f.setType("friends");
//                            friends.add(f);
//                        } catch(Exception ex){
//                            Log.e("AUD",Log.getStackTraceString(ex));
//                        }
//                    }
//                    adapter = new Adapters.FriendAdapter(getApplicationContext(), friends);
//                    miniFriendsList.setAdapter(adapter);
//                }
//            }
//        });
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
        if(recording){
            mr.stop();
            record.setBackgroundColor(getResources().getColor(R.color.clear));
            cdt.cancel();
            recording = false;
        } else {
            mr.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    switch (what) {
                        case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                            mr.stop();
                            record.setBackgroundColor(getResources().getColor(R.color.clear));
                            cdt.cancel();
                            recording = false;
                            break;
                    }
                }
            });
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mr.setOutputFile(getCacheDir().getPath() + "tmp_mus.aac");
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mr.setMaxDuration(Utilities.SOUND_DURATION);
            try {
                mr.prepare();
                record.setBackgroundColor(getResources().getColor(R.color.light_red));
                mr.start();
                createAndStartCountdownTimer();
                recording = true;
            } catch (IOException ioe) {
                Log.e("AUD", Log.getStackTraceString(ioe));
            }
        }
    }

    public void onPlayClick(View v){
        try{
            MediaPlayer player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(tFileURL);
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {

                    mp.start();
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    mp = null;
                }
            });
            player.prepareAsync();
        } catch (Exception ex){
            Log.e("AUD",Log.getStackTraceString(ex));
        }
    }

    private void createAndStartCountdownTimer(){
        Log.e("AUD","Creating countdown timer");
        cdt = new CountDownTimer(Utilities.SOUND_DURATION,1000) {
            @Override
            public void onTick(long millisLeft) {
                prog.setProgress(prog.getProgress() + (SCREEN_WIDTH / 30));
                Log.e("AUD", "count down tick registered");
            }

            @Override
            public void onFinish() {

            }
        };
        cdt.start();
    }

    public void onSaveClick(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.change_name_dialog, null);
        ((TextView)view.findViewById(R.id.edit_name_title)).setText("New Sound Name");
        final EditText namer = (EditText)view.findViewById(R.id.new_name_field);
        namer.setHint("new sound");
        builder.setView(view);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new AsyncTask<Void, Void, Object[]>() {
                    private final String cDir = getCacheDir().getPath() + "tmp_mus.aac";
                    private ParseUser tUser = currUser;
                    private double geoX, geoY;
                    @Override
                    public void onPreExecute() {
                        Location l = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (l == null) {
                            Log.e("AUD", "could not find location");
                            geoX = geoY = 0D;
                        } else {
                            geoX = l.getLatitude();
                            geoY = l.getLongitude();
                        }
                    }

                    @Override
                    public Object[] doInBackground(Void... v) {
                        Object[] ret = new Object[2];
                        try {
                            File f = new File(cDir);
                            byte[] array = new byte[(int) f.length()];
                            FileInputStream fis = new FileInputStream(f);
                            fis.read(array);
                            fis.close();
                            ParseObject po = new ParseObject("Sounds");
                            ParseFile pf = new ParseFile("" +
                                    (long)(Math.random() * Long.MAX_VALUE) + ".aac", array);
                            pf.save();
                            ret[0] = pf;
                            ret[1] = pf.getUrl();
                            po.put("file", pf);
                            po.put("title", namer.getText().toString());
                            ParseGeoPoint gtfo = new ParseGeoPoint(geoX, geoY);
                            ParseQuery<ParseObject> soundNeargtfo = ParseQuery.getQuery("Sounds")
                                    .whereWithinKilometers("location", gtfo, 0.01);
                            List<ParseObject> closeSounds = soundNeargtfo.find();
                            for(ParseObject pObj : closeSounds){
                                pObj.delete();
                            }
                            po.put("location", gtfo);
                            po.save();
                            tUser.fetchIfNeeded();
                            tUser.add("sounds", po);
                            tUser.save();
                            mr.reset();
                        } catch (Exception ex) {
                            Log.e("AUD", Log.getStackTraceString(ex));
                            return null;
                        }
                        return ret;
                    }

                    @Override
                    public void onPostExecute(Object[] res) {
                        if (res != null) {
                            tFileURL = res[1].toString();
                            Toast.makeText(getApplicationContext(), "Sound Saved!", Toast.LENGTH_SHORT).show();
                            Log.e("AUD", "Saved sound");
                        } else {
                            Log.e("AUD", "didnt save sound");
                        }
                    }
                }.execute();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
}
