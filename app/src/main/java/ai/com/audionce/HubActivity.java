package ai.com.audionce;

import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.IOException;
import java.util.List;


public class HubActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap tMap;
    private int i;
    private LatLng mLoc;
    private boolean isSoundPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        MapFragment mapFrag = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        isSoundPlaying = false;
        Utilities.startSoundPickupService(this);
//        startService(new Intent(this, SoundsPickupService.class));
//        Intent in = getIntent();
//        if(in.getFlags() == Utilities.FLAG_FROM_SERVICE_TO_HUB){
//
//        }
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
//        tMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
//            @Override
//            public boolean onMarkerClick(Marker marker) {
//                LatLng markerLoc = marker.getPosition();
//                Location mark = new Location("");
//                mark.setLongitude(markerLoc.longitude);
//                mark.setLatitude(markerLoc.latitude);
//                Location myLoc = new Location("");
//                myLoc.setLatitude(mLoc.latitude);
//                myLoc.setLongitude(mLoc.longitude);
//                Log.e("AUD", "Marker tapped; you are " + mark.distanceTo(myLoc) + "m away");
//                if (mark.distanceTo(myLoc) <= 40) {
//                    ParseQuery<ParseObject> gSound = ParseQuery.getQuery("Sounds");
//                    gSound.whereEqualTo("location",
//                            new ParseGeoPoint(mark.getLatitude(), mark.getLongitude()));
//                    gSound.findInBackground(new FindCallback<ParseObject>() {
//                        @Override
//                        public void done(List<ParseObject> list, ParseException e) {
//                            if(e == null){
//                                Sound s = Sound.parseSound(list.get(0));
//                                if(!isSoundPlaying) {
//                                    try {
//                                        playSound(s);
//                                        Log.e("AUD","Playing Sound");
//                                    } catch (IOException ioe){
//                                        showToast("Error Playing Media");
//                                        Log.e("AUD", Log.getStackTraceString(ioe));
//                                    }
//                                }
//                            } else {
//                                Log.e("AUD",Log.getStackTraceString(e));
//                            }
//                        }
//                    });
//                }
//                return false;
//            }
//        });
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
//                Log.e("AUD","lat: " + mLoc.latitude + " long: " + mLoc.longitude);
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
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Intent in = new Intent(this,LoginActivity.class);
//                in.putExtra("should_auto_login_from_intent", "no");
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
        MediaPlayer mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mp.setDataSource(s.getUrl());
        mp.prepareAsync();
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                isSoundPlaying = true;
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                mp = null;
                isSoundPlaying = false;
            }
        });
    }

    private void showToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }
}
