package ai.com.audionce;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.List;

//TODO -- how to make action bar sit under translucent status bar?
public class HubActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap tMap;
    private int i;
    private LatLng mLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        View decorView = getWindow().getDecorView();
//        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(R.layout.activity_hub);
        MapFragment mapFrag = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("should_start_service_from_hub", true))
            Utilities.startSoundPickupService(this);
        if(savedInstanceState == null)
            i = 0;
        else
            i = 1;
    }

    @Override
    public void onResume(){
        super.onResume();
        if(tMap != null) {
            tMap.clear();
            Location mLoc = tMap.getMyLocation();
            if(mLoc != null) {
                tMap.moveCamera(CameraUpdateFactory.
                        newLatLng(new LatLng(mLoc.getLatitude(), mLoc.getLongitude())));
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map){
        tMap = map;
        tMap.setMyLocationEnabled(true);
        tMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                LatLng markerLoc = marker.getPosition();
                Location mark = new Location("");
                mark.setLongitude(markerLoc.longitude);
                mark.setLatitude(markerLoc.latitude);
                ParseQuery<ParseObject> gSound = ParseQuery.getQuery("Sounds");
                gSound.whereEqualTo("location",
                        new ParseGeoPoint(mark.getLatitude(), mark.getLongitude()));
                gSound.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        Sound s = Sound.parseSound(list.get(0));
                        try {
                            playSound(s);
                        } catch (Exception ex) {
                            Utilities.makeLogFromThrowable(ex);
                        }
                    }
                });
                return false;
            }
        });
        tMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (i >= 1) {
                    LatLngBounds bnds = tMap.getProjection().getVisibleRegion().latLngBounds;
                    ParseGeoPoint sw = new ParseGeoPoint(bnds.southwest.latitude, bnds.southwest.longitude);
                    ParseGeoPoint ne = new ParseGeoPoint(bnds.northeast.latitude, bnds.northeast.longitude);
                    ParseQuery<ParseObject> initialQ = ParseQuery.getQuery("Sounds");
                    initialQ.whereWithinGeoBox("location", sw, ne);
                    initialQ.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> list, ParseException e) {
                            if (e == null && !list.isEmpty()) {
                                for (ParseObject p : list) {
                                    ParseGeoPoint gp = (ParseGeoPoint) p.get("location");
                                    LatLng gll = new LatLng(gp.getLatitude(), gp.getLongitude());
                                    tMap.addMarker(new MarkerOptions()
                                            .position(gll)
                                            .title(p.get("title").toString()));
                                }
                            } else {
                                if (e != null)
                                    Log.e("AUD", Log.getStackTraceString(e));
                                else
                                    Log.e("AUD", "List empty");
                            }
                        }
                    });
                }
            }
        });
        tMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                mLoc = new LatLng(location.getLatitude(), location.getLongitude());
                if (i == 0) {
                    tMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLoc, 17), 4000, null);
                    i++;
                }
            }
        });
        Log.e("AUD", "Map ready");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_hub, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Utilities.stopSoundPickupService(this);
                Intent in = new Intent(this,LoginActivity.class);
                startActivity(in);
                break;
            case R.id.edit_profile:
                startActivity(new Intent(this,ProfileMain.class));
                break;
            case R.id.new_sound_from_hub:
                startActivity(new Intent(this,NewSoundActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void playSound(Sound s) throws IOException {
        String tmpPath = getCacheDir().getPath() + "/halp.aac";
        final MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        Log.e("AUD", s.getUrl());
        mp.setDataSource(s.getUrl());
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer medp) {
                mp.start();
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("AUD", what + "");
                return false;
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer medp) {
                mp.release();
            }
        });
        mp.prepareAsync();
    }
}
