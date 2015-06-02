package ai.com.audionce;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.List;

public class SoundsPickupService extends Service {
    private LocationManager manager;
    private PrioritizedQueue<Sound> playQueue;
    private Sound playingSound;
    private MediaPlayer tPlayer;

    public SoundsPickupService() {
        playQueue = new PrioritizedQueue<>();
        playingSound = null;
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 30, new SoundLocationListener());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playMedia() throws IOException {
        if(!playQueue.isEmpty() && playingSound == null){
            tPlayer = new MediaPlayer();
            tPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            playingSound = playQueue.dequeueByPriority();
            tPlayer.setDataSource(playingSound.getUrl());
            tPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    try {
                        mp.release();
                        mp = null;
                        playingSound = null;
                        playMedia();
                    } catch (Exception ex){
                        Log.e("AUD", Log.getStackTraceString(ex));
                    }
                }
            });

        }
    }

    private class SoundLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(final Location l){
            ParseGeoPoint mpgp = new ParseGeoPoint(l.getLatitude(),l.getLongitude());
            ParseQuery<ParseObject> soundsNearMe = ParseQuery.getQuery("Sound");
            soundsNearMe.whereWithinKilometers("location",mpgp,Utilities.SOUNDS_DISTANCE_AWAY_KM);
            soundsNearMe.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if(e == null && !list.isEmpty()){
                        playQueue.clear();
//                        if(playingSound != null){
//                            playQueue.clearAllBut(playingSound);
//                        } else {
//                            playQueue.clear();
//                        }
                        for(ParseObject po : list){
                            try {
                                Sound s = Sound.parseSound(po);
                                LatLng sLatLng = s.getLatLng();
                                Location sLoc = new Location("");
                                sLoc.setLongitude(sLatLng.longitude);
                                sLoc.setLatitude(sLatLng.latitude);
                                s.setPriority(sLoc.distanceTo(l));
                                playQueue.enqueue(s);
                            } catch (Exception ex){
                                Utilities.makeLogFromThrowable(ex);
                            }
                        }
                    } else if (e == null){
                        try{
                            playMedia();
                        } catch (Exception ex){
                            Utilities.makeLogFromThrowable(ex);
                        }
                    }
                }
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
}
