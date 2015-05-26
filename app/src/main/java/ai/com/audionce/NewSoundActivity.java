package ai.com.audionce;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Path;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class NewSoundActivity extends AppCompatActivity {
    private ListView miniFriendsList;
    private Switch pubOrPriv;
    private ImageButton play,save,record;
    private Adapters.FriendAdapter adapter;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private List<Friend> friends;
    private ParseFile pf;
    private MediaRecorder mr;
    private List<String> sndsUrls;
    private boolean recording;
//    private String currCode;
    private ParseFile tFile;
    private String tFileURL;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sound);
        recording = false;
        tFileURL = null;
        miniFriendsList = (ListView) findViewById(R.id.new_sound_friend_share_view);
        pubOrPriv = (Switch)findViewById(R.id.pub_or_priv_switch);
        pubOrPriv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(pubOrPriv.getText().equals("private")){
                    miniFriendsList.setVisibility(View.GONE);
                } else {
                    miniFriendsList.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        play = (ImageButton)findViewById(R.id.playback_button);
        save = (ImageButton)findViewById(R.id.save_button);
        record = (ImageButton)findViewById(R.id.record_button);
        friends = new ArrayList<>();
        sndsUrls = new ArrayList<>();
        mr = new MediaRecorder();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendTable");
        query.whereEqualTo("user", currUser);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null && !list.isEmpty()) {
                    ParseObject obj = list.get(0);
                    List<ParseUser> uf = (List<ParseUser>) obj.get("all_friends");
                    for (ParseUser pu : uf) {
                        Friend f = Friend.parseFriend(pu);
                        friends.add(f);
                    }
                    adapter = new Adapters.FriendAdapter(getApplicationContext(), friends);
                    miniFriendsList.setAdapter(adapter);
                }
            }
        });
        if(pubOrPriv.getText().equals("private"))
            miniFriendsList.setVisibility(View.GONE);
        else
            miniFriendsList.setVisibility(View.VISIBLE);
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRecordClick(View v){
        if(recording){
            mr.stop();
            record.setBackgroundColor(getResources().getColor(R.color.clear));
            recording = false;
        } else {
//            currCode = (int)(Math.random() * Long.MAX_VALUE) + ".mp3";
            mr.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    switch (what) {
                        case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                            mr.stop();
                            record.setBackgroundColor(getResources().getColor(R.color.clear));
                            recording = false;
                            break;
                    }
                }
            });
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mr.setOutputFile(getCacheDir().getPath() + "tmp_mus.mp3");
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mr.setMaxDuration(12 * 1000);
            try {
                mr.prepare();
                record.setBackgroundColor(getResources().getColor(R.color.light_red));
                mr.start();
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
            player.prepareAsync();
        } catch (Exception ex){
            Log.e("AUD",Log.getStackTraceString(ex));
        }
    }

    public void onSaveClick(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.change_name_dialog,null);
        ((TextView)view.findViewById(R.id.edit_name_title)).setText("New Sound Name");
        final EditText namer = (EditText)view.findViewById(R.id.new_name_field);
        namer.setHint("new sound");
        builder.setView(view)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void,Void,Object[]>(){
                            private final String cDir = getCacheDir().getPath() + "tmp_mus.mp3";
                            private ParseUser tUser = currUser;

                            @Override
                            public Object[] doInBackground(Void... v){
                                Object[] ret = new Object[2];
                                try{
                                    File f = new File(cDir);
                                    byte[] array = new byte[(int)f.length()];
                                    FileInputStream fis = new FileInputStream(f);
                                    fis.read(array);
                                    fis.close();
                                    ParseObject po = new ParseObject("Sounds");
                                    ParseFile pf = new ParseFile("" +
                                            (int)(Math.random() * Integer.MAX_VALUE) + ".mp3",array);
                                    pf.save();
                                    ret[0] = pf;
                                    ret[1] = pf.getUrl();
                                    po.put("file", pf);
                                    po.put("title", namer.getText().toString());
                                    po.put("location", new ParseGeoPoint());
                                    po.save();
                                    tUser.fetchIfNeeded();
                                    tUser.add("sounds", po);
                                    tUser.save();
                                    mr.reset();
                                } catch(Exception ex){
                                    Log.e("AUD",Log.getStackTraceString(ex));
                                    return null;
                                }
                                return ret;
                            }

                            @Override
                            public void onPostExecute(Object[] res){
                                if(res != null){
                                    tFileURL = res[1].toString();
                                    Toast.makeText(getApplicationContext(),"Sound Saved!",Toast.LENGTH_SHORT).show();
                                    Log.e("AUD","Saved sound");
                                } else {
                                    Log.e("AUD","didnt save sound");
                                }
                            }
                        }.execute();
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
