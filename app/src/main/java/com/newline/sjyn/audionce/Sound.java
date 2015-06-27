package com.newline.sjyn.audionce;


import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

/**
 * public class Sound extends Prioritized
 *
 * A wrapper class for a ParseObject representation of a Sound.
 */
public class Sound extends Prioritized {
    private LatLng loc;
    private String title;
    private String url;
    private ParseObject ptr;
    private String objectId;

    public Sound(LatLng location, String name, String url, ParseObject po, String id) {
        loc = location;
        title = name;
        this.url = url;
        ptr = po;
        objectId = id;
    }

    public static Sound parseSound(ParseObject po){
        ParseGeoPoint pgp = (ParseGeoPoint)po.get("location");
        LatLng ll = new LatLng(pgp.getLatitude(),pgp.getLongitude());
        String title = po.get("title").toString();
        ParseFile pf = (ParseFile)po.get("file");
        String url = pf.getUrl();
        return new Sound(ll, title, url, po, po.getObjectId());
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

    public ParseObject getParseObject() {
        return ptr;
    }

    @Override
    public boolean equals(Object o){
        return (o instanceof Sound) && ((Sound) o).getObjectId().equals(objectId);
    }

    public String getObjectId() {
        return objectId;
    }
}
