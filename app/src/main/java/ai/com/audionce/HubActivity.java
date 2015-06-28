package ai.com.audionce;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class HubActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap tMap;
    private int i;
    private LatLng mLoc;
    private ParseUser currUser;
    private List<Sound> tempSounds;
    private CircularProgressView cpv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        cpv = (CircularProgressView) findViewById(R.id.progress_view);
        cpv.setVisibility(View.GONE);
        currUser = ParseUser.getCurrentUser();
        tempSounds = new ArrayList<>();
        Utilities.loadFriends(ParseUser.getCurrentUser());
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
    @SuppressWarnings({"unchecked"})
    public void onMapReady(GoogleMap map){
        tMap = map;
        tMap.setMyLocationEnabled(true);
        tMap.getUiSettings().setZoomControlsEnabled(true);
        tMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (i >= 1) {
                    LatLngBounds bnds = tMap.getProjection().getVisibleRegion().latLngBounds;
                    ParseGeoPoint sw = new ParseGeoPoint(bnds.southwest.latitude, bnds.southwest.longitude);
                    ParseGeoPoint ne = new ParseGeoPoint(bnds.northeast.latitude, bnds.northeast.longitude);
                    new AsyncTask<ParseGeoPoint, Void, Boolean>() {
                        @Override
                        public void onPreExecute() {
                            cpv.setVisibility(View.VISIBLE);
                            cpv.startAnimation();
                        }

                        @Override
                        public Boolean doInBackground(ParseGeoPoint... pgpa) {
                            try {
                                List<ParseObject> lpo1 = ParseQuery.getQuery("Sounds")
                                        .whereWithinGeoBox("location", pgpa[0], pgpa[1])
                                        .whereEqualTo("is_private", false)
                                        .find();
                                for (ParseObject po : lpo1) {
                                    Sound s = Sound.parseSound(po.fetchIfNeeded());
                                    if (!tempSounds.contains(s))
                                        tempSounds.add(s);
                                }
                                List<ParseObject> lpo2 = currUser.getParseObject("shared_sounds")
                                        .fetchIfNeeded()
                                        .getList("sounds");
                                for (ParseObject po : lpo2) {
                                    Sound s = Sound.parseSound(po.fetchIfNeeded());
                                    if (!tempSounds.contains(s))
                                        tempSounds.add(s);
                                }
                                List<ParseObject> lpo3 = currUser.getList("sounds");
                                for (ParseObject po : lpo3) {
                                    Sound s = Sound.parseSound(po.fetchIfNeeded());
                                    if (!tempSounds.contains(s))
                                        tempSounds.add(s);
                                }
                            } catch (Exception ex) {
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public void onPostExecute(Boolean res) {
                            cpv.clearAnimation();
                            cpv.setVisibility(View.GONE);
                            if (res) {
                                for (Sound s : tempSounds) {
                                    LatLng ll = s.getLatLng();
                                    tMap.addMarker(new MarkerOptions()
                                            .title(s.getTitle())
                                            .position(ll));
                                }
                            }
                        }

                    }.execute(sw, ne);
                }
            }
        });
        tMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                mLoc = new LatLng(location.getLatitude(), location.getLongitude());
                if (i == 0) {
                    cpv.setVisibility(View.VISIBLE);
                    cpv.startAnimation();
                    tMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLoc, 17), 4000, null);
                    i++;
                    cpv.clearAnimation();
                    cpv.setVisibility(View.GONE);
                }
            }
        });
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
            case R.id.goto_friends:
                startActivity(new Intent(this, FriendsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
