package ai.com.audionce;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.util.List;


public class HubActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap tMap;
    private int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hub);
        MapFragment mapFrag = (MapFragment)getFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        if(savedInstanceState == null)
            i = 0;
        else
            i = 1;

    }

    @Override
    public void onMapReady(GoogleMap map){
        tMap = map;
        tMap.setMyLocationEnabled(true);
        tMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Log.e("AUD", "cam changed");
                if (i >= 1) {
                    tMap.clear();
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
                if(location != null && i == 0){
                    LatLng lat = new LatLng(location.getLatitude(),location.getLongitude());
                    tMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lat,17),4000,null);
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
                startActivity(new Intent(this,LoginActivity.class));
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
}
