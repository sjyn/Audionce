package ai.com.audionce;


import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

public class Sound {
    private LatLng loc;
    private String title;
    private String url;

    public Sound(LatLng location, String name, String url){
        loc = location;
        title = name;
        this.url = url;
    }

    public static Sound parseSound(ParseObject po){
//        try {
//            po.fetchIfNeeded();
            ParseGeoPoint pgp = (ParseGeoPoint)po.get("location");
            LatLng ll = new LatLng(pgp.getLatitude(),pgp.getLongitude());
            String title = po.get("title").toString();
            ParseFile pf = (ParseFile)po.get("file");
            String url = pf.getUrl();
            return new Sound(ll,title,url);
//        } catch (Exception ex){
//            Log.e("AUD", Log.getStackTraceString(ex));
//            return null;
//        }
    }

    public LatLng getLatLng(){
        return loc;
    }

    public String getTitle(){
        return title;
    }

    public String getUrl(){
        return url;
    }
}
